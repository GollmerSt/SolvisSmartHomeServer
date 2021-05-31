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

import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.core.config.ConfigurationSource;
import org.apache.logging.log4j.core.config.Configurator;
import org.apache.logging.log4j.LogManager;

import de.sgollmer.solvismax.Constants;
import de.sgollmer.solvismax.error.FileException;
import de.sgollmer.solvismax.helper.FileHelper;
import de.sgollmer.solvismax.log.LogManager.ILoggerBase;
import de.sgollmer.solvismax.log.LogManager.ILoggerExt;

/**
 *
 * @author gollmer
 */
public class Logger4j2 {

	static class Logger implements ILoggerExt, ILoggerBase {

		private final org.apache.logging.log4j.Logger logger;
		private final Level LEARN;

		Logger() {
			this.logger = null;
			this.LEARN = null;
		}

		private Logger(final String className) {
			this.logger = org.apache.logging.log4j.LogManager.getLogger(className);
			this.LEARN = Level.getLevel("LEARN");
		}

		@Override
		public de.sgollmer.solvismax.log.LogManager.ILoggerExt create(final String className) {
			Logger logger = new Logger(className);
			return logger;
		}

		private Level getLevel(final de.sgollmer.solvismax.log.LogManager.Level level) {
			switch (level) {
				case DEBUG:
					return Level.DEBUG;
				case ERROR:
					return Level.ERROR;
				case FATAL:
					return Level.FATAL;
				case INFO:
					return Level.INFO;
				case LEARN:
					return this.LEARN;
				case WARN:
					return Level.WARN;
				default:
					this.logger.error("Log level " + level.name() + " not implemented");
					return Level.DEBUG;
			}
		}

		@Override
		public boolean initInstance(final String pathName) throws IOException, FileException {
			return new Logger4j2(pathName).setConfiguration();
		}

		@Override
		public void log(final de.sgollmer.solvismax.log.LogManager.Level level, final String message,
				final Throwable throwable) {
			this.logger.log(this.getLevel(level), message, throwable);

		}

		@Override
		public void shutdown() throws InterruptedException {
			LogManager.shutdown();

		}
	}

	private final File parent;

	private Logger4j2(final String pathNameP) {
		File parent;

		String pathName;
		if (pathNameP == null) {
			pathName = System.getProperty("user.home");
			if (System.getProperty("os.name").startsWith("Windows")) {
				pathName = System.getenv("APPDATA");
			}

		} else {
			pathName = pathNameP;
		}

		pathName += File.separator + Constants.Files.RESOURCE_DESTINATION;
		parent = new File(pathName);
		this.parent = parent;
	}

	private void copyFiles() throws IOException, FileException {

		boolean success = true;

		if (!this.parent.exists()) {
			success = FileHelper.mkdir(this.parent);
		}

		if (!success) {
			throw new FileException("Error on creating directory <" + this.parent.getAbsolutePath() + ">");
		}

		File xml = new File(this.parent, Constants.LOG4J_CONFIG_FILE);

		if (!xml.exists()) {
			FileHelper.copyFromResource(Constants.Files.RESOURCE + '/' + Constants.LOG4J_CONFIG_FILE, xml,
					"****LogPath****", this.parent.getAbsolutePath());
		}

	}

	private boolean setConfiguration() throws IOException, FileException {

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

}
