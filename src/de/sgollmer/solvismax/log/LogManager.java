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

	public interface ILogger {
		public ILogger create(Class<?> clazz);

		public boolean createInstance(String path) throws IOException, FileException;

		public void shutdown() throws InterruptedException;

		public void fatal(String message);

		public void fatal(String message, Throwable throwable);

		public void error(String message);

		public void error(String message, Throwable throwable);

		public void learn(String message);

		public void learn(String message, Throwable throwable);

		public void warn(String message);

		public void warn(String message, Throwable throwable);

		public void info(String message);

		public void info(String message, Throwable throwable);

		public void debug(String message);

		public void debug(String message, Throwable throwable);

		public void log(Level level, String message);

		public void log(Level level, String message, Throwable throwable);
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
			if (level.isLessSpecificThan(Level.WARN)) {
				System.err.println(message);
			} else if (level.isLessSpecificThan(Level.INFO)) {
				System.out.println(message);
			}
		}
	}

	private final ILogger loggerBase;

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

	public LogErrors createInstance(String path) throws IOException, FileException {
		boolean successfull = this.loggerBase.createInstance(path);
		if (!successfull) {
			return LogErrors.INIT;
		}
		this.initialized = true;
		return this.outputDelayedMessages() ? LogErrors.PREVIOUS : LogErrors.OK;
	}

	public ILogger getLogger(Class<?> loggedClass) {
		return new Logger(loggedClass);
	}

	public void addDelayedErrorMessage(DelayedMessage message) {
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
				ILogger logger = this.loggerBase.create(message.loggedClass);
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

	public static void out(ILogger logger, Level level, String message, StackTraceElement[] elements) {
		StringBuilder builder = new StringBuilder(message);
		for (StackTraceElement element : elements) {
			builder.append('\n');
			builder.append(element.toString());
		}
		logger.log(level, builder.toString());
	}

	public static void exit(int errorCode) {
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

	private class Logger implements ILogger {

		private ILogger logger = null;
		private final Class<?> clazz;

		public Logger(Class<?> clazz) {
			this.clazz = clazz;
		}

		@Override
		public ILogger create(Class<?> clazz) {
			return new Logger(clazz);
		}

		@Override
		public boolean createInstance(String path) throws IOException, FileException {
			return false;
		}

		@Override
		public void shutdown() throws InterruptedException {
			LogManager.this.loggerBase.shutdown();
		}

		private ILogger getLogger() {
			if (this.logger == null) {
				this.logger = LogManager.this.loggerBase.create(this.clazz);
			}
			return this.logger;
		}

		@Override
		public void fatal(String message) {
			this.getLogger().fatal(message);

		}

		@Override
		public void fatal(String message, Throwable throwable) {
			this.getLogger().fatal(message, throwable);

		}

		@Override
		public void error(String message) {
			this.getLogger().error(message);

		}

		@Override
		public void error(String message, Throwable throwable) {
			this.getLogger().error(message, throwable);
		}

		@Override
		public void learn(String message) {
			this.getLogger().learn(message);
		}

		@Override
		public void learn(String message, Throwable throwable) {
			this.getLogger().learn(message, throwable);
		}

		@Override
		public void warn(String message) {
			this.getLogger().warn(message);
		}

		@Override
		public void warn(String message, Throwable throwable) {
			this.getLogger().warn(message, throwable);
		}

		@Override
		public void info(String message) {
			this.getLogger().info(message);
		}

		@Override
		public void info(String message, Throwable throwable) {
			this.getLogger().info(message, throwable);
		}

		@Override
		public void debug(String message) {
			this.getLogger().debug(message);
		}

		@Override
		public void debug(String message, Throwable throwable) {
			this.getLogger().debug(message, throwable);
		}

		@Override
		public void log(Level level, String message) {
			this.getLogger().log(level, message);
		}

		@Override
		public void log(Level level, String message, Throwable throwable) {
			this.getLogger().log(level, message, throwable);
		}

	}

}
