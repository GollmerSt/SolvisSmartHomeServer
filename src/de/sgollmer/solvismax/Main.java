/************************************************************************
 * 
 * $Id$
 *
 * 
 ************************************************************************/

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
import de.sgollmer.solvismax.connection.TerminateClient;
import de.sgollmer.solvismax.error.LearningError;
import de.sgollmer.solvismax.error.XmlError;
import de.sgollmer.solvismax.helper.AbortHelper;
import de.sgollmer.solvismax.log.Logger2;
import de.sgollmer.solvismax.model.Instances;
import de.sgollmer.solvismax.xml.BaseControlFileReader;

public class Main {

	public static final Pattern cmdPattern = Pattern.compile("--([^=]*)(=(.*)){0,1}");
	
	public static void main(String[] args) {

		BaseData baseData = null;
		try {
			baseData = new BaseControlFileReader().read().getTree();
		} catch (IOException | XmlError | XMLStreamException e) {
			e.printStackTrace();
			System.err.println("base.xml couldn't be read.");
			System.exit(40);
		}

		String path = baseData.getWritablePath();

		try {
			Logger2.createInstance(path);
		} catch (IOException e) {
			e.printStackTrace();
			System.err.println("Log4j couldn't initalized");
		}

		final Logger logger = LogManager.getLogger(Main.class);
		boolean learn = false ;

		for (String arg : args) {
			Matcher matcher = cmdPattern.matcher(arg);
			if (!matcher.matches()) {
				System.err.println("Unknowwn argument: " + arg);
			} else {

				String command = matcher.group(1);
				//String value = matcher.group(2);

				switch (command) {
					case "server-terminate":
						int port = baseData.getPort();
						TerminateClient client = new TerminateClient(port) ;
						try {
							client.connectAndTerminateOtherServer();
						} catch (IOException e) {
							e.printStackTrace();
							System.err.println("Terminate not successfull.");
						}
						System.exit(0);
						break;
					case "server-learn":
						learn = true ;;
						break;
					default:
						System.err.println("Unknowwn argument: " + arg);
						break;
				}
			}
		}


		ServerSocket serverSocket = null;

		try {
			serverSocket = new ServerSocket(baseData.getPort());
		} catch (IOException e) {
			System.err.println("Port " + baseData.getPort() + " is in use.");
			System.exit(40);
		}

		Instances tempInstances = null;

		try {
			tempInstances = new Instances(baseData);
		} catch (IOException | XmlError | XMLStreamException | LearningError e) {
			logger.error("Exception on reading configuration or learning files occured, cause:", e);
			e.printStackTrace();
			System.exit(-1);
		} finally {

		}
		
		if ( learn) {
			System.exit(0); ;
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

		// runnable.run();

	}

}
