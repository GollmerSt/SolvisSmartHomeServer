/************************************************************************
 * 
 * $Id$
 *
 * 
 ************************************************************************/

package de.sgollmer.solvismax.log;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collection;

import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.core.config.ConfigurationSource;
import org.apache.logging.log4j.core.config.Configurator;

import de.sgollmer.solvismax.Constants;
import de.sgollmer.solvismax.error.FileError;
import de.sgollmer.solvismax.helper.FileHelper;

/**
 *
 * @author gollmer
 */
public class Logger2 {

	public static Logger2 getInstance() {
		return LOGGER;
	}

	public enum LogErrors {
		OK, INIT, PREVIOUS
	};

	public static LogErrors createInstance(String pathName) throws IOException {
		LOGGER = new Logger2(pathName);
		if (!LOGGER.setConfiguration()) {
			return LogErrors.INIT;
		}
		return Logger2.outputDelayedMessages() ? LogErrors.PREVIOUS : LogErrors.OK;
	}

	public static class DelayedMessage {
		private final Level level;
		private final String message;
		private final Class<?> loggedClass;
		private final Integer errorCode;

		public DelayedMessage(Level level, String message, Class<?> loggedClass, Integer errorCode) {
			this.level = level;
			this.message = message;
			this.loggedClass = loggedClass;
			this.errorCode = errorCode;
		}
	}

	private static Collection<DelayedMessage> delayedErrorMessages = new ArrayList<>();
	private static int delayedErrorCode = -1;

	private static Logger2 LOGGER = null;

	private final File parent;

	public Logger2(String pathName) {
		File parent;

		if (pathName == null) {
			pathName = System.getProperty("user.home");
			if (System.getProperty("os.name").startsWith("Windows")) {
				pathName = System.getenv("APPDATA");
			}

		}

		pathName += File.separator + Constants.RESOURCE_DESTINATION_PATH;
		parent = new File(pathName);
		this.parent = parent;
	}

	private void copyFiles() throws IOException {

		boolean success = true;

		if (!this.parent.exists()) {
			success = this.parent.mkdir();
		}

		if (!success) {
			throw new FileError("Error on creating directory <" + this.parent.getAbsolutePath() + ">");
		}

		File xml = new File(this.parent, Constants.LOG4J_CONFIG_FILE);

		if (!xml.exists()) {
			FileHelper.copyFromResource(Constants.RESOURCE_PATH + File.separator + Constants.LOG4J_CONFIG_FILE, xml,
					"****LogPath****", this.parent.getAbsolutePath());
		}

	}

	public boolean setConfiguration() throws IOException {

		copyFiles();

		File xml = new File(this.parent, Constants.LOG4J_CONFIG_FILE);

		InputStream input = null;

		if (xml.exists()) {
			try {
				input = new FileInputStream(xml);
			} catch (FileNotFoundException ex) {
				System.err.println("Error on reading log4j.xml");
				return false;
			}
		}
		ConfigurationSource source = new ConfigurationSource(input);
		Configurator.initialize(null, source);
		return true;
	}

	public static void addDelayedErrorMessage(DelayedMessage message) {
		if (LOGGER != null) {
			Logger logger = LogManager.getLogger(message.loggedClass);
			logger.log(message.level, message.message);

		} else {
			Logger2.delayedErrorMessages.add(message);
		}
	}

	private static boolean outputDelayedMessages() {
		Level level = Level.INFO;
		for (DelayedMessage message : Logger2.delayedErrorMessages) {
			if (message.errorCode != null && !message.level.equals(level) && message.level.isLessSpecificThan(level)) {
				Logger2.delayedErrorCode = message.errorCode;
				level = message.level;
			}

			if (LOGGER != null) {
				Logger logger = LogManager.getLogger(message.loggedClass);
				logger.log(message.level, message.message);
			} else {
				System.err.println(message.level.toString() + ": " + message.message);
			}
		}
		Logger2.delayedErrorMessages.clear();
		return Logger2.delayedErrorCode != -1;
	}

	public static int getExitCode() {
		if (Logger2.delayedErrorCode == 0) {
			Level level = Level.INFO;
			Logger2.delayedErrorCode = -1;
			for (DelayedMessage message : Logger2.delayedErrorMessages) {

				if (message.errorCode != null && !message.level.equals(level)
						&& message.level.isLessSpecificThan(level)) {
					Logger2.delayedErrorCode = message.errorCode;
					level = message.level;
				}
			}
		}
		return Logger2.delayedErrorCode;
	}

	public static void out(Logger logger, Level level, String message, StackTraceElement[] elements) {
		StringBuilder builder = new StringBuilder(message);
		for (StackTraceElement element : elements) {
			builder.append('\n');
			builder.append(element.toString());
		}
		logger.log(level, builder.toString());
	}

	public static void exit(int errorCode) {
		if (Logger2.LOGGER == null && !Logger2.delayedErrorMessages.isEmpty()) {
			boolean error = Logger2.outputDelayedMessages() ;
			if ( error ) {
				System.exit(Logger2.getExitCode() );
			}
		}
		System.exit(errorCode);
	}

}
