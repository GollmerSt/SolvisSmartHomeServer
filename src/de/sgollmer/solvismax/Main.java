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
import de.sgollmer.solvismax.error.PackageException;
import de.sgollmer.solvismax.error.TerminationException;
import de.sgollmer.solvismax.error.TypeException;
import de.sgollmer.solvismax.helper.AbortHelper;
import de.sgollmer.solvismax.helper.Helper;
import de.sgollmer.solvismax.log.LogManager;
import de.sgollmer.solvismax.log.LogManager.ILogger;
import de.sgollmer.solvismax.log.LogManager.Level;
import de.sgollmer.solvismax.log.LogManager.LogErrors;
import de.sgollmer.solvismax.model.Instances;
import de.sgollmer.solvismax.smarthome.IoBroker;
import de.sgollmer.solvismax.windows.Task;
import de.sgollmer.solvismax.xml.BaseControlFileReader;
import de.sgollmer.xmllibrary.XmlException;

public class Main {

	private static final ILogger logger = LogManager.getInstance().getLogger(Main.class);

	public static Main getInstance() {
		Main main = MainHolder.INSTANCE;
		return main;
	}

	private static class MainHolder {

		private static final Main INSTANCE = new Main();
	}

	private static final Pattern cmdPattern = Pattern.compile("--([^=]*)(=(.*)){0,1}");
	private String startTime = null;
	private boolean shutdownExecuted = false;
	private Instances instances = null;
	private CommandHandler commandHandler = null;
	private Server server = null;
	private Mqtt mqtt = null;

	private enum ExecutionMode {
		STANDARD(true, true, false), //
		LEARN(false, true, false), //
		DOCUMENTATION_OF_UNIT(false, false, true), //
		CHANNELS_OF_ALL_CONFIGURATIONS(false, false, false), //
		IOBROKER(false, false, true);

		private final boolean start;
		private final boolean ipChannelLock;
		private final boolean mustLearned;

		private ExecutionMode(final boolean start, final boolean ipChannelLock, final boolean mustLearned) {
			this.start = start;
			this.ipChannelLock = ipChannelLock;
			this.mustLearned = mustLearned;
		}

		public boolean isStart() {
			return this.start;
		}

		boolean mustLearned() {
			return this.mustLearned;
		}
	}

	private void execute(final String[] args) {

		String createTaskName = null;
		String baseXml = null;
		boolean onBoot = false;

		Collection<String> argCollection = splitWindowsArgs(args);

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
					case "string-to-crypt": // Einen String verschlüsseln
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
					case "create-task-xml": // Eine Steuerfile für den Windows-Task-Manager erstellen
						if (value == null) {
							System.err.println("To less arguments!");
							System.exit(Constants.ExitCodes.ARGUMENT_FAIL);
						}
						createTaskName = value;
						break;
					case "onBoot": // TaskManager: Start on boot
						onBoot = true;
						break;
					case "base-xml": // Base-xml-Path
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
		} catch (IOException | XmlException | XMLStreamException e) {
			e.printStackTrace();
			logger.log(Level.FATAL, "base.xml couldn't be read.", null, ExitCodes.READING_CONFIGURATION_FAIL);
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

		Level.getLevel("LEARN");

		ExecutionMode executionMode = ExecutionMode.STANDARD;
		boolean semicolon = false;

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
						logger.info("Termination started");
						this.serverTerminateAndExit(baseData);
						break;
					case "server-learn":
						executionMode = ExecutionMode.LEARN;
						break;
					case "server-restart":
						logger.info("Restart started");
						this.serverRestartAndExit(baseData, value);
						break;
					case "test-mail":
						if (baseData.getExceptionMail() == null) {
							throw new Error(
									"Sending mail not possible in case of mssing or invalid data in <base.xml>");
						}
						System.exit(baseData.getExceptionMail().sendTestMail(baseData));
						break;
					case "documentation":
						executionMode = ExecutionMode.DOCUMENTATION_OF_UNIT;
						break;
					case "channels_of_all_configurations":
						executionMode = ExecutionMode.CHANNELS_OF_ALL_CONFIGURATIONS;
						break;
					case "csvSemicolon":
						semicolon = true;
						break;
					case "iobroker":
						executionMode = ExecutionMode.IOBROKER;
						break;
					default:
						System.err.println("Unknowwn argument: " + arg);
						System.exit(Constants.ExitCodes.ARGUMENT_FAIL);
						break;
				}
			}
		}

		ServerSocket serverSocketHelper = null;
		ServerSocket serverSocket = null;

		if (executionMode.ipChannelLock) {

			serverSocketHelper = this.openSocket(baseData.getPort() + 1);
			serverSocket = this.openSocket(baseData.getPort());
		}

		Runnable runnable = new Runnable() {

			@Override
			public void run() {
				Main.this.shutDownHandling(true);
			}
		};

		Runtime.getRuntime().addShutdownHook(new Thread(runnable));

		try {
			this.instances = new Instances(baseData, executionMode == ExecutionMode.LEARN);
		} catch (IOException | XmlException | XMLStreamException | FileException e) {
			logger.error("Exception on reading configuration occured, cause:", e);
			e.printStackTrace();
			System.exit(ExitCodes.READING_CONFIGURATION_FAIL);
		}

		if (executionMode.mustLearned() && this.instances.mustLearn()) {
			System.err.println("Error: Learning is necessarry!");
			System.exit(ExitCodes.LEARNING_NECESSARY);
		}

		try {
			switch (executionMode) {
				case CHANNELS_OF_ALL_CONFIGURATIONS:
					this.instances.createCsvOut(semicolon);
					System.exit(ExitCodes.OK);
					break;

				case DOCUMENTATION_OF_UNIT:
					this.instances.createCurrentDocumentation(semicolon);
					System.exit(ExitCodes.OK);
					break;
				case IOBROKER:
					IoBroker ioBroker = this.instances.getIobroker();
					this.instances.init();
					ioBroker.writeObjectList(this.instances);
					ioBroker.writePairingScript(this.instances);
					System.exit(ExitCodes.OK);
					break;
			}
		} catch (IOException | XMLStreamException | LearningException | AliasException | TypeException
				| XmlException e) {
			System.out.flush();
			try {
				Thread.sleep(100);
			} catch (InterruptedException e1) {
			}
			logger.error("Exception on reading configuration or learning files occured, cause:", e);
			e.printStackTrace();
			System.exit(ExitCodes.READING_CONFIGURATION_FAIL);
		}

		try {
			this.waitForValidTime();
		} catch (TerminationException e1) {
			System.exit(ExitCodes.OK);
		}

		String serverStart = "Server started, Version " + Version.getInstance().getVersion();
		if (Version.getInstance().getBuildDate() != null) {
			serverStart += ", compiled at " + Version.getInstance().getBuildDate();
		}

		logger.info(serverStart);

		SimpleDateFormat format = new SimpleDateFormat("yyyy.MM.dd HH:mm:ss");
		this.startTime = format.format(new Date());

		if (executionMode == ExecutionMode.LEARN || this.instances.mustLearn()) {

			this.closeSocket(serverSocket);

			try {
				this.instances.learn(executionMode == ExecutionMode.LEARN);
			} catch (IOException | XMLStreamException | LearningException | FileException e) {
				logger.error("Exception on reading configuration or learning files occured, cause:", e);
				e.printStackTrace();
				System.exit(ExitCodes.READING_CONFIGURATION_FAIL);
			} catch (TerminationException e) {
				System.exit(ExitCodes.OK);
			}
			serverSocket = this.openSocket(baseData.getPort());
		}

		this.closeSocket(serverSocketHelper);

		try {
			this.instances.init();

			if (executionMode.isStart()) {
				this.instances.start();
			} else {
				System.exit(ExitCodes.OK);
			}
		} catch (IOException | AssignmentException | XMLStreamException | AliasException | TypeException e) {
			logger.error("Exception on reading configuration occured, cause:", e);
			e.printStackTrace();
			System.exit(ExitCodes.READING_CONFIGURATION_FAIL);
		} catch (LearningException e2) {
			logger.error(e2.getMessage());
			System.exit(ExitCodes.LEARNING_NECESSARY);
		}

		Constants.Debug.logDebugging(logger);

		this.commandHandler = new CommandHandler(this.instances);
		this.server = new Server(serverSocket, this.commandHandler,
				this.instances.getSolvisDescription().getMiscellaneous());
		this.mqtt = baseData.getMqtt();
		try {
			this.mqtt.connect(this.instances, this.commandHandler);
		} catch (MqttException e) {
			logger.error("Error: Mqtt connection error", e);
			System.exit(Constants.ExitCodes.MQTT_ERROR);
		}

		this.instances.initialized();
		this.server.start();

		System.out.println(serverStart);

		// runnable.run();

	}

	private Collection<String> splitWindowsArgs(final String[] args) {
		Collection<String> argCollection = new ArrayList<>();
		for (String argO : args) {
			String[] comp = argO.split("--");

			for (String argI : comp) {
				argI = argI.trim();
				if (!argI.isEmpty()) {
					argCollection.add("--" + argI);
				}
			}
		}
		return argCollection;
	}

	private void serverRestartAndExit(final BaseData baseData, final String agentlibOption) {
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
				Restart restart = new Restart();
				restart.startMainProcess(agentlibOption);
			} else {
				logger.error("Restart not possible, server still running");
			}
		} catch (Throwable e) {
			logger.info("Unexpected error on restart: " + e.getMessage());
		}
		logger.info("Restart finished");
		LogManager.exit(ExitCodes.OK);
	}

	private void serverTerminateAndExit(final BaseData baseData) {
		int port = baseData.getPort();
		TerminateClient client = new TerminateClient(port);
		try {
			client.connectAndTerminateOtherServer();
		} catch (IOException | JsonException | PackageException e) {
			e.printStackTrace();
			System.err.println("Terminate not successfull.");
			LogManager.exit(ExitCodes.SERVER_TERMINATION_FAIL);
		}
		logger.info("Termination finished.");
		LogManager.exit(ExitCodes.OK);

	}

	public void restart() {
		this.shutDownHandling(false);
		Restart restart = new Restart();
		restart.startRestartProcess();
		logger.info("Server terminated (started at " + Main.this.startTime + ")");
		LogManager.exit(ExitCodes.OK);
	}

	void shutDownHandling(final boolean out) {
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
		if (this.mqtt != null && this.mqtt.isEnable()) {
			try {
				this.mqtt.deleteRetainedTopics();
			} catch (MqttException | MqttConnectionLost e) {
				logger.error("Error on deleting Mqtt retained topics", e);
			}
			this.mqtt.abort();
		}
		if (this.server != null) {
			this.server.abort();
		}

		Helper.Runnable.shutdown();

		if (out) {
			logger.info("Server terminated (started at " + Main.this.startTime + ")");
		}
	}

	public static void main(final String[] args) {
//		try {
//			Thread.sleep(20000);
//		} catch (InterruptedException e) {
//		}
		Main main = Main.getInstance();
		try {

			main.execute(args);
		} catch (Throwable e) {
			main.shutDownHandling(true);
		}
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
				logger.info("Waiting for valid system time");
			}
			AbortHelper.getInstance().sleep(waitTime);
		}
		if (waiting) {
			logger.info("Valid system time detected after " + currentWaitTime / 1000 + "s.");
		}
	}

	private ServerSocket openSocket(final int port) {
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

	private void closeSocket(final ServerSocket socket) {
		try {
			socket.close();
		} catch (IOException e) {
		}
	}
}
