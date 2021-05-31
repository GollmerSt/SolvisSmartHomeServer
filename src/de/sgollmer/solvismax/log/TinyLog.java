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

import org.tinylog.Level;
import org.tinylog.configuration.Configuration;
import org.tinylog.format.AdvancedMessageFormatter;
import org.tinylog.format.MessageFormatter;
import org.tinylog.provider.LoggingProvider;
import org.tinylog.provider.ProviderRegistry;

import de.sgollmer.solvismax.Constants;
import de.sgollmer.solvismax.error.FileException;
import de.sgollmer.solvismax.helper.FileHelper;
import de.sgollmer.solvismax.log.LogManager.ILoggerBase;
import de.sgollmer.solvismax.log.LogManager.ILoggerExt;

public class TinyLog implements ILoggerBase {

	private LoggingProvider provider;
	private MessageFormatter formatter;

	private boolean minimumLevelCoversDebug;
	private boolean minimumLevelCoversInfo;
	private boolean minimumLevelCoversWarn;
	private boolean minimumLevelCoversLearn;
	private boolean minimumLevelCoversError;
	private boolean minimumLevelCoversFatal;

	private boolean isCoveredByMinimumLevel(final Level level) {
		return this.provider.getMinimumLevel(null).ordinal() <= level.ordinal();
	}

	private void init() {
		this.provider = ProviderRegistry.getLoggingProvider();
		this.formatter = new AdvancedMessageFormatter(Configuration.getLocale(), Configuration.isEscapingEnabled());

		this.minimumLevelCoversDebug = isCoveredByMinimumLevel(Level.DEBUG);
		this.minimumLevelCoversInfo = isCoveredByMinimumLevel(Level.INFO);
		this.minimumLevelCoversWarn = isCoveredByMinimumLevel(Level.WARN);
		this.minimumLevelCoversLearn = isCoveredByMinimumLevel(Level.ERROR);
		this.minimumLevelCoversError = isCoveredByMinimumLevel(Level.ERROR);
		this.minimumLevelCoversFatal = isCoveredByMinimumLevel(Level.ERROR);

	}

	private class LoggerTiny implements ILoggerExt {

		private final String className;
		private StringBuilder builder = new StringBuilder();

		private LoggerTiny(final String className) {
			this.className = className;
		}

		private Level getLevel(final de.sgollmer.solvismax.log.LogManager.Level level) {
			switch (level) {
				case FATAL:
					if (TinyLog.this.minimumLevelCoversFatal) {
						return Level.ERROR;
					} else {
						return null;
					}
				case ERROR:
					if (TinyLog.this.minimumLevelCoversError) {
						return Level.ERROR;
					} else {
						return null;
					}
				case LEARN:
					if (TinyLog.this.minimumLevelCoversLearn) {
						return Level.INFO;
					} else {
						return null;
					}
				case INFO:
					if (TinyLog.this.minimumLevelCoversInfo) {
						return Level.INFO;
					} else {
						return null;
					}
				case WARN:
					if (TinyLog.this.minimumLevelCoversWarn) {
						return Level.WARN;
					} else {
						return null;
					}
				case DEBUG:
					if (TinyLog.this.minimumLevelCoversDebug) {
						return Level.DEBUG;
					} else {
						return null;
					}
				default:
					TinyLog.this.provider.log(2, null, Level.ERROR, null, TinyLog.this.formatter,
							"unknown level " + level.name(), (Object[]) null);
			}
			return null;
		}

		@Override
		public void log(final de.sgollmer.solvismax.log.LogManager.Level level, final String message,
				final Throwable throwable) {

			Level tinyLevel = this.getLevel(level);
			if (tinyLevel == null) {
				return;
			}

			String tag = null;

			switch (level) {
				case LEARN:
					tag = "LEARN";
			}
			
			this.builder.delete(0, this.builder.length());
			this.builder.append(this.className);
			this.builder.append(" - ");
			this.builder.append(message);

			TinyLog.this.provider.log(4, tag, tinyLevel, throwable, null, this.builder.toString());

		}

	}

	private File parent;

	public TinyLog() {
		this.parent = null;
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

	private boolean setConfiguration(String pathNameP) throws IOException, FileException {

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
		this.init();
		// Logger.tag("LEARN");
		return true;
	}

	@Override
	public boolean initInstance(final String path) throws IOException, FileException {
		return this.setConfiguration(path);
	}

	@Override
	public void shutdown() throws InterruptedException {
		TinyLog.this.provider.shutdown();

	}

	@Override
	public ILoggerExt create(final String className) {
		return new LoggerTiny(className);
	}

}
