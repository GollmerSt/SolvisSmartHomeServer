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

	private final ILoggerBase loggerBase;

	private Collection<DelayedMessage> delayedErrorMessages = new ArrayList<>();
	private int delayedErrorCode = -1;
	private boolean initialized = false;
	private boolean bufferMessages = false;

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

		public void log(final Level level, final String message, Throwable throwable);
	}

	public interface ILoggerBase {

		public boolean initInstance(final String path) throws IOException, FileException;

		public void shutdown() throws InterruptedException;

		public ILoggerExt create(final String className);
	}

	public interface ILogger extends ILoggerExt {
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

		public void debug(final boolean debug, final String message);

		public void debug(final boolean debug, final String message, final Throwable throwable);

		public void fatalExt(final String message, final Throwable throwable);

		public void errorExt(final String message, final Throwable throwable);

		public void warnExt(final String message, final Throwable throwable);

		public void infoExt(final String message, final Throwable throwable);

		public void log(final Level level, final String message);

		public void log(final Level level, final String message, Throwable throwable, Integer errorCode);

	}

	public static class DelayedMessage {
		private final Level level;
		private final String message;
		private final Class<?> loggedClass;
		private final ILoggerExt logger;
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
			this.logger = null;
		}

		public DelayedMessage(final Level level, final String message, ILoggerExt logger, final Integer errorCode) {
			this.level = level;
			this.message = message;
			this.loggedClass = null;
			this.errorCode = errorCode;
			this.logger = logger;
		}
	}

	private LogManager() {
		switch (this.loggerName) {
			case "TinyLog":
				this.loggerBase = new TinyLog();
				break;
			case "Log4j2":
				this.loggerBase = new Logger4j2.Logger();
				break;
			default:
				this.loggerBase = null;
		}
	}

	public LogErrors createInstance(final String path) throws IOException, FileException {
		boolean successfull = this.loggerBase.initInstance(path);
		if (!successfull) {
			return LogErrors.INIT;
		}
		this.initialized = true;
		return this.outputDelayedMessages() ? LogErrors.PREVIOUS : LogErrors.OK;
	}

	public ILogger getLogger(final Class<?> loggedClass) {
		return new Logger(loggedClass);
	}

	private boolean outputDelayedMessages() {
		Level level = Level.INFO;
		synchronized (this.delayedErrorMessages) {
			for (DelayedMessage message : this.delayedErrorMessages) {
				if (message.errorCode != null && !message.level.equals(level)
						&& message.level.isLessSpecificThan(level)) {
					this.delayedErrorCode = message.errorCode;
					level = message.level;
				}

				if (this.initialized) {
					ILoggerExt logger = message.logger;
					if (logger == null) {
						logger = this.loggerBase.create(message.loggedClass.getName());
					}
					logger.log(message.level, message.message, null);
				} else {
					System.err.println(message.level.toString() + ": " + message.message);
				}
			}
			this.delayedErrorMessages.clear();
		}
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
		logger.log(level, builder.toString(), null);
	}

	public static void exit(final int errorCode) {
		LogManager logManager = LogManager.getInstance();
		if (!logManager.initialized && !logManager.delayedErrorMessages.isEmpty()) {
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
		private final String className;

		private Logger(final Class<?> clazz) {
			this.className = clazz.getName();
		}

		private ILoggerExt getLogger() {
			if (this.logger == null) {
				this.logger = LogManager.this.loggerBase.create(this.className);
			}
			return this.logger;
		}

		@Override
		public void fatal(final String message) {
			LogManager.this.log(this.getLogger(), Level.FATAL, message, null);

		}

		@Override
		public void fatal(final String message, final Throwable throwable) {
			LogManager.this.log(this.getLogger(), Level.FATAL, message, throwable);

		}

		@Override
		public void error(final String message) {
			LogManager.this.log(this.getLogger(), Level.ERROR, message, null);

		}

		@Override
		public void error(final String message, final Throwable throwable) {
			LogManager.this.log(this.getLogger(), Level.ERROR, message, throwable);
		}

		@Override
		public void learn(final String message) {
			LogManager.this.log(this.getLogger(), Level.LEARN, message, null);
		}

		@Override
		public void learn(final String message, final Throwable throwable) {
			LogManager.this.log(this.getLogger(), Level.LEARN, message, throwable);
		}

		@Override
		public void warn(final String message) {
			LogManager.this.log(this.getLogger(), Level.WARN, message, null);
		}

		@Override
		public void warn(final String message, final Throwable throwable) {
			LogManager.this.log(this.getLogger(), Level.WARN, message, throwable);
		}

		@Override
		public void info(final String message) {
			LogManager.this.log(this.getLogger(), Level.INFO, message, null);
		}

		@Override
		public void info(final String message, final Throwable throwable) {
			LogManager.this.log(this.getLogger(), Level.INFO, message, throwable);
		}

		@Override
		public void debug(final boolean debug, final String message) {
			if (debug) {
				this.info(message, null);
			} else {
				this.debug(message, null);
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
			LogManager.this.log(this.getLogger(), Level.DEBUG, message, null);
		}

		@Override
		public void debug(final String message, final Throwable throwable) {
			LogManager.this.log(this.getLogger(), Level.DEBUG, message, throwable);
		}

		@Override
		public void log(final Level level, final String message) {
			LogManager.this.log(this.getLogger(), level, message, null);
		}

		@Override
		public void log(final Level level, final String message, final Throwable throwable) {
			LogManager.this.log(this.getLogger(), level, message, throwable);
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

		@Override
		public void log(Level level, String message, Throwable throwable, Integer errorCode) {
			LogManager.this.log(this.getLogger(), level, message, throwable, errorCode);
		}

	}

	public void setBufferedMessages(boolean enable) {
		if (enable) {
			this.bufferMessages = true;
		} else {
			synchronized (this.delayedErrorMessages) {
				if (this.initialized) {
					this.outputDelayedMessages();
				}
			}
			this.bufferMessages = false;
		}
	}

	private void log(ILoggerExt loggerExt, Level level, final String message, Throwable throwable, Integer errorCode) {
		DelayedMessage delayedMessage = null;

		if (this.initialized && !this.bufferMessages) {
			try {
				loggerExt.log(level, message, throwable);
			} catch (Throwable t) {
				System.err.println("The log of <" + message + "> creates a error in the log library. Ignored.");
				t.printStackTrace();
			}
		} else if (!this.initialized || this.bufferMessages) {
			delayedMessage = new DelayedMessage(level, message, loggerExt, errorCode);
		}

		if (delayedMessage != null) {
			synchronized (this.delayedErrorMessages) {
				this.delayedErrorMessages.add(delayedMessage);
			}
		}
	}

	private void log(ILoggerExt loggerExt, Level level, final String message, Throwable throwable) {
		this.log(loggerExt, level, message, throwable, null);
	}

}
