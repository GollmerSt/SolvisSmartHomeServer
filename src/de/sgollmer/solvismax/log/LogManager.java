/************************************************************************
 * 
 * $Id$
 *
 * 
 ************************************************************************/

/**
 * A facade for loggers
 */

package de.sgollmer.solvismax.log;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;

import de.sgollmer.solvismax.error.FileException;

public class LogManager {

	private final String loggerName = "TinyLog"; // Possibilities: "Log4j2", "TinyLog"

	public static LogManager getInstance() {
		LogManager logManager = LogManagerHolder.INSTANCE;
		return logManager;
	}

	private static class LogManagerHolder {

		private static final LogManager INSTANCE = new LogManager();
	}

	public enum Level {
		FATAL(10), ERROR(20), LEARN(30), WARN(40), INFO(50), DEBUG(60);

		private final int prio;

		private Level(int prio) {
			this.prio = prio;
		}

		private boolean isLessSpecificThan(Level level) {
			return this.prio < level.prio;
		}

		public static Level getLevel(String levelString) {
			return Level.valueOf(levelString);
		}
	}

	public enum LogErrors {
		OK, INIT, PREVIOUS
	}

	public interface ILoggerExt {
		public ILoggerExt create(Class<?> clazz);

		public boolean createInstance(final String path) throws IOException, FileException;

		public void shutdown() throws InterruptedException;

		public void fatal(final String message);

		public void fatal(final String message, final Throwable throwable);

		public void error(final String message);

		public void error(final String message, final Throwable throwable);

		public void learn(final String message);

		public void learn(final String message, final Throwable throwable);

		public void warn(final String message);

		public void warn(final String message, final Throwable throwable);

		public void info(final String message);

		public void info(final String message, final Throwable throwable);

		public void debug(final String message);

		public void debug(final String message, final Throwable throwable);

		public void log(final Level level, final String message);

		public void log(final Level level, final String message, Throwable throwable);
	}

	public interface ILogger extends ILoggerExt {
		public void debug(final boolean debug, final String message);

		public void debug(final boolean debug, final String message, final Throwable throwable);

		public void fatalExt(final String message, final Throwable throwable);

		public void errorExt(final String message, final Throwable throwable);

		public void warnExt(final String message, final Throwable throwable);

		public void infoExt(final String message, final Throwable throwable);
	}

	public static class DelayedMessage {
		private final Level level;
		private final String message;
		private final Class<?> loggedClass;
		private final Integer errorCode;

		public DelayedMessage(final Level level, final String message, final Class<?> loggedClass,
				final Integer errorCode) {
			this.level = level;
			this.message = message;
			this.loggedClass = loggedClass;
			this.errorCode = errorCode;
			if (level.isLessSpecificThan(Level.WARN)) {
				System.err.println(message);
			} else if (level.isLessSpecificThan(Level.INFO)) {
				System.out.println(message);
			}
		}
	}

	private final ILoggerExt loggerBase;

	private Collection<DelayedMessage> delayedErrorMessages = new ArrayList<>();
	private int delayedErrorCode = -1;
	private boolean initialized = false;

	private LogManager() {
		switch (this.loggerName) {
			case "TinyLog":
				this.loggerBase = new TinyLog.LoggerTiny();
				break;
			case "Log4j2":
				this.loggerBase = new Logger4j2.Logger();
				break;
			default:
				this.loggerBase = null;
		}
	}

	public LogErrors createInstance(final String path) throws IOException, FileException {
		boolean successfull = this.loggerBase.createInstance(path);
		if (!successfull) {
			return LogErrors.INIT;
		}
		this.initialized = true;
		return this.outputDelayedMessages() ? LogErrors.PREVIOUS : LogErrors.OK;
	}

	public ILogger getLogger(final Class<?> loggedClass) {
		return new Logger(loggedClass);
	}

	public void addDelayedErrorMessage(final DelayedMessage message) {
		if (this.initialized) {
			ILogger logger = this.getLogger(message.loggedClass);
			logger.log(message.level, message.message);

		} else {
			this.delayedErrorMessages.add(message);
		}
	}

	private boolean outputDelayedMessages() {
		Level level = Level.INFO;
		for (DelayedMessage message : this.delayedErrorMessages) {
			if (message.errorCode != null && !message.level.equals(level) && message.level.isLessSpecificThan(level)) {
				this.delayedErrorCode = message.errorCode;
				level = message.level;
			}

			if (this.initialized) {
				ILoggerExt logger = this.loggerBase.create(message.loggedClass);
				logger.log(message.level, message.message);
			} else {
				System.err.println(message.level.toString() + ": " + message.message);
			}
		}
		this.delayedErrorMessages.clear();
		return this.delayedErrorCode != -1;
	}

	private int getExitCode() {
		if (this.delayedErrorCode == 0) {
			Level level = Level.INFO;
			this.delayedErrorCode = -1;
			for (DelayedMessage message : this.delayedErrorMessages) {

				if (message.errorCode != null && !message.level.equals(level)
						&& message.level.isLessSpecificThan(level)) {
					this.delayedErrorCode = message.errorCode;
					level = message.level;
				}
			}
		}
		return this.delayedErrorCode;
	}

	public static void out(final ILogger logger, final Level level, final String message,
			final StackTraceElement[] elements) {
		StringBuilder builder = new StringBuilder(message);
		for (StackTraceElement element : elements) {
			builder.append('\n');
			builder.append(element.toString());
		}
		logger.log(level, builder.toString());
	}

	public static void exit(final int errorCode) {
		LogManager logManager = LogManager.getInstance();
		if (!logManager.initialized && logManager.delayedErrorMessages.isEmpty()) {
			boolean error = logManager.outputDelayedMessages();
			if (error) {
				logManager.shutdown();
				System.exit(logManager.getExitCode());
			}
		}
		logManager.shutdown();
		System.exit(errorCode);
	}

	public void shutdown() {
		try {
			if (this.initialized) {
				this.loggerBase.shutdown();
			}
		} catch (InterruptedException e) {
		}
	}

	private String extended(final String message, final Throwable throwable) {
		StringBuilder builder = new StringBuilder(message);
		builder.append('\n');
		builder.append(throwable.getMessage());
		for (StackTraceElement element : throwable.getStackTrace()) {
			builder.append('\n');
			builder.append(element.toString());
		}
		return builder.toString();
	}

	private class Logger implements ILogger {

		private ILoggerExt logger = null;
		private final Class<?> clazz;

		public Logger(final Class<?> clazz) {
			this.clazz = clazz;
		}

		@Override
		public ILogger create(final Class<?> clazz) {
			return new Logger(clazz);
		}

		@Override
		public boolean createInstance(final String path) throws IOException, FileException {
			return false;
		}

		@Override
		public void shutdown() throws InterruptedException {
			LogManager.this.loggerBase.shutdown();
		}

		private ILoggerExt getLogger() {
			if (this.logger == null) {
				this.logger = LogManager.this.loggerBase.create(this.clazz);
			}
			return this.logger;
		}

		@Override
		public void fatal(final String message) {
			this.getLogger().fatal(message);

		}

		@Override
		public void fatal(final String message, final Throwable throwable) {
			this.getLogger().fatal(message, throwable);

		}

		@Override
		public void error(final String message) {
			this.getLogger().error(message);

		}

		@Override
		public void error(final String message, final Throwable throwable) {
			this.getLogger().error(message, throwable);
		}

		@Override
		public void learn(final String message) {
			this.getLogger().learn(message);
		}

		@Override
		public void learn(final String message, final Throwable throwable) {
			this.getLogger().learn(message, throwable);
		}

		@Override
		public void warn(final String message) {
			this.getLogger().warn(message);
		}

		@Override
		public void warn(final String message, final Throwable throwable) {
			this.getLogger().warn(message, throwable);
		}

		@Override
		public void info(final String message) {
			this.getLogger().info(message);
		}

		@Override
		public void info(final String message, final Throwable throwable) {
			this.getLogger().info(message, throwable);
		}

		@Override
		public void debug(final boolean debug, final String message) {
			if (debug) {
				this.info(message);
			} else {
				this.debug(message);
			}
		};

		@Override
		public void debug(final boolean debug, final String message, final Throwable throwable) {
			if (debug) {
				this.info(message, throwable);
			} else {
				this.debug(message, throwable);
			}
		};

		@Override
		public void debug(final String message) {
			this.getLogger().debug(message);
		}

		@Override
		public void debug(final String message, final Throwable throwable) {
			this.getLogger().debug(message, throwable);
		}

		@Override
		public void log(final Level level, final String message) {
			this.getLogger().log(level, message);
		}

		@Override
		public void log(final Level level, final String message, final Throwable throwable) {
			this.getLogger().log(level, message, throwable);
		}

		@Override
		public void fatalExt(final String message, final Throwable throwable) {
			this.fatal(extended(message, throwable));

		}

		@Override
		public void errorExt(final String message, final Throwable throwable) {
			this.error(extended(message, throwable));

		}

		@Override
		public void warnExt(final String message, final Throwable throwable) {
			this.warn(extended(message, throwable));

		}

		@Override
		public void infoExt(final String message, final Throwable throwable) {
			this.info(extended(message, throwable));

		}

	}

}
