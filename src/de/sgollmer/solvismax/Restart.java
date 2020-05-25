/************************************************************************
 * 
 * $Id$
 *
 * 
 ************************************************************************/

package de.sgollmer.solvismax;

import java.io.File;
import java.lang.management.ManagementFactory;
import java.util.ArrayList;
import java.util.Collection;

import de.sgollmer.solvismax.Constants.ExitCodes;
import de.sgollmer.solvismax.connection.CommandHandler;
import de.sgollmer.solvismax.log.LogManager;
import de.sgollmer.solvismax.log.LogManager.Logger;

public class Restart {

	private static final Logger logger = LogManager.getInstance().getLogger(CommandHandler.class);

	/**
	 * Sun property pointing the main class and its arguments. Might not be defined
	 * on non Hotspot VM implementations.
	 * 
	 * This class is based on, but improved (original one doesn't work on a linux systems).
	 * http://lewisleo.blogspot.com/2012/08/programmatically-restart-java.html
	 */
	public static final String SUN_JAVA_COMMAND = "sun.java.command";

	private String java;
	private Collection<String> vmArgs = new ArrayList<>();
	private String vmAgentLib = null;
	private Collection<String> mainCommand = new ArrayList<>();

	public Restart() {
		// arguments of the virtual machine
		Collection<String> vmArguments = ManagementFactory.getRuntimeMXBean().getInputArguments();

		for (String arg : vmArguments) {
			int agentIdx = arg.indexOf("-agentlib:");
			if (agentIdx == 0) {
				this.vmAgentLib = arg.substring(10);
			} else {
				this.vmArgs.add(arg);
			}
		}
		// java binary
		this.java = System.getProperty("java.home") + File.separator + "bin" + File.separator + "java";

		// init the command to execute, add the vm args
		// program main and program arguments

		String javaCommand = System.getProperty(SUN_JAVA_COMMAND);

		int jarIdx = javaCommand.indexOf(".jar");
		if (jarIdx >= 0) {
			String main = javaCommand.substring(0, jarIdx + 4);
			this.mainCommand.add("-jar");
			this.mainCommand.add(new File(main).getPath());
		} else {
			this.mainCommand.add("-cp");
			this.mainCommand.add(System.getProperty("java.class.path"));
			this.mainCommand.add(javaCommand.split(" ")[0]);
		}

	}

	public String[] createRestartProcessArray() {
		Collection<String> cmd = new ArrayList<>();
		cmd.add(this.java);
		cmd.addAll(this.vmArgs);
		cmd.addAll(this.mainCommand);
		if (this.vmAgentLib != null) {
			cmd.add("--server-restart=" + this.vmAgentLib);
		} else {
			cmd.add("--server-restart");
		}

		return cmd.toArray(new String[1]);
	}

	public String[] createMainProcessArray(String agentlibOption) {
		Collection<String> cmd = new ArrayList<>();
		cmd.add(this.java);
		cmd.addAll(this.vmArgs);
		if (agentlibOption != null) {
			cmd.add("-agentlib:" + agentlibOption);
		}
		cmd.addAll(this.mainCommand);
		return cmd.toArray(new String[1]);
	}

	private void startProcess(String[] cmd) {
		String cmdString = "";
		for (String part : cmd) {
			cmdString += part + " ";
		}
		logger.info("Restart command: " + cmdString);
		try {
			Runtime.getRuntime().exec(cmd);
		} catch (Throwable e) {
			logger.error("Restart not successfull");
			logger.error(e.getMessage());
			e.printStackTrace();
			System.exit(ExitCodes.RESTART_FAILURE);
		}
	}

	public void startRestartProcess() {
		this.startProcess(this.createRestartProcessArray());
	}

	public void startMainProcess(String agentlibOption) {
		this.startProcess(this.createMainProcessArray(agentlibOption));
	}

}
