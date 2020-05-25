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

import de.sgollmer.solvismax.Constants.ExitCodes;
import de.sgollmer.solvismax.connection.CommandHandler;
import de.sgollmer.solvismax.connection.Server;
import de.sgollmer.solvismax.connection.TerminateClient;
import de.sgollmer.solvismax.crypt.CryptAes;
import de.sgollmer.solvismax.error.LearningError;
import de.sgollmer.solvismax.error.XmlError;
import de.sgollmer.solvismax.helper.AbortHelper;
import de.sgollmer.solvismax.log.LogManager;
import de.sgollmer.solvismax.log.LogManager.DelayedMessage;
import de.sgollmer.solvismax.log.LogManager.Level;
import de.sgollmer.solvismax.log.LogManager.LogErrors;
import de.sgollmer.solvismax.log.LogManager.Logger;
import de.sgollmer.solvismax.model.Instances;
import de.sgollmer.solvismax.xml.BaseControlFileReader;
import de.sgollmer.solvismax.xml.XmlStreamReader;

public class Main {

	public static final Pattern cmdPattern = Pattern.compile("--([^=]*)(=(.*)){0,1}");

	private static Logger logger;
	private static Level LEARN;

	public static void main(String[] args) {

		for (String arg : args) {
			Matcher matcher = cmdPattern.matcher(arg);
			if (!matcher.matches()) {
				System.err.println("Unknowwn argument: " + arg);
			} else {

				String command = matcher.group(1);
				String value = null;
				if (matcher.groupCount() >= 3) {
					value = matcher.group(3);
				}

				switch (command) {
					case "string-to-crypt":
						if (value == null) {
							System.err.println("To less arguments!");
							System.exit(Constants.ExitCodes.ARGUMENT_FAIL);
						}
						try {
							String out = new CryptAes().encrypt(value);
							System.out.println(out);
							System.exit(Constants.ExitCodes.OK);
						} catch (Throwable e) {
							System.err.println(e.getMessage());
							System.exit(Constants.ExitCodes.CRYPTION_FAIL);
						}
						break;
				}
			}
		}

		BaseData baseData = null;
		final LogManager logManager = LogManager.getInstance();
		try {
			XmlStreamReader.Result<BaseData> base = new BaseControlFileReader().read();
			if (base == null) {
				throw new XmlError("");
			}
			baseData = base.getTree();
		} catch (IOException | XmlError | XMLStreamException e) {
			e.printStackTrace();
			logManager.addDelayedErrorMessage(new DelayedMessage(Level.FATAL, "base.xml couldn't be read.", Main.class,
					ExitCodes.READING_CONFIGURATION_FAIL));
			LogManager.exit(ExitCodes.READING_CONFIGURATION_FAIL);
		}

		String path = baseData.getWritablePath();

		LogErrors error = LogErrors.INIT;

		try {
			error = logManager.createInstance(path);
		} catch (IOException e) {
			error = LogErrors.INIT;
			e.printStackTrace();
		}

		if (error == LogErrors.INIT) {
			System.err.println("Log4j couldn't initalized");
		} else if (error == LogErrors.PREVIOUS) {
			LogManager.exit(0);
		}

		logger = logManager.getLogger(Main.class);
		logger.info("Server started");
		LEARN = Level.getLevel("LEARN");
		boolean learn = false;

		for (String arg : args) {
			Matcher matcher = cmdPattern.matcher(arg);
			if (!matcher.matches()) {
				System.err.println("Unknowwn argument: " + arg);
			} else {

				String command = matcher.group(1);
				String value = null;
				if (matcher.groupCount() >= 3) {
					value = matcher.group(3);
				}

				switch (command) {
					case "server-terminate":
						logger.info("server-terminate");
						Main.serverTerminateAndExit(baseData);
						break;
					case "server-learn":
						learn = true;
						break;
					case "server-restart":
						logger.info("server-restart");
						Main.serverRestartAndExit(baseData, value);
						break;
					case "test-mail":
						try {
							if (baseData.getExceptionMail() == null) {
								throw new Error(
										"Sending mail not possible in case of mssing or invalid data in <base.xml>");
							}
							baseData.getExceptionMail().send("Test mail", "This is a test mail", null);
							System.exit(Constants.ExitCodes.OK);
						} catch (Throwable e) {
							logger.error("Mailing error", e);
							System.exit(Constants.ExitCodes.MAILING_ERROR);
						}
						break;
					default:
						System.err.println("Unknowwn argument: " + arg);
						System.exit(Constants.ExitCodes.ARGUMENT_FAIL);
						break;
				}
			}
		}

		ServerSocket serverSocket = null;

		try {
			serverSocket = new ServerSocket(baseData.getPort());
		} catch (IOException e) {
			System.err.println("Port " + baseData.getPort() + " is in use.");
			System.exit(ExitCodes.SERVER_PORT_IN_USE);
		}

		Instances tempInstances = null;

		try {
			tempInstances = new Instances(baseData, learn);
		} catch (IOException | XmlError | XMLStreamException e) {
			logger.error("Exception on reading configuration occured, cause:", e);
			e.printStackTrace();
			System.exit(ExitCodes.READING_CONFIGURATION_FAIL);
		}

		if (learn) {
			try {
				boolean learned = tempInstances.learn();
				if (!learned) {
					logger.log(LEARN, "Nothing to learn!");
				}
			} catch (IOException | XmlError | XMLStreamException | LearningError e) {
				logger.error("Exception on reading configuration or learning files occured, cause:", e);
				e.printStackTrace();
				System.exit(ExitCodes.READING_CONFIGURATION_FAIL);
			}
			System.exit(ExitCodes.OK);
		}

		try {
			tempInstances.init();
		} catch (IOException | XmlError | XMLStreamException e) {
			logger.error("Exception on reading configuration occured, cause:", e);
			e.printStackTrace();
			System.exit(ExitCodes.READING_CONFIGURATION_FAIL);
		} catch (LearningError e2) {
			logger.error(e2.getMessage());
			System.exit(ExitCodes.LEARNING_NECESSARY);
		}

		final Instances instances = tempInstances;
		final CommandHandler commandHandler = new CommandHandler(instances);
		Server server = new Server(serverSocket, commandHandler, instances.getSolvisDescription().getMiscellaneous());
		Runnable runnable = new Runnable() {

			@Override
			public void run() {
				instances.abort();
				commandHandler.abort();
				if (server != null) {
					server.abort();
				}
				AbortHelper.getInstance().abort();
				logManager.shutdown();
			}
		};

		Runtime.getRuntime().addShutdownHook(new Thread(runnable));

		instances.initialized();
		server.start();

		// runnable.run();

	}

	private static void serverRestartAndExit(BaseData baseData, String agentlibOption) {
		try {
			long unsuccessfullTime = System.currentTimeMillis() + Constants.MAX_WAIT_TIME_TERMINATING_OTHER_SERVER;
			ServerSocket serverSocket = null;
			logger.info("Wait for server termination");
			while (System.currentTimeMillis() < unsuccessfullTime && serverSocket == null) {
				try {
					serverSocket = new ServerSocket(baseData.getPort());
					serverSocket.close();
				} catch (IOException e) {
					serverSocket = null;
				}
				try {
					Thread.sleep(100);
				} catch (InterruptedException e1) {
				}

			}
			if (serverSocket != null) {
				logger.info("Server terminated");
				Restart restart = new Restart();
				restart.startMainProcess(agentlibOption);
				System.exit(ExitCodes.OK);
			} else {
				System.err.println("Restart not possible, server still running");
			}
		} catch (Throwable e) {
			logger.info("Unexpected error on restart: " + e.getMessage());
		}
	}

	private static void serverTerminateAndExit(BaseData baseData) {
		int port = baseData.getPort();
		TerminateClient client = new TerminateClient(port);
		try {
			client.connectAndTerminateOtherServer();
		} catch (IOException e) {
			e.printStackTrace();
			System.err.println("Terminate not successfull.");
			LogManager.exit(ExitCodes.SERVER_TERMINATION_FAIL);
		}
		System.exit(ExitCodes.OK);

	}

}
