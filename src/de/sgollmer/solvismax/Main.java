package de.sgollmer.solvismax;

import java.io.IOException;
import java.net.ServerSocket;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.xml.stream.XMLStreamException;

import de.sgollmer.solvismax.connection.CommandHandler;
import de.sgollmer.solvismax.connection.Server;
import de.sgollmer.solvismax.error.XmlError;
import de.sgollmer.solvismax.log.Logger2;
import de.sgollmer.solvismax.model.Instances;
import de.sgollmer.solvismax.model.objects.Units;
import de.sgollmer.solvismax.model.objects.Units.Unit;

public class Main {

	public static final Pattern cmdPattern = Pattern.compile("--([^=]*)=(.*)");

	public static void main(String[] args) {

		String id = null;
		String url = null;
		String account = null;
		String password = null;
		int port = 'S' * 128 + 'o';
		String path = null;

		for (String arg : args) {
			Matcher matcher = cmdPattern.matcher(arg);
			if (!matcher.matches()) {
				System.err.println("Unknowwn argument: " + arg);
			} else {

				String command = matcher.group(1);
				String value = matcher.group(2);

				switch (command) {
					case "solvis-id":
						id = value;
						break;
					case "solvis-url":
						url = value;
						break;
					case "solvis-account":
						account = value;
						break;
					case "solvis-password":
						password = value;
						break;
					case "server-port":
						port = Integer.parseInt(value);
						break;
					case "server-path":
						path = value;
						break;
					default:
						System.err.println("Unknowwn argument: " + arg);
						break;
				}
			}
		}
		
		try {
			Logger2.createInstance(path);
		} catch (IOException e2) {
			e2.printStackTrace();
			System.err.println("Log4j couldn't initalized");
		}

		Unit unit = null;

		if (id != null & url != null && account != null && password != null) {
			unit = new Units.Unit(id, url, account, password);
		}
		
		ServerSocket serverSocket = null ;

		try {
			serverSocket = new ServerSocket(port);
		} catch (IOException e1) {
			System.err.println( "Port " + port + " is in use.");
			System.exit(40);
		}

		Instances instances = null;

		try {
			instances = new Instances(path, unit);
		} catch (IOException | XmlError | XMLStreamException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

		try {
			serverSocket.close();
			Server server = new Server(port, 100, new CommandHandler(instances));
			server.start();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

	}

}
