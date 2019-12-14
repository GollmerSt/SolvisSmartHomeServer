package de.sgollmer.solvismax.connection;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;

import javax.xml.stream.XMLStreamException;

import org.slf4j.LoggerFactory;

import de.sgollmer.solvismax.connection.Server.Client;
import de.sgollmer.solvismax.connection.transfer.ChannelDescriptionsPackage;
import de.sgollmer.solvismax.connection.transfer.Command;
import de.sgollmer.solvismax.connection.transfer.ConnectPackage;
import de.sgollmer.solvismax.connection.transfer.ConnectedPackage;
import de.sgollmer.solvismax.connection.transfer.DisconnectPackage;
import de.sgollmer.solvismax.connection.transfer.GetPackage;
import de.sgollmer.solvismax.connection.transfer.JsonPackage;
import de.sgollmer.solvismax.connection.transfer.ReconnectPackage;
import de.sgollmer.solvismax.connection.transfer.SetPackage;
import de.sgollmer.solvismax.error.JsonError;
import de.sgollmer.solvismax.error.XmlError;
import de.sgollmer.solvismax.model.Instances;
import de.sgollmer.solvismax.model.Solvis;
import de.sgollmer.solvismax.model.objects.DataDescription;
import de.sgollmer.solvismax.model.objects.data.ModeI;
import de.sgollmer.solvismax.model.objects.data.ModeValue;
import de.sgollmer.solvismax.model.objects.data.SingleData;

public class CommandHandler {

	private static final org.slf4j.Logger logger = LoggerFactory.getLogger(CommandHandler.class);

	private final Collection<ClientAssignments> clients;
	private final Instances instances;
	private int nextClientId = Long.hashCode(System.currentTimeMillis());

	public CommandHandler(Instances instances) {
		this.instances = instances;
		this.clients = new ArrayList<>();
	}

	public void commandFromClient(JsonPackage jsonPackage, Client client) throws IOException {
		Command command = jsonPackage.getCommand();
		logger.info("Command <" + command.name() + "> received");
		switch (command) {
			case CONNECT:
				this.connect((ConnectPackage) jsonPackage, client);
				break;
			case RECONNECT:
				this.reconnect((ReconnectPackage) jsonPackage, client);
				break;
			case DISCONNECT:
				this.disconnect((DisconnectPackage) jsonPackage, client, false);
				break;
			case SHUTDOWN:
				this.disconnect((DisconnectPackage) jsonPackage, client, true);
				break;
			case GET:
				this.get((GetPackage) jsonPackage, client);
				break ;
			case SET:
				this.set((SetPackage) jsonPackage, client);
				break;
			default:
				// TODO Command unknown
				break;
		}
	}

	private void connect(ConnectPackage jsonPackage, Client client) {
		int clientId = this.getNewClientId();
		Solvis solvis = null;
		try {
			solvis = this.instances.getInstance(jsonPackage);
		} catch (IOException | XmlError | XMLStreamException e1) {
			client.send(new ConnectionState(ConnectionStatus.CONNECTION_NOT_POSSIBLE,
					"Solvis not connected: " + e1.getMessage()).createJsonPackage());
			client.closeDelayed();
			return;
		}
		if (solvis == null) {
			client.send(new ConnectionState(ConnectionStatus.UNKNOWN, "Solvis id unknown").createJsonPackage());
			client.closeDelayed();
			return;
		}
		solvis.getDistributor().register(client);
		client.send(new ConnectedPackage(clientId));
		ClientAssignments assignments = new ClientAssignments(clientId, solvis, client);
		this.register(assignments);
		this.clients.add(assignments);

		ChannelDescriptionsPackage dataDescription = new ChannelDescriptionsPackage(
				this.instances.getSolvisDescription().getDataDescriptions());
		client.send(dataDescription);

		client.send(solvis.getAllSolvisData().getMeasurementsPackage());
	}

	private void reconnect(ReconnectPackage reconnectPackage, Client client) {

		int clientId = reconnectPackage.getClientId();
		ClientAssignments assignments = this.get(clientId);
		if (assignments == null) {
			client.send(new ConnectionState(ConnectionStatus.UNKNOWN, "Client id unknown").createJsonPackage());
			client.closeDelayed();
		}
		Client former = assignments.client;
		former.close();
		assignments.client = client;

		Solvis solvis = assignments.solvis;
		solvis.getDistributor().unregister(former);
		solvis.getDistributor().register(client);

		client.send(solvis.getAllSolvisData().getMeasurementsPackage());
	}

	private void disconnect(DisconnectPackage jsonPackage, Client client, boolean shutdown) {
		Solvis solvis = this.get(client).solvis;
		client.close();
		if (shutdown) {
			if (this.clients.size() > 0 && !this.isSolvisConnected(solvis)) {
				solvis.terminate();
			}
		}
	}

	private void set(SetPackage jsonPackage, Client client) {
		ClientAssignments assignments = this.get(client);
		Solvis solvis = assignments.solvis;
		DataDescription description = solvis.getDataDescription(jsonPackage.getId());
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
		solvis.execute(new de.sgollmer.solvismax.model.Command(description, singleData));
	}

	private void get(GetPackage jsonPackage, Client client) {
		ClientAssignments assignments = this.get(client);
		Solvis solvis = assignments.solvis;
		DataDescription description = solvis.getDataDescription(jsonPackage.getId());
		logger.info("Channel <" + description.getId() + "> will be updated by GET command");
		solvis.execute(new de.sgollmer.solvismax.model.Command(description));
	}

	public class ClientAssignments {
		private final int clientId;
		private final Solvis solvis;
		private Client client;

		public ClientAssignments(int clientid, Solvis solvis, Client client) {
			this.clientId = clientid;
			this.solvis = solvis;
			this.client = client;
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

	public synchronized ClientAssignments unregister(Client client) {
		for (Iterator<ClientAssignments> it = this.clients.iterator(); it.hasNext();) {
			ClientAssignments assignments = it.next();
			if (assignments.client == client) {
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

	public void clientClosed(Client client) {
		ClientAssignments assignments = this.unregister(client);
		if (assignments != null) {
			assignments.solvis.getDistributor().unregister(client);
		}

	}

}
