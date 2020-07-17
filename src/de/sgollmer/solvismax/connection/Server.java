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
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;

import de.sgollmer.solvismax.Constants;
import de.sgollmer.solvismax.connection.transfer.ConnectionState;
import de.sgollmer.solvismax.connection.transfer.JsonPackage;
import de.sgollmer.solvismax.connection.transfer.ReceivedPackageCreator;
import de.sgollmer.solvismax.helper.Helper;
import de.sgollmer.solvismax.log.LogManager;
import de.sgollmer.solvismax.log.LogManager.ILogger;
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

	public Server(ServerSocket serverSocket, CommandHandler commandHandler, Miscellaneous misc) {
		this.connectedClients = new ArrayList<>(Constants.MAX_CONNECTIONS);
		this.serverSocket = serverSocket;
		this.commandHandler = commandHandler;
		this.clientTimeout = misc.getClientTimeoutTime_ms();
		this.serverThread = new ServerThread();
	}

	public class ServerThread extends Thread {

		private boolean abort = false;

		public ServerThread() {
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
					synchronized (this) {

					}
					if (!this.abort) {
						e.printStackTrace();
						logger.error("Unexpected termination of server", e);
						try {
							this.wait(Constants.RETRY_STARTING_SERVER_TIME);
						} catch (InterruptedException e1) {
						}
					}
				}
			}

		}

		public synchronized void abort() {
			this.abort = true;
			try {
				Server.this.serverSocket.close();
			} catch (IOException e) {
			}
			this.notifyAll();
		}
	}

	private void waitForAvailableSocket() {
		synchronized (this.connectedClients) {
			if (this.connectedClients.size() >= Constants.MAX_CONNECTIONS) {
				try {
					this.connectedClients.wait();
				} catch (InterruptedException e) {
				}
			}
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
			this.connectedClients.notifyAll();
		}
	}

	public class Client extends Helper.Runnable implements IObserver<JsonPackage>, IClient {

		private Socket socket;

		public Client(Socket socket) {
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
					abortConnection = Server.this.commandHandler.commandFromClient(jsonPackage, this);
				}

			} catch (Throwable e) {
				if (!Server.this.abort) {
					logger.info("Client connection closed. cause:", e);
				}
			}
			this.close();
		}

		@Override
		public synchronized void send(JsonPackage jsonPackage) {
			try {
				if (this.socket != null) {
					jsonPackage.send(this.socket.getOutputStream());
				}
			} catch (IOException e) {
				logger.info("IOException occured. Cause:", e);
				/**
				 * Im Falle einer fehlerhaften Daten�bertragung wird die Verbindung getrennt.
				 * Der Client sollte sie wieder aufbauen, falls er noch existiert
				 */
				this.close();
			}

		}

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
				this.socket.close();
			} catch (IOException e) {
			}
		}

		public synchronized void abort() {
			try {
				this.socket.close();
			} catch (IOException e) {
			}
			this.notifyAll();
		}

		@Override
		public void sendCommandError(String message) {
			logger.info(message);
			ConnectionState state = new ConnectionState(ConnectionStatus.COMMAND_ERROR, message) ; 
			this.send(state.createJsonPackage());
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
				it.remove(); // Wird durch client.abort gel�scht
			}
		}
		this.serverThread.abort();
	}
}
