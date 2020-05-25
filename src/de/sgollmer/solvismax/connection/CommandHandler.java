/************************************************************************
 * 
 * $Id$
 *
 * 
 ************************************************************************/

package de.sgollmer.solvismax.connection;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;

import javax.xml.stream.XMLStreamException;

import de.sgollmer.solvismax.Constants.ExitCodes;
import de.sgollmer.solvismax.Restart;
import de.sgollmer.solvismax.connection.Server.Client;
import de.sgollmer.solvismax.connection.transfer.Command;
import de.sgollmer.solvismax.connection.transfer.ConnectPackage;
import de.sgollmer.solvismax.connection.transfer.ConnectedPackage;
import de.sgollmer.solvismax.connection.transfer.ConnectionState;
import de.sgollmer.solvismax.connection.transfer.DescriptionsPackage;
import de.sgollmer.solvismax.connection.transfer.DisconnectPackage;
import de.sgollmer.solvismax.connection.transfer.GetPackage;
import de.sgollmer.solvismax.connection.transfer.JsonPackage;
import de.sgollmer.solvismax.connection.transfer.MeasurementsPackage;
import de.sgollmer.solvismax.connection.transfer.ReconnectPackage;
import de.sgollmer.solvismax.connection.transfer.ServerCommandPackage;
import de.sgollmer.solvismax.connection.transfer.SetPackage;
import de.sgollmer.solvismax.error.JsonError;
import de.sgollmer.solvismax.error.LearningError;
import de.sgollmer.solvismax.error.TypeError;
import de.sgollmer.solvismax.error.XmlError;
import de.sgollmer.solvismax.helper.Helper;
import de.sgollmer.solvismax.log.LogManager;
import de.sgollmer.solvismax.log.LogManager.Logger;
import de.sgollmer.solvismax.model.Instances;
import de.sgollmer.solvismax.model.Solvis;
import de.sgollmer.solvismax.model.objects.ChannelDescription;
import de.sgollmer.solvismax.model.objects.data.SingleData;

public class CommandHandler {

	private static final Logger logger = LogManager.getInstance().getLogger(CommandHandler.class);

	private final Collection<ClientAssignments> clients;
	private final Instances instances;
	private int nextClientId = Long.hashCode(System.currentTimeMillis());
	private boolean abort = false;

	public CommandHandler(Instances instances) {
		this.instances = instances;
		this.clients = new ArrayList<>();

	}

	public boolean commandFromClient(JsonPackage jsonPackage, Client client) throws IOException {
		Command command = jsonPackage.getCommand();
		if (command != Command.SERVER_COMMAND) {
			logger.info("Command <" + command.name() + "> received");
		}
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
				this.executeServerCommand((ServerCommandPackage) jsonPackage, client);
				break;
			case TERMINATE:
				if (this.get(client) == null) {
					System.exit(ExitCodes.OK);
				}
				break;
			default:
				logger.warn("Command <" + command.name() + ">unknown, old version of SolvisSmartHomeServer?");
				break;
		}
		return abortConnection;
	}

	private void executeServerCommand(ServerCommandPackage serverCommand, Client client) throws IOException {
		ClientAssignments assignments = this.get(client);
		logger.info("Server-Command <" + serverCommand.getServerCommand().name() + "> received");
		switch (serverCommand.getServerCommand()) {
			case BACKUP:
				try {
					this.instances.backupMeasurements();
				} catch (XMLStreamException e) {
					logger.error("XMLStream error while writing the backup file");
				}
				break;
			case SCREEN_RESTORE_INHIBIT:
				assignments.screenRestoreInhibit(true);
				break;
			case SCREEN_RESTORE_ENABLE:
				assignments.screenRestoreInhibit(false);
				break;
			case COMMAND_OPTIMIZATION_INHIBIT:
				assignments.optimizationInhibit(true);
				break;
			case COMMAND_OPTIMIZATION_ENABLE:
				assignments.optimizationInhibit(false);
				break;
			case RESTART:
				this.restart();
				break;
			case GUI_COMMANDS_DISABLE:
				assignments.enableGuiCommands(false);
				break;
			case GUI_COMMANDS_ENABLE:
				assignments.enableGuiCommands(true);
				break;
			case SERVICE_RESET:
				assignments.serviceReset();
				break;
			case UPDATE_CHANNELS:
				assignments.updateControlChannels();
				break;
			default:
				logger.warn("Server command <" + serverCommand.getServerCommand().name()
						+ ">unknown, old version of SolvisSmartHomeServer?");
				break;
		}

	}

	private boolean connect(ConnectPackage jsonPackage, Client client) {
		int clientId = this.getNewClientId();
		if (jsonPackage.getId() != null) {
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
			ClientAssignments assignments = new ClientAssignments(clientId, solvis, client);
			synchronized (this) {
				this.register(assignments);
				this.clients.add(assignments);
			}
			client.send(new ConnectedPackage(clientId));
			DescriptionsPackage channelDescription = new DescriptionsPackage(
					this.instances.getSolvisDescription().getChannelDescriptions(), solvis.getConfigurationMask());
			client.send(channelDescription);

			client.send(new MeasurementsPackage(solvis.getAllSolvisData().getMeasurements()));
			client.send(solvis.getSolvisState().getPackage());
		} else {
			client.send(new ConnectedPackage(clientId));
		}
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
		Client former = assignments.getClient();
		Solvis solvis = assignments.getSolvis();
		if (former != null) {
			former.close();
			solvis.getDistributor().unregister(former);
		}

		assignments.reconnect(client);
		solvis.getDistributor().register(client);

		client.send(new MeasurementsPackage(solvis.getAllSolvisData().getMeasurements()));
		client.send(solvis.getSolvisState().getPackage());

		return false;
	}

	private void disconnect(DisconnectPackage jsonPackage, Client client, boolean shutdown) {

		ClientAssignments assignments = this.get(client);
		Solvis solvis = assignments.getSolvis();
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
		Solvis solvis = assignments.getSolvis();
		ChannelDescription description = solvis.getChannelDescription(jsonPackage.getId());
		SingleData<?> singleData = jsonPackage.getSingleData();
		logger.info("Channel <" + description.getId() + "> will be set to " + singleData.toString() + ">.");
		try {
			singleData = description.interpretSetData(singleData);
		} catch (TypeError e) {
			throw new JsonError(e.getMessage() + " Located in revceived Json package.");
		}
		solvis.execute(new de.sgollmer.solvismax.model.CommandControl(description, singleData, solvis));
	}

	private void get(GetPackage jsonPackage, Client client) {
		ClientAssignments assignments = this.get(client);
		Solvis solvis = assignments.getSolvis();
		ChannelDescription description = solvis.getChannelDescription(jsonPackage.getId());
		logger.info("Channel <" + description.getId() + "> will be updated by GET command");
		solvis.execute(new de.sgollmer.solvismax.model.CommandControl(description, solvis));
	}

	private void restart() {
		Restart restart = new Restart();
		restart.startRestartProcess();
		System.exit(ExitCodes.OK);
	}

	public synchronized ClientAssignments get(Client client) {
		for (ClientAssignments assignments : this.clients) {
			if (assignments.getClient() == client) {
				return assignments;
			}
		}
		return null;
	}

	public synchronized ClientAssignments get(int clientId) {
		for (ClientAssignments assignments : this.clients) {
			if (assignments.getClientId() == clientId) {
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
				assignments.clientClosed();
				it.remove();
				return assignments;
			}
		}
		return null;
	}

	public synchronized boolean isSolvisConnected(Solvis solvis) {
		for (ClientAssignments assignments : this.clients) {
			if (assignments.getSolvis() == solvis) {
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
		if (!this.abort && assignments != null) {
			assignments.getSolvis().getDistributor().unregister(client);
			assignments.setClosingThread(new ClosingThread(assignments));
			assignments.getClosingThread().submit();
			assignments.setClient(null);
		}

	}

	class ClosingThread extends Helper.Runnable implements Runnable {
		private final ClientAssignments assignments;
		private boolean abort = false;

		public ClosingThread(ClientAssignments assignments) {
			super("ClosingThread");
			this.assignments = assignments;
		}

		@Override
		public void run() {
			int delay = this.assignments.getSolvis().getSolvisDescription().getMiscellaneous().getConnectionHoldTime();
			synchronized (this) {
				try {
					this.wait(delay);
				} catch (InterruptedException e) {
				}
				if (!this.abort) {
					unregister(this.assignments);
				}
			}
		}

		public synchronized void abort() {
			this.abort = true;
			this.notifyAll();
		}

	}

	public synchronized void abort() {
		this.abort = true;
		for (ClientAssignments assignments : this.clients) {
			assignments.abort();
		}
	}

}
