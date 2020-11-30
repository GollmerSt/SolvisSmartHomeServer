/************************************************************************
 * 
 * $Id$
 *
 * 
 ************************************************************************/

package de.sgollmer.solvismax.connection;

import java.io.IOException;
import java.io.InputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.concurrent.Semaphore;

import de.sgollmer.solvismax.Constants;
import de.sgollmer.solvismax.connection.transfer.ConnectionState;
import de.sgollmer.solvismax.connection.transfer.JsonPackage;
import de.sgollmer.solvismax.connection.transfer.ReceivedPackageCreator;
import de.sgollmer.solvismax.error.ConnectionClosedException;
import de.sgollmer.solvismax.error.TerminationException;
import de.sgollmer.solvismax.helper.AbortHelper;
import de.sgollmer.solvismax.helper.Helper;
import de.sgollmer.solvismax.log.LogManager;
import de.sgollmer.solvismax.log.LogManager.ILogger;
import de.sgollmer.solvismax.model.Solvis;
import de.sgollmer.solvismax.model.objects.Miscellaneous;
import de.sgollmer.solvismax.model.objects.Observer.IObserver;

public class Server {

	private static final ILogger logger = LogManager.getInstance().getLogger(Server.class);

	private ServerSocket serverSocket;
	private final Collection<Client> connectedClients;
	private final CommandHandler commandHandler;
	private final ServerThread serverThread;
	private final int clientTimeout;
	private boolean abort = false;
	private final Semaphore permits = new Semaphore(Constants.MAX_CONNECTIONS);

	public Server(ServerSocket serverSocket, CommandHandler commandHandler, Miscellaneous misc) {
		this.connectedClients = new ArrayList<>(Constants.MAX_CONNECTIONS);
		this.serverSocket = serverSocket;
		this.commandHandler = commandHandler;
		this.clientTimeout = misc.getClientTimeoutTime_ms();
		this.serverThread = new ServerThread();
	}

	private class ServerThread extends Thread {

		private boolean abort = false;

		private ServerThread() {
			super("Server");
		}

		@Override
		public void run() {
			while (!this.abort) {
				try {
					waitForAvailableSocket();
					Socket clientSocket = Server.this.serverSocket.accept();
					Client client = new Client(clientSocket);
					addClient(client);
					client.submit();

				} catch (Throwable e) {
					if (!this.abort) {
						e.printStackTrace();
						logger.error("Unexpected termination of server", e);
						try {
							AbortHelper.getInstance().sleep(Constants.RETRY_STARTING_SERVER_TIME);
						} catch (TerminationException e1) {
							return;
						}
					}
				}
			}

		}

		private synchronized void abort() {
			this.abort = true;
			try {
				Server.this.serverSocket.close();
			} catch (IOException e) {
			}
			this.notifyAll();
		}
	}

	private void waitForAvailableSocket() {
		try {
			this.permits.acquire();
		} catch (InterruptedException e1) {
		}
	}

	private void addClient(Client client) {
		synchronized (this.connectedClients) {
			this.connectedClients.add(client);
		}
	}

	private void removeClient(Client client) {
		synchronized (this.connectedClients) {
			this.connectedClients.remove(client);
			this.permits.release();
		}
	}

	class Client extends Helper.Runnable implements IObserver<JsonPackage>, IClient {

		private Socket socket;
		private String clientId;
		private Solvis solvis;

		private Client(Socket socket) {
			super("Client");
			this.socket = socket;
		}

		@Override
		public void run() {
			logger.info("Client connected from " + this.socket.getInetAddress().toString());

			try {
				InputStream in = this.socket.getInputStream();

				boolean abortConnection = false;

				while (!abortConnection) {
					JsonPackage jsonPackage = ReceivedPackageCreator.getInstance().receive(in,
							Server.this.clientTimeout);
					jsonPackage.setSolvis(this.solvis);
					abortConnection = Server.this.commandHandler.commandFromClient(jsonPackage, this);
				}

			} catch (ConnectionClosedException e) {
				logger.info(e.getMessage());
			} catch (SocketException e ) {
				logger.info("Client connection closed by client.");
			} catch (Throwable e) {
				if (!Server.this.abort) {
					logger.info("Client connection closed. cause:", e);
				}
			}
			this.close();
		}

		private synchronized void send(JsonPackage jsonPackage) {
			try {
				if (this.socket != null) {
					jsonPackage.send(this.socket.getOutputStream());
				}
			} catch (IOException e) {
				logger.info("IOException occured. Cause:", e);
				/**
				 * Im Falle einer fehlerhaften Datenübertragung wird die Verbindung getrennt.
				 * Der Client sollte sie wieder aufbauen, falls er noch existiert
				 */
				this.close();
			}

		}

		@Override
		public synchronized void close() {
			logger.info("Client disconnected");
			try {
				removeClient(this);
				Server.this.commandHandler.clientClosed(this);
				if (this.socket != null)
					this.socket.close();
				this.socket = null;
				this.notifyAll();
			} catch (IOException e) {
			}
		}

		@Override
		public synchronized void update(JsonPackage data, Object source) {
			this.send(data);

		}

		@Override
		public synchronized void closeDelayed() {
			try {
				this.wait(Constants.DELAYED_CLOSING_TIME);
			} catch (InterruptedException e) {
			}
			try {
				if (this.socket != null) {
					this.socket.close();
				}
			} catch (IOException e) {
			}
		}

		private synchronized void abort() {
			try {
				this.socket.close();
			} catch (IOException e) {
			}
			this.notifyAll();
		}

		@Override
		public void sendCommandError(String message) {
			logger.info(message);
			ConnectionState state = new ConnectionState(ConnectionStatus.COMMAND_ERROR, message);
			this.send(state.createJsonPackage());
		}

		@Override
		public String getClientId() {
			return this.clientId;
		}

		void setClientId(String clientId) {
			this.clientId = clientId;
		}

		@Override
		public boolean equals(Object obj) {
			if (!(obj instanceof Client)) {
				return false;
			}
			if (this.clientId == null) {
				return false;
			}
			return this.clientId.equals(((Client) obj).getClientId());
		}

		@Override
		public int hashCode() {
			if (this.clientId == null) {
				return 263;
			}
			return this.clientId.hashCode();
		}

		@Override
		public void send(ISendData sendData) {
			this.send(sendData.createJsonPackage());

		}

		@Override
		public Solvis getSolvis() {
			return this.solvis;
		}

		void setSolvis(Solvis solvis) {
			this.solvis = solvis;
		}

	}

	public void start() {
		this.serverThread.start();
	}

	public void abort() {
		this.abort = true;
		synchronized (this.connectedClients) {
			for (Iterator<Client> it = this.connectedClients.iterator(); it.hasNext();) {
				Client client = it.next();
				client.abort();
				it.remove(); // Wird durch client.abort gelöscht
			}
		}
		this.serverThread.abort();
	}
}
