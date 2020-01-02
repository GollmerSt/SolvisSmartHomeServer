package de.sgollmer.solvismax;

import java.io.IOException;
import java.net.ServerSocket;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.xml.stream.XMLStreamException;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import de.sgollmer.solvismax.connection.CommandHandler;
import de.sgollmer.solvismax.connection.Server;
import de.sgollmer.solvismax.error.LearningError;
import de.sgollmer.solvismax.error.XmlError;
import de.sgollmer.solvismax.helper.AbortHelper;
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

		final Logger logger = LogManager.getLogger(Main.class);

		Unit unit = null;

		if (id != null & url != null && account != null && password != null) {
			unit = new Units.Unit(id, url, account, password);
		}

		ServerSocket serverSocket = null;

		try {
			serverSocket = new ServerSocket(port);
		} catch (IOException e1) {
			System.err.println("Port " + port + " is in use.");
			System.exit(40);
		}

		Instances tempInstances = null;

		try {
			tempInstances = new Instances(path, unit);
		} catch (IOException | XmlError | XMLStreamException | LearningError e) {
			logger.error("Exception on reading configuration or learning files occured, cause:", e);
			e.printStackTrace();
			System.exit(-1);
		} finally {
			
		}

		final Instances instances = tempInstances;
		final CommandHandler commandHandler = new CommandHandler(instances);
		Server server = new Server(serverSocket, commandHandler);
		server.start();

		
		Runnable runnable = new Runnable() {
			
			@Override
			public void run() {
				instances.abort();
				commandHandler.abort();
				if (server != null) {
					server.abort();
				}
				AbortHelper.getInstance().abort();
			}
		};

		Runtime.getRuntime().addShutdownHook(new Thread(runnable));
		
		//runnable.run();
		
	}

}
