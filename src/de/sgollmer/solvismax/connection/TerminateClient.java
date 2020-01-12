package de.sgollmer.solvismax.connection;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.Socket;
import java.net.UnknownHostException;

import de.sgollmer.solvismax.connection.transfer.ConnectPackage;
import de.sgollmer.solvismax.connection.transfer.ConnectedPackage;
import de.sgollmer.solvismax.connection.transfer.JsonPackage;
import de.sgollmer.solvismax.connection.transfer.ReceivedPackageCreator;
import de.sgollmer.solvismax.connection.transfer.TerminatePackage;

public class TerminateClient {

	private final int port;

	public TerminateClient(int port) {
		this.port = port;
	}

	public boolean connectAndTerminateOtherServer() throws UnknownHostException, IOException {
		Socket socket = null;
		try {
			socket = new Socket("localhost", port);
		} catch (IOException e) {
			System.out.println("Server not startetd");
			System.exit(0);
		}

		ConnectPackage connectPackage = new ConnectPackage(null);

		InputStream input = socket.getInputStream();
		OutputStream out = socket.getOutputStream();

		connectPackage.send(out);

		JsonPackage jsonPackage = ReceivedPackageCreator.getInstance().receive(input);

		if (!(jsonPackage instanceof ConnectedPackage)) {
			System.err.println("Wrong json package.");
			socket.close();
			return false;
		}

		// int clientId = ((ConnectedPackage) jsonPackage).getClientId();

		TerminatePackage terminatePackage = new TerminatePackage();

		terminatePackage.send(out);

		out.flush();
		socket.close();

		return true;
	}
}
