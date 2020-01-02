package de.sgollmer.solvismax.connection;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadPoolExecutor;

import javax.xml.stream.XMLStreamException;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import de.sgollmer.solvismax.Constants;
import de.sgollmer.solvismax.connection.Server.Client;
import de.sgollmer.solvismax.connection.transfer.ChannelDescriptionsPackage;
import de.sgollmer.solvismax.connection.transfer.Command;
import de.sgollmer.solvismax.connection.transfer.ConnectPackage;
import de.sgollmer.solvismax.connection.transfer.ConnectedPackage;
import de.sgollmer.solvismax.connection.transfer.ConnectionState;
import de.sgollmer.solvismax.connection.transfer.DisconnectPackage;
import de.sgollmer.solvismax.connection.transfer.GetPackage;
import de.sgollmer.solvismax.connection.transfer.JsonPackage;
import de.sgollmer.solvismax.connection.transfer.ReconnectPackage;
import de.sgollmer.solvismax.connection.transfer.ServerCommand;
import de.sgollmer.solvismax.connection.transfer.SetPackage;
import de.sgollmer.solvismax.error.JsonError;
import de.sgollmer.solvismax.error.LearningError;
import de.sgollmer.solvismax.error.XmlError;
import de.sgollmer.solvismax.model.CommandScreenRestore;
import de.sgollmer.solvismax.model.Instances;
import de.sgollmer.solvismax.model.Solvis;
import de.sgollmer.solvismax.model.objects.ChannelDescription;
import de.sgollmer.solvismax.model.objects.data.ModeI;
import de.sgollmer.solvismax.model.objects.data.ModeValue;
import de.sgollmer.solvismax.model.objects.data.SingleData;

public class CommandHandler {

	private static final Logger logger = LogManager.getLogger(CommandHandler.class);

	private final Collection<ClientAssignments> clients;
	private final Instances instances;
	private int nextClientId = Long.hashCode(System.currentTimeMillis());
	private ThreadPoolExecutor executor;

	public CommandHandler(Instances instances) {
		this.instances = instances;
		this.clients = new ArrayList<>();
		this.executor = (ThreadPoolExecutor) Executors.newFixedThreadPool(Constants.MAX_CONNECTIONS);

	}

	public boolean commandFromClient(JsonPackage jsonPackage, Client client) throws IOException {
		Command command = jsonPackage.getCommand();
		logger.info("Command <" + command.name() + "> received");
		boolean abortConnection = false;
		switch (command) {
			case CONNECT:
				abortConnection = this.connect((ConnectPackage) jsonPackage, client);
				break;
			case RECONNECT:
				abortConnection = this.reconnect((ReconnectPackage) jsonPackage, client);
				break;
			case DISCONNECT:
				this.disconnect((DisconnectPackage) jsonPackage, client, false);
				break;
			case SHUTDOWN:
				this.disconnect((DisconnectPackage) jsonPackage, client, true);
				break;
			case GET:
				this.get((GetPackage) jsonPackage, client);
				break;
			case SET:
				this.set((SetPackage) jsonPackage, client);
				break;
			case SERVER_COMMAND:
				this.executSeverCommand((ServerCommand) jsonPackage, client);
				break;
			default:
				logger.warn("Command <" + command.name() + ">unknown, old version of SolvisSmartHomeServer?");
				break;
		}
		return abortConnection;
	}

	private void executSeverCommand(ServerCommand serverCommand, Client client) throws IOException {
		ClientAssignments assignments = this.get(client);
		switch (serverCommand.getServerCommand()) {
			case BACKUP:
				try {
					this.instances.backupMeasurements();
				} catch (XMLStreamException e) {
					logger.error("XMLStream error while writing the backup file");
				}
				break;
			case SCREEN_RESTORE_INHIBIT:
				this.screenRestoreInhibit(true, assignments);
				break;
			case SCREEN_RESTORE_ENABLE:
				this.screenRestoreInhibit(false, assignments);
				break;

			default:
				logger.warn("Server command <" + serverCommand.getServerCommand().name()
						+ ">unknown, old version of SolvisSmartHomeServer?");
				break;
		}

	}

	@SuppressWarnings("static-method")
	private void screenRestoreInhibit(boolean inhibit, ClientAssignments assignments) {
		if (assignments != null) {
			Boolean restoreInhibit = null;
			if (inhibit && !assignments.screenRestoreInhibit) {
				restoreInhibit = true;
			} else if (!inhibit && assignments.screenRestoreInhibit) {
				restoreInhibit = false;
			}
			if (restoreInhibit != null) {
				assignments.screenRestoreInhibit = restoreInhibit;
				assignments.solvis.execute(new CommandScreenRestore(!restoreInhibit));
			}
		}
	}

	private boolean connect(ConnectPackage jsonPackage, Client client) {
		int clientId = this.getNewClientId();
		Solvis solvis = null;
		try {
			solvis = this.instances.getInstance(jsonPackage);
		} catch (IOException | XmlError | XMLStreamException | LearningError e1) {
			client.send(new ConnectionState(ConnectionStatus.CONNECTION_NOT_POSSIBLE,
					"Solvis not connected: " + e1.getMessage()).createJsonPackage());
			client.closeDelayed();
			return true;
		}
		if (solvis == null) {
			client.send(new ConnectionState(ConnectionStatus.CONNECTION_NOT_POSSIBLE, "Solvis id unknown")
					.createJsonPackage());
			client.closeDelayed();
			return true;
		}
		solvis.getDistributor().register(client);
		client.send(new ConnectedPackage(clientId));
		ClientAssignments assignments = new ClientAssignments(clientId, solvis, client);
		this.register(assignments);
		this.clients.add(assignments);

		ChannelDescriptionsPackage channelDescription = new ChannelDescriptionsPackage(
				this.instances.getSolvisDescription().getChannelDescriptions(), solvis.getConfigurationMask());
		client.send(channelDescription);

		client.send(solvis.getAllSolvisData().getMeasurementsPackage());
		client.send(solvis.getSolvisState().getPackage());
		return false;
	}

	private boolean reconnect(ReconnectPackage reconnectPackage, Client client) {

		int clientId = reconnectPackage.getClientId();
		ClientAssignments assignments = this.get(clientId);
		if (assignments == null) {
			client.send(new ConnectionState(ConnectionStatus.CLIENT_UNKNOWN, "Client id unknown").createJsonPackage());
			client.closeDelayed();
			return true;
		}
		Client former = assignments.client;
		Solvis solvis = assignments.solvis;
		if (former != null) {
			former.close();
			solvis.getDistributor().unregister(former);
		}

		assignments.reconnect(client);
		solvis.getDistributor().register(client);

		client.send(solvis.getAllSolvisData().getMeasurementsPackage());
		client.send(solvis.getSolvisState().getPackage());

		return false;
	}

	private void disconnect(DisconnectPackage jsonPackage, Client client, boolean shutdown) {

		ClientAssignments assignments = this.get(client);
		Solvis solvis = assignments.solvis;
		this.unregister(assignments);
		client.close();
		if (shutdown) {
			if (this.clients.size() > 0 && !this.isSolvisConnected(solvis)) {
				solvis.abort();
			}
		}
	}

	private void set(SetPackage jsonPackage, Client client) {
		ClientAssignments assignments = this.get(client);
		Solvis solvis = assignments.solvis;
		ChannelDescription description = solvis.getChannelDescription(jsonPackage.getId());
		SingleData<?> singleData = jsonPackage.getSingleData();
		logger.info("Channel <" + description.getId() + "> will be set to " + singleData.toString() + ">.");
		if (description.getModes() != null) {
			ModeI setMode = null;
			for (ModeI mode : description.getModes()) {
				if (mode.getName().equals(singleData.toString())) {
					setMode = mode;
					break;
				}
			}
			if (setMode == null)
				throw new JsonError("Unknown mode <" + singleData.toString() + "> in revceived Json package");
			singleData = new ModeValue<ModeI>(setMode);
		}
		solvis.execute(new de.sgollmer.solvismax.model.CommandControl(description, singleData));
	}

	private void get(GetPackage jsonPackage, Client client) {
		ClientAssignments assignments = this.get(client);
		Solvis solvis = assignments.solvis;
		ChannelDescription description = solvis.getChannelDescription(jsonPackage.getId());
		logger.info("Channel <" + description.getId() + "> will be updated by GET command");
		solvis.execute(new de.sgollmer.solvismax.model.CommandControl(description));
	}

	public class ClientAssignments {
		private final int clientId;
		private final Solvis solvis;
		private Client client;
		private boolean screenRestoreInhibit = false;
		private ClosingThread closingThread = null;

		public ClientAssignments(int clientid, Solvis solvis, Client client) {
			this.clientId = clientid;
			this.solvis = solvis;
			this.client = client;
		}

		public void reconnect(Client client) {
			this.abort();
			this.client = client;
		}

		public synchronized void abort() {
			if (this.closingThread != null) {
				this.closingThread.abort();
				this.closingThread = null;
			}
		}
	}

	public synchronized ClientAssignments get(Client client) {
		for (ClientAssignments assignments : this.clients) {
			if (assignments.client == client) {
				return assignments;
			}
		}
		return null;
	}

	public synchronized ClientAssignments get(int clientId) {
		for (ClientAssignments assignments : this.clients) {
			if (assignments.clientId == clientId) {
				return assignments;
			}
		}
		return null;
	}

	public synchronized void register(ClientAssignments assignment) {

	}

	public synchronized ClientAssignments unregister(ClientAssignments assignments) {
		for (Iterator<ClientAssignments> it = this.clients.iterator(); it.hasNext();) {
			ClientAssignments assignmentsC = it.next();
			if (assignmentsC == assignments) {
				this.screenRestoreInhibit(false, assignments);
				it.remove();
				return assignments;
			}
		}
		return null;
	}

	public boolean isSolvisConnected(Solvis solvis) {
		for (ClientAssignments assignments : this.clients) {
			if (assignments.solvis == solvis) {
				return true;
			}
		}
		return false;
	}

	private synchronized int getNewClientId() {
		return this.nextClientId++;
	}

	public synchronized void clientClosed(Client client) {

		ClientAssignments assignments = this.get(client);
		// this.unregister(client);
		if (assignments != null) {
			assignments.solvis.getDistributor().unregister(client);
			assignments.closingThread = new ClosingThread(assignments);
			if (this.executor != null) {
				this.executor.submit(assignments.closingThread);
			}
			assignments.client = null;
		}

	}

	private class ClosingThread implements Runnable {
		private final ClientAssignments assignments;
		private boolean abort = false;

		public ClosingThread(ClientAssignments assignments) {
			this.assignments = assignments;
		}

		@Override
		public void run() {
			int delay = assignments.solvis.getSolvisDescription().getMiscellaneous().getConnectionHoldTime();
			synchronized (this) {
				try {
					this.wait(delay);
				} catch (InterruptedException e) {
				}
				if (!abort) {
					unregister(assignments);
				}
			}
		}

		public synchronized void abort() {
			this.abort = true;
			this.notifyAll();
		}

	}

	public void abort() {
		for (ClientAssignments assignments : this.clients) {
			assignments.abort();
		}
		if (executor != null) {
			this.executor.shutdown();
			this.executor = null;
		}
	}

}
