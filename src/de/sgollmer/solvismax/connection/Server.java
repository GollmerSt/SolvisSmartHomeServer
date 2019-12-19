package de.sgollmer.solvismax.connection;

import java.io.IOException;
import java.io.InputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.Collection;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadPoolExecutor;

import org.slf4j.LoggerFactory;

import de.sgollmer.solvismax.Constants;
import de.sgollmer.solvismax.connection.transfer.JsonPackage;
import de.sgollmer.solvismax.connection.transfer.ReceivedPackageCreator;
import de.sgollmer.solvismax.model.objects.Observer.ObserverI;

public class Server {

	private static final org.slf4j.Logger logger = LoggerFactory.getLogger(Server.class);

	private ServerSocket serverSocket;
	private final Collection<Socket> connectedClients;
	private final CommandHandler commandHandler;
	private ThreadPoolExecutor executor;
	private final ServerThread serverThread;

	public Server(int port, CommandHandler commandHandler) throws IOException {
		this.connectedClients = new ArrayList<>(Constants.MAX_CONNECTIONS);
		this.serverSocket = new ServerSocket(port);
		this.commandHandler = commandHandler;
		this.executor = (ThreadPoolExecutor) Executors.newFixedThreadPool(Constants.MAX_CONNECTIONS);
		this.serverThread = new ServerThread();
	}

	public class ServerThread extends Thread {

		private boolean terminate = false;

		public ServerThread() {
			super("Server");
		}

		@Override
		public void run() {
			while (!terminate) {
				try {
					waitForAvailableSocket();
					Socket client = serverSocket.accept();
					addClient(client);
					executor.execute(new Client(client));

				} catch (Throwable e) {
					e.printStackTrace();
					logger.error("Unexpected termination of server", e);
					try {
						Thread.sleep(Constants.RETRY_STARTING_SERVER_TIME);
					} catch (InterruptedException e1) {
					}
				}
			}

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

	private void addClient(Socket client) {
		synchronized (this.connectedClients) {
			this.connectedClients.add(client);
		}
	}

	private void removeClient(Socket client) {
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

				boolean terminateConnection = false;

				while (!terminateConnection) {
					JsonPackage jsonPackage = ReceivedPackageCreator.getInstance().receive(in);
					terminateConnection = commandHandler.commandFromClient(jsonPackage, this);
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
				 * Im Falle einer fehlerhaften Datenübertragung wird die
				 * Verbindung getrennt. Der Client sollte sie wieder aufbauen,
				 * falls er noch existiert
				 */
				this.close();
			}

		}

		public synchronized void close() {
			logger.info("Client disconnected");
			try {
				removeClient(this.socket);
				commandHandler.clientClosed(this);
				if (this.socket != null)
					this.socket.close(); 
				this.socket = null;
				this.notifyAll();
			} catch (IOException e) {
			}
		}

		@Override
		public synchronized void update(JsonPackage data, Object source ) {
			this.send(data);

		}

		public synchronized void closeDelayed() {
			try {
				this.wait(Constants.DELAYED_CLOSING_TIME);
			} catch (InterruptedException e) {
			}
		}

	}

	public void start() {
		this.serverThread.start();
	}
}
