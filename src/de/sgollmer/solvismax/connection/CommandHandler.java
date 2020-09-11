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
import de.sgollmer.solvismax.Main;
import de.sgollmer.solvismax.connection.Server.Client;
import de.sgollmer.solvismax.connection.transfer.Command;
import de.sgollmer.solvismax.connection.transfer.ConnectedPackage;
import de.sgollmer.solvismax.connection.transfer.ConnectionState;
import de.sgollmer.solvismax.connection.transfer.DescriptionsPackage;
import de.sgollmer.solvismax.error.ClientAssignmentException;
import de.sgollmer.solvismax.error.FileException;
import de.sgollmer.solvismax.error.JsonException;
import de.sgollmer.solvismax.error.TypeException;
import de.sgollmer.solvismax.helper.Helper;
import de.sgollmer.solvismax.log.LogManager;
import de.sgollmer.solvismax.log.LogManager.ILogger;
import de.sgollmer.solvismax.model.Instances;
import de.sgollmer.solvismax.model.Solvis;
import de.sgollmer.solvismax.model.objects.ChannelDescription;
import de.sgollmer.solvismax.model.objects.data.SingleData;

public class CommandHandler {

	private static final ILogger logger = LogManager.getInstance().getLogger(CommandHandler.class);

	private final Collection<ClientAssignments> clients;
	private final Instances instances;
	private int nextClientId = Long.hashCode(System.currentTimeMillis());
	private boolean abort = false;

	public CommandHandler(Instances instances) {
		this.instances = instances;
		this.clients = new ArrayList<>();

	}

	public boolean commandFromClient(ITransferedData receivedData, IClient client)
			throws IOException, ClientAssignmentException, JsonException {
		Command command = receivedData.getCommand();
		if (command == null) {
			return false;
		}
		if (command != Command.SERVER_COMMAND) {
			logger.info("Command <" + command.name() + "> received");
		}
		boolean abortConnection = false;
		switch (command) {
			case CONNECT:
				abortConnection = this.connect(receivedData, client);
				break;
			case RECONNECT:
				abortConnection = this.reconnect(receivedData, client);
				break;
			case DISCONNECT:
				this.disconnect(receivedData, client, false);
				break;
			case SHUTDOWN:
				this.disconnect(receivedData, client, true);
				break;
			case GET:
				this.get(receivedData, client);
				break;
			case SET:
				this.set(receivedData, client);
				break;
			case SERVER_COMMAND:
				this.executeServerCommand(receivedData, client);
				break;
			case CLIENT_ONLINE:
				this.clientOnline(receivedData, client);
				break;
			default:
				logger.warn("Command <" + command.name() + ">unknown, old version of SolvisSmartHomeServer?");
				break;
		}
		return abortConnection;
	}

	private void executeServerCommand(ITransferedData receivedData, IClient client)
			throws IOException, ClientAssignmentException {
		ServerCommand command;
		String commandString = (String) receivedData.getSingleData().get();
		try {
			command = ServerCommand.valueOf(commandString);
		} catch (Exception e) {
			client.sendCommandError("Server command <" + commandString + ">unknown");
			return;
		}

		ClientAssignments assignments = null;
		Solvis solvis = null;

		if (!command.isGeneral()) {
			assignments = this.get(client);
			if (assignments == null) {
				client.sendCommandError("Client id unknown");
				client.closeDelayed();
				return;
			}

			solvis = client.getSolvis();
			if (solvis == null) {
				solvis = receivedData.getSolvis();
				assignments.add(solvis);
			}
		}

		logger.info("Server-Command <" + command.name() + "> received");
		switch (command) {
			case BACKUP:
				try {
					this.instances.backupMeasurements();
				} catch (XMLStreamException e) {
					logger.error("XMLStream error while writing the backup file", e);
				} catch (FileException e) {
					logger.error("File error while writing the backup file", e);
				}
				break;
			case SCREEN_RESTORE_INHIBIT:
				assignments.screenRestoreEnable(solvis, false);
				break;
			case SCREEN_RESTORE_ENABLE:
				assignments.screenRestoreEnable(solvis, true);
				break;
			case COMMAND_OPTIMIZATION_INHIBIT:
				assignments.optimizationEnable(solvis, true);
				break;
			case COMMAND_OPTIMIZATION_ENABLE:
				assignments.optimizationEnable(solvis, false);
				break;
			case RESTART:
				this.terminate(true);
				break;
			case GUI_COMMANDS_DISABLE:
				assignments.enableGuiCommands(solvis, false);
				break;
			case GUI_COMMANDS_ENABLE:
				assignments.enableGuiCommands(solvis, true);
				break;
			case SERVICE_RESET:
				assignments.serviceReset(solvis);
				break;
			case UPDATE_CHANNELS:
				assignments.updateControlChannels(solvis);
				break;
			case TERMINATE:
				this.terminate(false);
				break;
			default:
				String message = "Server command <" + commandString + ">unknown.";
				client.sendCommandError(message);
				break;
		}

	}

	private synchronized ClientAssignments createClientAssignments(IClient client) {
		ClientAssignments assignments = new ClientAssignments(client);
		this.clients.add(assignments);
		return assignments;
	}

	private void clientOnline(ITransferedData receivedData, IClient client) {
		boolean online = (Boolean) receivedData.getSingleData().get();
		synchronized (this) {
			ClientAssignments assignments = this.get(client);
			if (online) {
				if (assignments != null) {
					assignments.reconnect(client);
				} else {
					this.createClientAssignments(client);
				}
			} else {
				this.clientClosed(assignments);
			}
		}
	}

	private boolean connect(ITransferedData receivedData, IClient client) {
		if (!(client instanceof Client)) {
			logger.error("Client doesn't exists. Connection ignored.");
			return true;
		}
		int clientId = this.getNewClientId();
		((Client) client).setClientId(Integer.toString(clientId));
		if (receivedData.getSingleData().get() != null) {
			Solvis solvis = null;
			solvis = this.instances.getInstance((String) receivedData.getSingleData().get());
			if (solvis == null) {
				client.send(new ConnectionState(ConnectionStatus.CONNECTION_NOT_POSSIBLE, "Solvis id unknown"));
				client.closeDelayed();
				return true;
			}
			((Client) client).setSolvis(solvis);
			solvis.getDistributor().register((Client) client);
			ClientAssignments assignments = this.createClientAssignments(client);
			assignments.add(solvis);
			client.send(new ConnectedPackage(clientId));
			DescriptionsPackage channelDescription = new DescriptionsPackage(
					this.instances.getSolvisDescription().getChannelDescriptions(), solvis);
			client.send(channelDescription);

			this.sendMeasurements(solvis, (Client) client);
		} else {
			client.send(new ConnectedPackage(clientId));
		}
		return false;
	}

	private boolean reconnect(ITransferedData receivedData, IClient client) {
		if (!(client instanceof Client)) {
			logger.error("Client doesn't exists. Reconnection ignored.");
			return true;
		}
		String clientId = (String) receivedData.getSingleData().get();
		((Client) client).setClientId(clientId);
		synchronized (this) {
			ClientAssignments assignments = this.get(client);
			if (assignments != null) {
				Client former = (Client) assignments.getClient(); // only Server/Client
				Solvis solvis = assignments.getSolvis();
				if (former != null) {
					former.close();
					solvis.getDistributor().unregister(former);
				}

				assignments.reconnect(client);
				solvis.getDistributor().register((Client) client);

				sendMeasurements(solvis, client);
				return false;
			}
		}
		client.send(new ConnectionState(ConnectionStatus.CLIENT_UNKNOWN, "Client id unknown"));
		client.closeDelayed();
		return true;
	}

	private void sendMeasurements(Solvis solvis, IClient client) {
		solvis.getDistributor().sendCollection(solvis.getAllSolvisData().getMeasurements());
		client.send(solvis.getSolvisState());
		ConnectionStatus status = solvis.getHumanAccess().getConnectionStatus();
		client.send(new ConnectionState(status));

	}

	private void disconnect(ITransferedData receivedData, IClient client, boolean shutdown)
			throws ClientAssignmentException {
		if (!(client instanceof Client)) {
			logger.error("Client doesn't exists. Disconnection ignored.");
			return;
		}

		ClientAssignments assignments = this.get(client);
		Solvis solvis = client.getSolvis();
		this.unregister(assignments);
		((Client) client).close();
		if (shutdown) {
			if (this.clients.size() > 0 && !this.isSolvisConnected(solvis)) {
				solvis.abort();
			}
		}
	}

	private void set(ITransferedData receivedDat, IClient client) throws JsonException {
		Solvis solvis = receivedDat.getSolvis();
		if (solvis == null) {
			solvis = client.getSolvis();
		}
		ChannelDescription description = solvis.getChannelDescription(receivedDat.getChannelId());
		SingleData<?> singleData = receivedDat.getSingleData();
		try {
			singleData = description.interpretSetData(singleData);
		} catch (TypeException e) {
			throw new JsonException(e.getMessage() + " Located in revceived Json package.");
		}
		boolean ignored = solvis.setFromExternal(description, singleData);
		if (ignored) {
			logger.info("Setting the channel <" + description.getId() + "> ignored to prevent feedback loops.");
		} else {
			logger.info("Channel <" + description.getId() + "> will be set to " + singleData.toString() + ">.");
		}
	}

	private void get(ITransferedData receivedDat, IClient client) {
		Solvis solvis = receivedDat.getSolvis();
		if (solvis == null) {
			solvis = client.getSolvis();
		}
		ChannelDescription description = solvis.getChannelDescription(receivedDat.getChannelId());
		logger.info("Channel <" + description.getId() + "> will be updated by GET command");
		solvis.execute(new de.sgollmer.solvismax.model.CommandControl(description, solvis));
	}

	private void terminate(boolean restart) {
		if (restart) {
			Main.getInstance().restart();
		} else {
			System.exit(ExitCodes.OK);
		}
	}

	private synchronized ClientAssignments get(IClient client) {
		for (ClientAssignments assignments : this.clients) {
			IClient cmp = assignments.getClient();
			if (cmp == null) {
				logger.error("Error in client list. Client not defined");
			} else if (assignments.getClient().equals(client)) {
				return assignments;
			}
		}
		return null;
	}

	private synchronized ClientAssignments unregister(ClientAssignments assignments) throws ClientAssignmentException {
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

	private synchronized boolean isSolvisConnected(Solvis solvis) {
		for (ClientAssignments assignments : this.clients) {
			if (assignments.getState(solvis) != null) {
				return true;
			}
		}
		return false;
	}

	private synchronized int getNewClientId() {
		return this.nextClientId++;
	}

	private synchronized void clientClosed(ClientAssignments assignments) {
		if (!this.abort && assignments != null) {
			IClient client = assignments.getClient();
			if (client instanceof Client) {
				client.getSolvis().getDistributor().unregister((Client) client);
			}
			assignments.setClosingThread(new ClosingThread(assignments));
			assignments.getClosingThread().submit();

		}
	}

	synchronized void clientClosed(Client client) {
		ClientAssignments assignments = this.get(client);
		if (assignments != null) {
			this.clientClosed(assignments);
		}
	}

	class ClosingThread extends Helper.Runnable implements Runnable {
		private final ClientAssignments assignments;
		private boolean abort = false;

		private ClosingThread(ClientAssignments assignments) {
			super("ClosingThread");
			this.assignments = assignments;
		}

		@Override
		public void run() {
			int delay = CommandHandler.this.instances.getSolvisDescription().getMiscellaneous().getConnectionHoldTime();
			synchronized (this) {
				try {
					this.wait(delay);
				} catch (InterruptedException e) {
				}
			}
			synchronized (CommandHandler.this) {
				if (!this.abort) {
					try {
						unregister(this.assignments);
					} catch (ClientAssignmentException e) {
						logger.error("ClientAssignmentError, client missed", e);
					}
				}
			}
		}

		synchronized void abort() {
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
