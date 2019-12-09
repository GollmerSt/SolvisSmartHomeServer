package de.sgollmer.solvismax.connection;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;

import javax.xml.stream.XMLStreamException;

import de.sgollmer.solvismax.connection.Server.Client;
import de.sgollmer.solvismax.error.JsonError;
import de.sgollmer.solvismax.error.XmlError;
import de.sgollmer.solvismax.model.Instances;
import de.sgollmer.solvismax.model.Solvis;
import de.sgollmer.solvismax.model.objects.DataDescription;
import de.sgollmer.solvismax.model.objects.data.ModeI;
import de.sgollmer.solvismax.model.objects.data.ModeValue;
import de.sgollmer.solvismax.model.objects.data.SingleData;
import de.sgollmer.solvismax.model.transfer.ConnectPackage;
import de.sgollmer.solvismax.model.transfer.ConnectedPackage;
import de.sgollmer.solvismax.model.transfer.DataDescriptionsPackage;
import de.sgollmer.solvismax.model.transfer.DisconnectPackage;
import de.sgollmer.solvismax.model.transfer.JsonPackage;
import de.sgollmer.solvismax.model.transfer.ReconnectPackage;
import de.sgollmer.solvismax.model.transfer.SetPackage;

public class CommandHandler {

	private final Collection<ClientAssignments> clients;
	private final Instances instances;
	private int nextClientId = Long.hashCode(System.currentTimeMillis());

	public CommandHandler(Instances instances) {
		this.instances = instances;
		this.clients = new ArrayList<>();
	}

	public void commandFromClient(JsonPackage jsonPackage, Client client) throws IOException {
		switch (jsonPackage.getCommand()) {
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
		ClientAssignments assignments = new ClientAssignments(clientId, solvis, client) ;
		this.register(assignments);
		this.clients.add(assignments) ;

		DataDescriptionsPackage dataDescription = new DataDescriptionsPackage(
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
		SingleData<?> singleData = jsonPackage.getSingleData() ;
		if ( description.getModes() != null ) {
			ModeI setMode = null ;
			for ( ModeI mode : description.getModes() ) {
				if ( mode.getName().equals(singleData.toString())) {
					setMode = mode ;
					break ;
				}
			}
			if ( setMode == null ) 
				throw new JsonError("Unknown mode <" + singleData.toString()  + "> in revceived Json package") ;
			singleData = new ModeValue<ModeI>(setMode) ;
		}
		solvis.execute(new de.sgollmer.solvismax.model.Command(description, singleData));
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
