/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package de.sgollmer.solvismax.log;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;

import org.apache.logging.log4j.core.async.AsyncLogger;
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

	public static void createInstance(String pathName) throws IOException {
		LOGGER = new Logger2(pathName);
		LOGGER.setConfiguration();
	}

	private static Logger2 LOGGER;

	private final File parent;

	public Logger2(String pathName) {
		File parent;

		if (pathName == null) {
			String writeDirectory = System.getProperty("user.home");
			if (System.getProperty("os.name").startsWith("Windows")) {
				writeDirectory = System.getenv("APPDATA");
			}

			writeDirectory += File.separator + "SolvisMaxJava";

			parent = new File(writeDirectory);
		} else {
			parent = new File(pathName);
		}
		this.parent = parent;
	}

	private void copyFiles() throws IOException {

		boolean success = true;

		if (!parent.exists()) {
			success = parent.mkdir();
		}

		if (!success) {
			throw new FileError("Error on creating directory <" + parent.getAbsolutePath() + ">");
		}

		File xml = new File(this.parent, Constants.LOG4J_CONFIG_FILE);

		if (!xml.exists() ) {
			FileHelper.copyFromResource(Constants.RESOURCE_PATH + File.separator + Constants.LOG4J_CONFIG_FILE, xml, "****LogPath****",
					this.parent.getAbsolutePath());
		}

	}

	public void setConfiguration() throws IOException {

		copyFiles();

		File xml = new File(this.parent, Constants.LOG4J_CONFIG_FILE);

		InputStream input = null;

		if (xml.exists()) {
			try {
				input = new FileInputStream(xml);
			} catch (FileNotFoundException ex) {
				System.err.println("Error on reading log4j.xml");
				System.exit(-1);
			}
		}
		ConfigurationSource source = new ConfigurationSource(input);
		Configurator.initialize(null, source);
	}
	
}