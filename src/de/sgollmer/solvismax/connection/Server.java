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
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadPoolExecutor;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import de.sgollmer.solvismax.Constants;
import de.sgollmer.solvismax.connection.transfer.JsonPackage;
import de.sgollmer.solvismax.connection.transfer.ReceivedPackageCreator;
import de.sgollmer.solvismax.model.objects.Observer.ObserverI;

public class Server {

	private static final Logger logger = LogManager.getLogger(Server.class);

	private ServerSocket serverSocket;
	private final Collection<Client> connectedClients;
	private final CommandHandler commandHandler;
	private ThreadPoolExecutor executor;
	private final ServerThread serverThread;

	public Server(ServerSocket serverSocket, CommandHandler commandHandler) {
		this.connectedClients = new ArrayList<>(Constants.MAX_CONNECTIONS);
		this.serverSocket = serverSocket;
		this.commandHandler = commandHandler;
		this.executor = (ThreadPoolExecutor) Executors.newFixedThreadPool(Constants.MAX_CONNECTIONS);
		this.serverThread = new ServerThread();
	}

	public class ServerThread extends Thread {

		private boolean abort = false;

		public ServerThread() {
			super("Server");
		}

		@Override
		public void run() {
			while (!abort) {
				try {
					waitForAvailableSocket();
					Socket clientSocket = serverSocket.accept();
					Client client = new Client(clientSocket) ;
					addClient(client);
					executor.execute(client );

				} catch (Throwable e) {
					synchronized (this) {

					}
					if (!abort) {
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
			this.abort = true ;
			try {
				serverSocket.close();
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

	public class Client implements Runnable, ObserverI<JsonPackage> {

		private Socket socket;

		public Client(Socket socket) {
			this.socket = socket;
		}

		@Override
		public void run() {

			logger.info("Client connected from " + this.socket.getInetAddress().toString());

			try {
				InputStream in = this.socket.getInputStream();

				boolean abortConnection = false;

				while (!abortConnection) {
					JsonPackage jsonPackage = ReceivedPackageCreator.getInstance().receive(in);
					abortConnection = commandHandler.commandFromClient(jsonPackage, this);
				}

			} catch (Throwable e) {
				logger.debug("Client connection closed. cause:", e);
			}
			this.close();
		}

		public synchronized void send(JsonPackage jsonPackage) {
			try {
				if (this.socket != null) {
					jsonPackage.send(this.socket.getOutputStream());
				}
			} catch (IOException e) {
				logger.debug("IOException occured. Cause:", e);
				/**
				 * Im Falle einer fehlerhaften Datenübertragung wird die Verbindung getrennt.
				 * Der Client sollte sie wieder aufbauen, falls er noch existiert
				 */
				this.close();
			}

		}

		public synchronized void close() {
			logger.info("Client disconnected");
			try {
				removeClient(this);
				commandHandler.clientClosed(this);
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

	}

	public void start() {
		this.serverThread.start();
	}

	public synchronized void abort() {
		for ( Iterator<Client > it = connectedClients.iterator(); it.hasNext();) {
			Client client = it.next() ;
			client.abort();
			it.remove();
		}
		this.serverThread.abort();
		this.notifyAll();
		this.executor.shutdown();
	}
}
