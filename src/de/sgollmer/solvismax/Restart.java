/************************************************************************
 * 
 * $Id$
 *
 * 
 ************************************************************************/

package de.sgollmer.solvismax;

import java.io.File;
import java.io.IOException;
import java.lang.management.ManagementFactory;
import java.util.List;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import de.sgollmer.solvismax.connection.CommandHandler;

public class Restart {
	
	private static final Logger logger = LogManager.getLogger(CommandHandler.class);

	/**
	 * Sun property pointing the main class and its arguments. Might not be defined
	 * on non Hotspot VM implementations.
	 * 
	 * This class is based on:
	 * From: http://lewisleo.blogspot.com/2012/08/programmatically-restart-java.html
	 */
	public static final String SUN_JAVA_COMMAND = "sun.java.command";

	private String java;
	@SuppressWarnings("unused")
	private String vmArguments;
	private String vmAgumentsWOAgentlib;
	private String mainCommand;
	private String mainArguments;
	private boolean agent = false ; 

	public Restart() {
		// java binary
		this.java = System.getProperty("java.home") + File.separator + "bin" + File.separator + "java";

		// arguments of the virtual machine
		List<String> vmArguments = ManagementFactory.getRuntimeMXBean().getInputArguments();
		StringBuffer vmArgsOneLine = new StringBuffer();
		StringBuffer vmArgsOneLineWO = new StringBuffer();
		for (String arg : vmArguments) {
			// if it's the agent argument : we ignore it otherwise the
			// address of the old application and the new one will be in conflict
			vmArgsOneLine.append(arg);
			vmArgsOneLine.append(" ");
			if (!arg.contains("-agentlib")) {
				this.agent = true ;
				vmArgsOneLineWO.append(arg);
				vmArgsOneLineWO.append(" ");
			}
		}
		this.vmArguments = vmArgsOneLine.toString();
		this.vmAgumentsWOAgentlib = vmArgsOneLineWO.toString();

		// init the command to execute, add the vm args
		// program main and program arguments
		String[] mainCommand = System.getProperty(SUN_JAVA_COMMAND).split(" ");
		// program main is a jar
		if (mainCommand[0].endsWith(".jar")) {
			// if it's a jar, add -jar mainJar
			this.mainCommand = "-jar " + new File(mainCommand[0]).getPath();
		} else {
			// else it's a .class, add the classpath and mainClass
			this.mainCommand = "-cp \"" + System.getProperty("java.class.path") + "\" " + mainCommand[0];
		}

		// program arguments
		StringBuilder arguments = new StringBuilder();
		for (int i = 1; i < mainCommand.length; i++) {
			arguments.append(" ");
			arguments.append(mainCommand[i]);
		}

		this.mainArguments = arguments.toString();

	}

	public String createRestartProcessString() {
		String cmd = "\"" + this.java + "\" " + this.vmAgumentsWOAgentlib + " " + this.mainCommand
				+ " --server-restart=\"'" + this.java + "' " + this.vmAgumentsWOAgentlib + " "
				+ this.mainCommand.replace('"', '\'') + " " + this.mainArguments + "\"";
		return cmd;
	}

	public void startRestartProcess() {
		String cmd = this.createRestartProcessString();
		try {
			Runtime.getRuntime().exec(cmd.toString());
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	@SuppressWarnings("unused")
	public void startMainProcess(String cmd) {
		cmd = cmd.replace('\'', '"');
		logger.info("CmdString: " + cmd) ;
		if (agent && false ) {
			logger.error("Starting of new VM not possible in case of agentlib");
			System.exit(1);
		}
		try {
			Runtime.getRuntime().exec(cmd.toString());
		} catch (IOException e) {
			logger.error("Restart not successfull");
			logger.error(e.getMessage());
			e.printStackTrace();
			System.exit(1) ;
		}
	}
}
