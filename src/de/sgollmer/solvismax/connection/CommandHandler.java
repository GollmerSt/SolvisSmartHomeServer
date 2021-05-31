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

import de.sgollmer.solvismax.Constants;
import de.sgollmer.solvismax.Constants.ExitCodes;
import de.sgollmer.solvismax.Main;
import de.sgollmer.solvismax.connection.Server.Client;
import de.sgollmer.solvismax.connection.transfer.Command;
import de.sgollmer.solvismax.connection.transfer.ConnectedPackage;
import de.sgollmer.solvismax.connection.transfer.ConnectionState;
import de.sgollmer.solvismax.connection.transfer.DescriptionsPackage;
import de.sgollmer.solvismax.error.ClientAssignmentException;
import de.sgollmer.solvismax.error.CommandError;
import de.sgollmer.solvismax.error.FileException;
import de.sgollmer.solvismax.error.JsonException;
import de.sgollmer.solvismax.error.TypeException;
import de.sgollmer.solvismax.helper.Helper;
import de.sgollmer.solvismax.log.LogManager;
import de.sgollmer.solvismax.log.LogManager.ILogger;
import de.sgollmer.solvismax.model.Instances;
import de.sgollmer.solvismax.model.Solvis;
import de.sgollmer.solvismax.model.command.CommandSetScreen;
import de.sgollmer.solvismax.model.objects.ChannelDescription;
import de.sgollmer.solvismax.model.objects.data.SingleData;
import de.sgollmer.solvismax.model.objects.data.SolvisData;
import de.sgollmer.solvismax.model.objects.screen.AbstractScreen;
import de.sgollmer.solvismax.model.objects.screen.Screen;

public class CommandHandler {

	private static final ILogger logger = LogManager.getInstance().getLogger(CommandHandler.class);

	private final Collection<ClientAssignments> clients;
	private final Instances instances;
	private long nextClientId;
	private boolean abort = false;

	public CommandHandler(final Instances instances) {
		this.nextClientId = 0x00000000ffffffffL & Long.hashCode(System.currentTimeMillis());
		this.instances = instances;
		this.clients = new ArrayList<>();

	}

	public boolean commandFromClient(final IReceivedData receivedData, final IClient client)
			throws IOException, ClientAssignmentException, JsonException, TypeException {
		Command command = receivedData.getCommand();
		if (command == null) {
			return false;
		}
		if (command != Command.SERVER_COMMAND && command != Command.SET) {
			logger.info("Command <" + command.name() + "> received");
		}
		boolean abortConnection = false;
		try {
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
					this.get(receivedData);
					break;
				case SET:
					this.set(receivedData);
					break;
				case SERVER_COMMAND:
					this.executeServerCommand(receivedData, client);
					break;
				case SELECT_SCREEN:
					this.selectScreen(receivedData, client);
					break;
				case CLIENT_ONLINE:
					this.clientOnline(receivedData, client);
					break;
				default:
					logger.warn("Command <" + command.name() + ">unknown, old version of SolvisSmartHomeServer?");
					break;
			}
			return abortConnection;
		} catch (CommandError e) {
			client.sendCommandError(e.getMessage());
			return abortConnection;
		}
	}

	private void executeServerCommand(final IReceivedData receivedData, final IClient client)
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
				assignments.enableControlCommands(solvis, false);
				break;
			case GUI_COMMANDS_ENABLE:
				assignments.enableControlCommands(solvis, true);
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
			case DEBUG_ENABLE:
				Constants.Debug.DEBUG = true;
				break;
			case DEBUG_DISABLE:
				Constants.Debug.DEBUG = false;
				break;
			case LOG_STANDARD:
				LogManager.getInstance().setBufferedMessages(false);
				break;
			case LOG_BUFFERED:
				LogManager.getInstance().setBufferedMessages(true);
				break;

			default:
				String message = "Server command <" + commandString + ">unknown.";
				client.sendCommandError(message);
				break;
		}

	}

	private void selectScreen(final IReceivedData receivedData, final IClient client) {

		ClientAssignments assignments = null;
		Solvis solvis = null;

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

		String screenId = (String) receivedData.getSingleData().get();

		AbstractScreen screen = null;

		if (!screenId.equals("NONE")) {
			screen = solvis.getSolvisDescription().getScreens().get(screenId, solvis);

			if (screen == null || !(screen instanceof Screen)) {
				client.sendCommandError("Screen id unknown");
				return;
			} else if (screen.isNoRestore()) {
				client.sendCommandError("Screen <" + screenId + "> not usable for select screen command");
				return;
			}
		}

		CommandSetScreen command = new CommandSetScreen(screen);

		solvis.execute(command);
	}

	private synchronized ClientAssignments createClientAssignments(final IClient client) {
		ClientAssignments assignments = new ClientAssignments(this, client);
		this.clients.add(assignments);
		return assignments;
	}

	private void clientOnline(final IReceivedData receivedData, IClient client) {
		boolean online = (Boolean) receivedData.getSingleData().get();
		synchronized (this) {
			ClientAssignments assignments = this.get(client);
			if (online) {
				if (assignments != null) {
					assignments.reconnected(client);
				} else {
					this.createClientAssignments(client);
				}
			} else {
				this.clientClosed(assignments);
			}
		}
	}

	private boolean connect(final IReceivedData receivedData, final IClient client) {
		if (!(client instanceof Client)) {
			logger.error("Client doesn't exists. Connection ignored.");
			return true;
		}
		long clientId = this.getNewClientId();
		((Client) client).setClientId(Long.toString(clientId));
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
			DescriptionsPackage channelDescription = new DescriptionsPackage(solvis);
			client.send(channelDescription);

			this.sendMeasurements(solvis, (Client) client);
		} else {
			client.send(new ConnectedPackage(clientId)); // Only for termination
		}
		return false;
	}

	private boolean reconnect(final IReceivedData receivedData, final IClient client) {
		if (!(client instanceof Client)) {
			logger.error("Client doesn't exists. Reconnection ignored.");
			return true;
		}
		String clientId = (String) receivedData.getSingleData().get();
		((Client) client).setClientId(clientId);
		synchronized (this) {
			ClientAssignments assignments = this.get(client);
			if (assignments != null) {
				Solvis solvis = assignments.getSolvis();
				((Client) client).setSolvis(solvis);

				Client former = (Client) assignments.getClient(); // only Server/Client
				if (former != null) {
					former.close();
					solvis.getDistributor().unregister(former);
				}

				assignments.reconnected(client);
				solvis.getDistributor().register((Client) client);

				sendMeasurements(solvis, client);
				return false;
			}
		}
		client.send(new ConnectionState(ConnectionStatus.CLIENT_UNKNOWN, "Client id unknown"));
		client.closeDelayed();
		return true;
	}

	private void sendMeasurements(final Solvis solvis, final IClient client) {
		solvis.getDistributor().sendCollection(solvis.getAllSolvisData().getMeasurements());
		client.send(solvis.getSolvisState().getSolvisStatePackage());
		client.send(solvis.getHumanAccessPackage());

	}

	private void disconnect(final IReceivedData receivedData, final IClient client, final boolean shutdown)
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

	private void set(final IReceivedData receivedDat) throws JsonException, TypeException, CommandError {
		Solvis solvis = receivedDat.getSolvis();
		String name = receivedDat.getChannelId();

		SolvisData data = solvis.getAllSolvisData().getByName(name);

		if (data == null) {
			throw new CommandError("Channel <" + name + "> is unknown. Command ignored.");
		}
		ChannelDescription description = data.getDescription();

		SingleData<?> singleData = receivedDat.getSingleData();
		boolean ignored;
		try {
			ignored = solvis.setFromExternal(description, singleData);
		} catch (TypeException e) {
			logger.info("Command <SET> received");
			throw new TypeException(e.getMessage() + " Located in revceived command.");
		}
		if (ignored) {
			logger.debug("Command <SET> received");
			logger.debug("Setting the channel <" + name + "> ignored to prevent feedback loops.");
		} else {
			logger.info("Command <SET> received");
			logger.info("Channel <" + name + "> will be set to " + singleData.toString() + ">.");
		}
	}

	private void get(final IReceivedData receivedDat) throws CommandError {
		Solvis solvis = receivedDat.getSolvis();
		String name = receivedDat.getChannelId();

		SolvisData data = solvis.getAllSolvisData().getByName(name);
		if (data == null) {
			throw new CommandError("Channel <" + name + "> is unknown. Command ignored.");
		}
		ChannelDescription description = data.getDescription();
		logger.info("Channel <" + name + "> will be updated by GET command");
		solvis.execute(new de.sgollmer.solvismax.model.command.CommandControl(description, solvis));
	}

	private void terminate(final boolean restart) {
		if (restart) {
			Main.getInstance().restart();
		} else {
			System.exit(ExitCodes.OK);
		}
	}

	private synchronized ClientAssignments get(final IClient client) {
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

	private synchronized ClientAssignments unregister(final ClientAssignments assignments)
			throws ClientAssignmentException {
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

	private synchronized boolean isSolvisConnected(final Solvis solvis) {
		for (ClientAssignments assignments : this.clients) {
			if (assignments.getState(solvis) != null) {
				return true;
			}
		}
		return false;
	}

	private synchronized long getNewClientId() {
		return this.nextClientId++;
	}

	private synchronized void clientClosed(final ClientAssignments assignments) {
		if (!this.abort && assignments != null) {
			IClient client = assignments.getClient();
			if (client instanceof Client) {
				client.getSolvis().getDistributor().unregister((Client) client);
			}
			assignments.setClosingThread(new ClosingThread(assignments));
			assignments.getClosingThread().submit();

		}
	}

	synchronized void clientClosed(final Client client) {
		ClientAssignments assignments = this.get(client);
		if (assignments != null) {
			this.clientClosed(assignments);
		}
	}

	class ClosingThread extends Helper.Runnable implements Runnable {
		private final ClientAssignments assignments;
		private boolean abort = false;

		private ClosingThread(final ClientAssignments assignments) {
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
					this.assignments.getClient().close();
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

	public void handleControlEnable(final Solvis solvis) {
		boolean enable = true;
		for (ClientAssignments client : this.clients) {
			Boolean clientEnable = client.getControlEnabled(solvis);
			if (clientEnable != null) {
				enable &= client.getControlEnabled(solvis);
			}
		}
		solvis.controlEnable(enable);
	}
}
