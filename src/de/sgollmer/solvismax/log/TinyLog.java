package de.sgollmer.solvismax.log;

import java.io.File;
import java.io.IOException;
import java.util.Properties;

import org.tinylog.Logger;
import org.tinylog.TaggedLogger;
import org.tinylog.configuration.Configuration;
import org.tinylog.provider.ProviderRegistry;

import de.sgollmer.solvismax.Constants;
import de.sgollmer.solvismax.error.FileError;
import de.sgollmer.solvismax.helper.FileHelper;

public class TinyLog {

	private static TaggedLogger learningLogger;

	public static class LoggerTiny implements de.sgollmer.solvismax.log.LogManager.Logger {

		private final String className;

		public LoggerTiny() {
			this.className = null;
		}

		private String createMessage(String message) {
			return "{} - " + message;
		}

		public LoggerTiny(Class<?> clazz) {
			this.className = clazz.getName();
		}

		@Override
		public de.sgollmer.solvismax.log.LogManager.Logger create(Class<?> clazz) {
			return new LoggerTiny(clazz);
		}

		@Override
		public boolean createInstance(String path) throws IOException {
			return new TinyLog(path).setConfiguration();
		}

		@Override
		public void fatal(String message) {
			this.error(message, null);

		}

		@Override
		public void fatal(String message, Throwable throwable) {
			Logger.error(throwable, this.createMessage(message), this.className);
		}

		@Override
		public void error(String message) {
			this.error(message, null);

		}

		@Override
		public void error(String message, Throwable throwable) {
			Logger.error(throwable, this.createMessage(message), this.className);

		}

		@Override
		public void learn(String message) {
			this.learn(message, null);

		}

		@Override
		public void learn(String message, Throwable throwable) {
			learningLogger.info(throwable, message, this.className);
		}

		@Override
		public void warn(String message) {
			this.warn(message, null);

		}

		@Override
		public void warn(String message, Throwable throwable) {
			Logger.warn(throwable, this.createMessage(message), this.className);

		}

		@Override
		public void info(String message) {
			this.info(message, null);

		}

		@Override
		public void info(String message, Throwable throwable) {
			Logger.info(throwable, this.createMessage(message), this.className);

		}

		@Override
		public void debug(String message) {
			this.debug(message, null);

		}

		@Override
		public void debug(String message, Throwable throwable) {
			Logger.debug(throwable, this.createMessage(message), this.className);

		}

		@Override
		public void log(de.sgollmer.solvismax.log.LogManager.Level level, String message) {
			this.log(level, message, null);

		}

		@Override
		public void log(de.sgollmer.solvismax.log.LogManager.Level level, String message, Throwable throwable) {
			switch (level) {
				case DEBUG:
					this.debug(message, throwable);
					break;
				case ERROR:
					this.error(message, throwable);
					break;
				case FATAL:
					this.fatal(message, throwable);
					break;
				case INFO:
					this.info(message, throwable);
					break;
				case LEARN:
					this.learn(message, throwable);
					break;
				case WARN:
					this.warn(message, throwable);
					break;
				default:
					this.error("unknown level " + level.name());
			}

		}

		@Override
		public void shutdown() throws InterruptedException {
			ProviderRegistry.getLoggingProvider().shutdown();
			
		}

	}

	private final File parent;

	public TinyLog(String pathName) {
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

		File xml = new File(this.parent, Constants.TINY_LOG_CONFIG_FILE);

		if (!xml.exists()) {
			FileHelper.copyFromResource(Constants.RESOURCE_PATH + File.separator + Constants.TINY_LOG_CONFIG_FILE, xml);
		}

	}

	private boolean setConfiguration() throws IOException {

		copyFiles();

		File properties = new File(this.parent, Constants.TINY_LOG_CONFIG_FILE);

		Properties props = System.getProperties();
		props.setProperty("tinylog.configuration", properties.getAbsolutePath());

		for (int i = 1; i < 10; ++i) {
			String key = Constants.TINY_LOG_FILE_PROPERTY_PREFIX + i;
			String type = Configuration.get(key);
			if (type == null) {
				break;
			}
			key = Constants.TINY_LOG_FILE_PROPERTY_PREFIX + i + Constants.TINY_LOG_FILE_PROPERTY_SUFFIX;
			String file = Configuration.get(key);
			if (file != null) {
				Configuration.set(key, this.parent.getAbsolutePath() + File.separator + file);
			}
		}

		learningLogger = Logger.tag("LEARN");
		return true;
	}

}