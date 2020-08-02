/************************************************************************
 * 
 * $Id: Mqtt.java 277 2020-07-19 16:00:49Z stefa_000 $
 *
 * 
 ************************************************************************/

package de.sgollmer.solvismax.connection.mqtt;

import java.util.ResourceBundle;

import de.sgollmer.solvismax.log.LogManager;
import de.sgollmer.solvismax.log.LogManager.ILogger;

public class Logger implements org.eclipse.paho.client.mqttv3.logging.Logger {

	@Override
	public void initialise(ResourceBundle messageCatalog, String loggerID, String resourceName) {

	}

	@Override
	public void setResourceName(String logContext) {

	}

	@Override
	public boolean isLoggable(int level) {
		switch (level) {
			case CONFIG:
			case INFO:
			case SEVERE:
			case WARNING:
				return true;
		}
		return false;
	}

	@Override
	public void severe(String sourceClass, String sourceMethod, String msg) {
		if (!isLoggable(SEVERE)) {
			return;
		}
		ILogger logger = LogManager.getInstance().getLogger(getClass(sourceClass));
		logger.error(msg);
	}

	@Override
	public void severe(String sourceClass, String sourceMethod, String msg, Object[] inserts) {
		if (!isLoggable(SEVERE)) {
			return;
		}
		ILogger logger = LogManager.getInstance().getLogger(getClass(sourceClass));
		logger.error(java.text.MessageFormat.format(msg, inserts));
	}

	@Override
	public void severe(String sourceClass, String sourceMethod, String msg, Object[] inserts, Throwable thrown) {
		if (!isLoggable(SEVERE)) {
			return;
		}
		ILogger logger = LogManager.getInstance().getLogger(getClass(sourceClass));
		logger.error(java.text.MessageFormat.format(msg, inserts), thrown);
	}

	@Override
	public void warning(String sourceClass, String sourceMethod, String msg) {
		if (!isLoggable(WARNING)) {
			return;
		}
		ILogger logger = LogManager.getInstance().getLogger(getClass(sourceClass));
		logger.warn(msg);
	}

	@Override
	public void warning(String sourceClass, String sourceMethod, String msg, Object[] inserts) {
		if (!isLoggable(WARNING)) {
			return;
		}
		ILogger logger = LogManager.getInstance().getLogger(getClass(sourceClass));
		logger.warn(java.text.MessageFormat.format(msg, inserts));
	}

	@Override
	public void warning(String sourceClass, String sourceMethod, String msg, Object[] inserts, Throwable thrown) {
		if (!isLoggable(WARNING)) {
			return;
		}
		ILogger logger = LogManager.getInstance().getLogger(getClass(sourceClass));
		logger.warn(java.text.MessageFormat.format(msg, inserts), thrown);
	}

	@Override
	public void info(String sourceClass, String sourceMethod, String msg) {
		if (!isLoggable(INFO)) {
			return;
		}
		ILogger logger = LogManager.getInstance().getLogger(getClass(sourceClass));
		logger.info(msg);
	}

	@Override
	public void info(String sourceClass, String sourceMethod, String msg, Object[] inserts) {
		if (!isLoggable(INFO)) {
			return;
		}
		ILogger logger = LogManager.getInstance().getLogger(getClass(sourceClass));
		logger.info(java.text.MessageFormat.format(msg, inserts));
	}

	@Override
	public void info(String sourceClass, String sourceMethod, String msg, Object[] inserts, Throwable thrown) {
		if (!isLoggable(INFO)) {
			return;
		}
		ILogger logger = LogManager.getInstance().getLogger(getClass(sourceClass));
		logger.info(java.text.MessageFormat.format(msg, inserts), thrown);
	}

	@Override
	public void config(String sourceClass, String sourceMethod, String msg) {
		if (!isLoggable(CONFIG)) {
			return;
		}
		ILogger logger = LogManager.getInstance().getLogger(getClass(sourceClass));
		logger.info(msg);
	}

	@Override
	public void config(String sourceClass, String sourceMethod, String msg, Object[] inserts) {
		if (!isLoggable(CONFIG)) {
			return;
		}
		ILogger logger = LogManager.getInstance().getLogger(getClass(sourceClass));
		logger.info(java.text.MessageFormat.format(msg, inserts));
	}

	@Override
	public void config(String sourceClass, String sourceMethod, String msg, Object[] inserts, Throwable thrown) {
		if (!isLoggable(CONFIG)) {
			return;
		}
		ILogger logger = LogManager.getInstance().getLogger(getClass(sourceClass));
		logger.info(java.text.MessageFormat.format(msg, inserts), thrown);
	}

	public void debug(String sourceClass, String sourceMethod, String msg) {
		ILogger logger = LogManager.getInstance().getLogger(getClass(sourceClass));
		logger.debug(msg);
	}

	public void debug(String sourceClass, String sourceMethod, String msg, Object[] inserts) {
		ILogger logger = LogManager.getInstance().getLogger(getClass(sourceClass));
		logger.debug(java.text.MessageFormat.format(msg, inserts));
	}

	public void debug(String sourceClass, String sourceMethod, String msg, Object[] inserts, Throwable thrown) {
		ILogger logger = LogManager.getInstance().getLogger(getClass(sourceClass));
		logger.debug(java.text.MessageFormat.format(msg, inserts), thrown);
	}

	@Override
	public void fine(String sourceClass, String sourceMethod, String msg) {
		if (!isLoggable(FINE)) {
			return;
		}
		this.debug(sourceClass, sourceMethod, msg);
	}

	@Override
	public void fine(String sourceClass, String sourceMethod, String msg, Object[] inserts) {
		if (!isLoggable(FINE)) {
			return;
		}
		this.debug(sourceClass, sourceMethod, msg);
	}

	@Override
	public void fine(String sourceClass, String sourceMethod, String msg, Object[] inserts, Throwable thrown) {
		if (!isLoggable(FINE)) {
			return;
		}
		this.debug(sourceClass, sourceMethod, msg);
	}

	@Override
	public void finer(String sourceClass, String sourceMethod, String msg) {
		if (!isLoggable(FINER)) {
			return;
		}
		this.debug(sourceClass, sourceMethod, msg);
	}

	@Override
	public void finer(String sourceClass, String sourceMethod, String msg, Object[] inserts) {
		if (!isLoggable(FINER)) {
			return;
		}
		this.debug(sourceClass, sourceMethod, msg, inserts);
	}

	@Override
	public void finer(String sourceClass, String sourceMethod, String msg, Object[] inserts, Throwable ex) {
		if (!isLoggable(FINER)) {
			return;
		}
		this.debug(sourceClass, sourceMethod, msg, inserts, ex);
	}

	@Override
	public void finest(String sourceClass, String sourceMethod, String msg) {
		if (!isLoggable(FINEST)) {
			return;
		}
		this.debug(sourceClass, sourceMethod, msg);
	}

	@Override
	public void finest(String sourceClass, String sourceMethod, String msg, Object[] inserts) {
		if (!isLoggable(FINEST)) {
			return;
		}
		this.debug(sourceClass, sourceMethod, msg, inserts);
	}

	@Override
	public void finest(String sourceClass, String sourceMethod, String msg, Object[] inserts, Throwable ex) {
		if (!isLoggable(FINEST)) {
			return;
		}
		this.debug(sourceClass, sourceMethod, msg, inserts, ex);
	}

	@Override
	public void log(int level, String sourceClass, String sourceMethod, String msg, Object[] inserts,
			Throwable thrown) {
		switch (level) {
			case CONFIG:
				this.config(sourceClass, sourceMethod, msg, inserts);
				break;
			case FINE:
				this.fine(sourceClass, sourceMethod, msg, inserts);
				break;
			case FINER:
				this.finer(sourceClass, sourceMethod, msg, inserts);
				break;
			case FINEST:
				this.finest(sourceClass, sourceMethod, msg, inserts);
				break;
			case INFO:
				this.info(sourceClass, sourceMethod, msg, inserts);
				break;
			case SEVERE:
				this.severe(sourceClass, sourceMethod, msg, inserts);
				break;
			case WARNING:
				this.warning(sourceClass, sourceMethod, msg, inserts);
				break;
		}
	}

	@Override
	public void trace(int level, String sourceClass, String sourceMethod, String msg, Object[] inserts, Throwable ex) {
		switch (level) {
			case CONFIG:
				this.config(sourceClass, sourceMethod, msg, inserts, ex);
				break;
			case FINE:
				this.fine(sourceClass, sourceMethod, msg, inserts, ex);
				break;
			case FINER:
				this.finer(sourceClass, sourceMethod, msg, inserts, ex);
				break;
			case FINEST:
				this.finest(sourceClass, sourceMethod, msg, inserts, ex);
				break;
			case INFO:
				this.info(sourceClass, sourceMethod, msg, inserts, ex);
				break;
			case SEVERE:
				this.severe(sourceClass, sourceMethod, msg, inserts, ex);
				break;
			case WARNING:
				this.warning(sourceClass, sourceMethod, msg, inserts, ex);
				break;
		}
	}

	@Override
	public String formatMessage(String msg, Object[] inserts) {
		return java.text.MessageFormat.format(msg, inserts);
	}

	@Override
	public void dumpTrace() {
		// TODO Auto-generated method stub

	}

	private Class<?> getClass(String sourceClass) {
		Class<?> clazz = null;
		try {
			clazz = Class.forName(sourceClass);
		} catch (ClassNotFoundException e) {
		}
		return clazz;
	}

}
