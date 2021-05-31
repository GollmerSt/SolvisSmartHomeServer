/************************************************************************
 * 
 * $Id$
 *
 * 
 ************************************************************************/

package de.sgollmer.solvismax.log;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

import org.tinylog.Logger;
import org.tinylog.TaggedLogger;
import org.tinylog.configuration.Configuration;
import org.tinylog.provider.ProviderRegistry;

import de.sgollmer.solvismax.Constants;
import de.sgollmer.solvismax.error.FileException;
import de.sgollmer.solvismax.helper.FileHelper;
import de.sgollmer.solvismax.log.LogManager.ILoggerBase;
import de.sgollmer.solvismax.log.LogManager.ILoggerExt;

public class TinyLog {

	private static TaggedLogger learningLogger;

	static class LoggerTiny implements ILoggerExt, ILoggerBase {

		private final String className;

		LoggerTiny() {
			this.className = null;
		}

		private String createMessage(final String message) {
			return "{} - " + message;
		}

		private LoggerTiny(String className) {
			this.className = className;
		}

		@Override
		public de.sgollmer.solvismax.log.LogManager.ILoggerExt create(String className) {
			return new LoggerTiny(className);
		}

		@Override
		public boolean initInstance(final String path) throws IOException, FileException {
			return new TinyLog(path).setConfiguration();
		}

		@Override
		public void log(final de.sgollmer.solvismax.log.LogManager.Level level, final String message,
				final Throwable throwable) {
			switch (level) {
				case DEBUG:
					Logger.debug(throwable, this.createMessage(message), this.className);
					break;
				case ERROR:
				case FATAL:
					Logger.error(throwable, this.createMessage(message), this.className);
					break;
				case INFO:
					Logger.info(throwable, this.createMessage(message), this.className);
					break;
				case LEARN:
					learningLogger.info(throwable, message, this.className);
					break;
				case WARN:
					Logger.warn(throwable, this.createMessage(message), this.className);
					break;
				default:
					Logger.error((Throwable) null, this.createMessage("unknown level " + level.name()), this.className);
			}

		}

		@Override
		public void shutdown() throws InterruptedException {
			ProviderRegistry.getLoggingProvider().shutdown();

		}

	}

	private final File parent;

	private TinyLog(final String pathNameP) {

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
			;
		}

		if (!success) {
			throw new FileException("Error on creating directory <" + this.parent.getAbsolutePath() + ">");
		}

		File xml = new File(this.parent, Constants.TINY_LOG_CONFIG_FILE);

		if (!xml.exists()) {
			FileHelper.copyFromResourceText(Constants.Files.RESOURCE + '/' + Constants.TINY_LOG_CONFIG_FILE, xml);
		}

	}

	private boolean setConfiguration() throws IOException, FileException {

		copyFiles();

		File properties = new File(this.parent, Constants.TINY_LOG_CONFIG_FILE);

		Properties props = System.getProperties();
		props.setProperty("tinylog.configuration", properties.getAbsolutePath());

		Properties prop = new Properties();
		InputStream stream = new FileInputStream(properties);
		prop.load(stream);

		for (int i = 1; i < 10; ++i) {
			String key = Constants.TINY_LOG_FILE_PROPERTY_PREFIX + i;
			String type = prop.getProperty(key);
			if (type == null) {
				break;
			}
			key = Constants.TINY_LOG_FILE_PROPERTY_PREFIX + i + Constants.TINY_LOG_FILE_PROPERTY_SUFFIX;
			String file = prop.getProperty(key);
			if (file != null) {
				Configuration.set(key, this.parent.getAbsolutePath() + File.separator + file);
			}
		}

		learningLogger = Logger.tag("LEARN");
		return true;
	}

}
