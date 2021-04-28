/************************************************************************
 * 
 * $Id$
 *
 * 
 ************************************************************************/

package de.sgollmer.solvismax;

import java.io.IOException;
import java.net.ServerSocket;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Date;
import java.util.Iterator;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.xml.stream.XMLStreamException;

import org.eclipse.paho.client.mqttv3.MqttException;

import de.sgollmer.solvismax.Constants.ExitCodes;
import de.sgollmer.solvismax.connection.CommandHandler;
import de.sgollmer.solvismax.connection.Server;
import de.sgollmer.solvismax.connection.TerminateClient;
import de.sgollmer.solvismax.connection.mqtt.Mqtt;
import de.sgollmer.solvismax.crypt.CryptAes;
import de.sgollmer.solvismax.error.AliasException;
import de.sgollmer.solvismax.error.AssignmentException;
import de.sgollmer.solvismax.error.FileException;
import de.sgollmer.solvismax.error.JsonException;
import de.sgollmer.solvismax.error.LearningException;
import de.sgollmer.solvismax.error.MqttConnectionLost;
import de.sgollmer.solvismax.error.ReferenceException;
import de.sgollmer.solvismax.error.TerminationException;
import de.sgollmer.solvismax.error.TypeException;
import de.sgollmer.solvismax.helper.AbortHelper;
import de.sgollmer.solvismax.log.LogManager;
import de.sgollmer.solvismax.log.LogManager.DelayedMessage;
import de.sgollmer.solvismax.log.LogManager.ILogger;
import de.sgollmer.solvismax.log.LogManager.Level;
import de.sgollmer.solvismax.log.LogManager.LogErrors;
import de.sgollmer.solvismax.model.Instances;
import de.sgollmer.solvismax.windows.Task;
import de.sgollmer.solvismax.xml.BaseControlFileReader;
import de.sgollmer.xmllibrary.XmlException;

public class Main {

	public static Main getInstance() {
		Main main = MainHolder.INSTANCE;
		return main;
	}

	private static class MainHolder {

		private static final Main INSTANCE = new Main();
	}

	private static final Pattern cmdPattern = Pattern.compile("--([^=]*)(=(.*)){0,1}");
	private ILogger logger;
	private String startTime = null;
	private boolean shutdownExecuted = false;
	private Instances instances = null;
	private CommandHandler commandHandler = null;
	private Server server = null;
	private Mqtt mqtt = null;

	private enum ExecutionMode {
		STANDARD(true), LEARN(false), CHANNELS_OF_UNIT(false), CHANNELS_OF_ALL_CONFIGURATIONS(false);
		
		private final boolean start; 

		private ExecutionMode( boolean start ) {
			this.start = start;
		}
	}

	private void execute(String[] args) {

		String createTaskName = null;
		String baseXml = null;
		boolean onBoot = false;

		Collection<String> argCollection = new ArrayList<>(Arrays.asList(args));

		for (Iterator<String> it = argCollection.iterator(); it.hasNext();) {
			String arg = it.next();
			Matcher matcher = cmdPattern.matcher(arg);
			if (!matcher.matches()) {
				System.err.println("Unknowwn argument: " + arg);
			} else {

				String command = matcher.group(1);
				String value = null;
				if (matcher.groupCount() >= 3) {
					value = matcher.group(3);
				}

				boolean found = true;

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
					case "create-task-xml":
						if (value == null) {
							System.err.println("To less arguments!");
							System.exit(Constants.ExitCodes.ARGUMENT_FAIL);
						}
						createTaskName = value;
						break;
					case "onBoot":
						onBoot = true;
						break;
					case "base-xml":
						baseXml = value;
						break;
					default:
						found = false;
				}
				if (found) {
					it.remove();
				}
			}
		}

		if (createTaskName != null) {
			try {
				Task.createTask(createTaskName, onBoot);
				System.exit(Constants.ExitCodes.OK);
			} catch (XMLStreamException | IOException e) {
				System.err.println(e.getMessage());
				System.exit(Constants.ExitCodes.TASK_CREATING_ERROR);
			}
		}

		BaseData baseData = null;
		LogManager logManager = LogManager.getInstance();
		try {
			baseData = new BaseControlFileReader(baseXml).read();
			if (baseData == null) {
				throw new XmlException("");
			}
		} catch (IOException | XmlException | XMLStreamException | AssignmentException | ReferenceException e) {
			e.printStackTrace();
			logManager.addDelayedErrorMessage(new DelayedMessage(Level.FATAL, "base.xml couldn't be read.", Main.class,
					ExitCodes.READING_CONFIGURATION_FAIL));
			LogManager.exit(ExitCodes.READING_CONFIGURATION_FAIL);
		}

		String path = baseData.getWritablePath();

		LogErrors error = LogErrors.INIT;

		try {
			error = logManager.createInstance(path);
		} catch (IOException | FileException e) {
			error = LogErrors.INIT;
			e.printStackTrace();
		}

		if (error == LogErrors.INIT) {
			System.err.println("Log4j couldn't initalized");
		} else if (error == LogErrors.PREVIOUS) {
			LogManager.exit(0);
		}

		this.logger = logManager.getLogger(Main.class);

		String serverStart = "Server started, Version " + Version.getInstance().getVersion();
		if (Version.getInstance().getBuildDate() != null) {
			serverStart += ", compiled at " + Version.getInstance().getBuildDate();
		}

		this.logger.info(serverStart);
		Level.getLevel("LEARN");
		
		ExecutionMode executionMode = ExecutionMode.STANDARD;

		for (Iterator<String> it = argCollection.iterator(); it.hasNext();) {
			String arg = it.next();
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
						this.logger.info("Termination started");
						this.serverTerminateAndExit(baseData);
						break;
					case "server-learn":
						executionMode = ExecutionMode.LEARN;
						break;
					case "server-restart":
						this.logger.info("Restart started");
						this.serverRestartAndExit(baseData, value);
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
							this.logger.error("Mailing error", e);
							System.exit(Constants.ExitCodes.MAILING_ERROR);
						}
						break;
					case "channels":
						executionMode = ExecutionMode.CHANNELS_OF_UNIT;
						break;
					case "channels_of_all_configurations":
						executionMode = ExecutionMode.CHANNELS_OF_ALL_CONFIGURATIONS;
						break;
					default:
						System.err.println("Unknowwn argument: " + arg);
						System.exit(Constants.ExitCodes.ARGUMENT_FAIL);
						break;
				}
			}
		}

		ServerSocket serverSocketHelper = this.openSocket(baseData.getPort() + 1);
		ServerSocket serverSocket = this.openSocket(baseData.getPort());

		try {
			this.instances = new Instances(baseData, executionMode == ExecutionMode.LEARN);
		} catch (IOException | XmlException | XMLStreamException | AssignmentException | FileException
				| ReferenceException e) {
			this.logger.error("Exception on reading configuration occured, cause:", e);
			e.printStackTrace();
			System.exit(ExitCodes.READING_CONFIGURATION_FAIL);
		}

		try {
			this.waitForValidTime();
		} catch (TerminationException e1) {
			System.exit(ExitCodes.OK);
		}

		SimpleDateFormat format = new SimpleDateFormat("yyyy.MM.dd HH:mm:ss");
		this.startTime = format.format(new Date());

		if (executionMode == ExecutionMode.LEARN || this.instances.mustLearn()) {

			this.closeSocket(serverSocket);

			try {
				this.instances.learn(executionMode == ExecutionMode.LEARN);
			} catch (IOException | XMLStreamException | LearningException | FileException e) {
				this.logger.error("Exception on reading configuration or learning files occured, cause:", e);
				e.printStackTrace();
				System.exit(ExitCodes.READING_CONFIGURATION_FAIL);
			} catch (TerminationException e) {
				System.exit(ExitCodes.OK);
			}
			if (executionMode == ExecutionMode.LEARN) {
				System.exit(ExitCodes.OK);
			}
			serverSocket = this.openSocket(baseData.getPort());
		}

		this.closeSocket(serverSocketHelper);

		try {
			this.instances.init();
		} catch (IOException | AssignmentException | XMLStreamException | AliasException | TypeException e) {
			this.logger.error("Exception on reading configuration occured, cause:", e);
			e.printStackTrace();
			System.exit(ExitCodes.READING_CONFIGURATION_FAIL);
		} catch (LearningException e2) {
			this.logger.error(e2.getMessage());
			System.exit(ExitCodes.LEARNING_NECESSARY);
		}

		this.commandHandler = new CommandHandler(this.instances);
		this.server = new Server(serverSocket, this.commandHandler,
				this.instances.getSolvisDescription().getMiscellaneous());
		this.mqtt = baseData.getMqtt();
		try {
			this.mqtt.connect(this.instances, this.commandHandler);
		} catch (MqttException e) {
			this.logger.error("Error: Mqtt connection error", e);
			System.exit(Constants.ExitCodes.MQTT_ERROR);
		}
		Runnable runnable = new Runnable() {

			@Override
			public void run() {
				Main.this.shutDownHandling(true);
			}
		};

		Runtime.getRuntime().addShutdownHook(new Thread(runnable));

		this.instances.initialized();
		this.server.start();

		System.out.println(serverStart);

		// runnable.run();

	}

	private void serverRestartAndExit(BaseData baseData, String agentlibOption) {
		try {
			long unsuccessfullTime = System.currentTimeMillis() + Constants.MAX_WAIT_TIME_TERMINATING_OTHER_SERVER;
			ServerSocket serverSocket = null;
			this.logger.info("Wait for server termination");
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
				Restart restart = new Restart();
				restart.startMainProcess(agentlibOption);
			} else {
				this.logger.error("Restart not possible, server still running");
			}
		} catch (Throwable e) {
			this.logger.info("Unexpected error on restart: " + e.getMessage());
		}
		this.logger.info("Restart finished");
		LogManager.exit(ExitCodes.OK);
	}

	private void serverTerminateAndExit(BaseData baseData) {
		int port = baseData.getPort();
		TerminateClient client = new TerminateClient(port);
		try {
			client.connectAndTerminateOtherServer();
		} catch (IOException | JsonException e) {
			e.printStackTrace();
			System.err.println("Terminate not successfull.");
			LogManager.exit(ExitCodes.SERVER_TERMINATION_FAIL);
		}
		this.logger.info("Termination finished.");
		LogManager.exit(ExitCodes.OK);

	}

	public void restart() {
		this.shutDownHandling(false);
		Restart restart = new Restart();
		restart.startRestartProcess();
		this.logger.info("Server terminated (started at " + Main.this.startTime + ")");
		LogManager.exit(ExitCodes.OK);
	}

	void shutDownHandling(boolean out) {
		if (this.shutdownExecuted) {
			return;
		}
		this.shutdownExecuted = true;
		AbortHelper.getInstance().abort();
		if (this.instances != null) {
			this.instances.abort();
		}
		if (this.commandHandler != null) {
			this.commandHandler.abort();
		}
		if (this.mqtt != null) {
			try {
				this.mqtt.deleteRetainedTopics();
			} catch (MqttException | MqttConnectionLost e) {
				this.logger.error("Error on deleting Mqtt retained topics", e);
			}
			this.mqtt.abort();
		}
		if (this.server != null) {
			this.server.abort();
		}
		if (out) {
			this.logger.info("Server terminated (started at " + Main.this.startTime + ")");
		}
	}

	public static void main(String[] args) {
		Main.getInstance().execute(args);
	}

	private void waitForValidTime() throws TerminationException {
		long timeOfLastBackup = this.instances.getBackupHandler().getTimeOfLastBackup();
		int waitTime = 1000;
		boolean waiting = false;
		int currentWaitTime = 0;
		for (; currentWaitTime < Constants.MAX_WAIT_TIME_ON_STARTUP
				&& timeOfLastBackup > System.currentTimeMillis(); currentWaitTime += waitTime) {
			if (!waiting) {
				waiting = true;
				this.logger.info("Waiting for valid system time");
			}
			AbortHelper.getInstance().sleep(waitTime);
		}
		if (waiting) {
			this.logger.info("Valid system time detected after " + currentWaitTime / 1000 + "s.");
		}
	}

	private ServerSocket openSocket(int port) {
		ServerSocket serverSocket = null;
		;
		try {
			serverSocket = new ServerSocket(port);
		} catch (IOException e) {
			System.err.println("Port " + port + " is in use.");
			System.exit(ExitCodes.SERVER_PORT_IN_USE);
		}
		return serverSocket;
	}

	private void closeSocket(ServerSocket socket) {
		try {
			socket.close();
		} catch (IOException e) {
		}
	}
}
