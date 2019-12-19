package de.sgollmer.solvismax;

public class Constants {
	public static final boolean DEBUG = true;
	/**
	 * In case of an error the connection to the client ist closed delayed in
	 * case of sending the cause tro the client
	 */
	public static final int DELAYED_CLOSING_TIME = 1000;
	public static final String LOG4J_CONFIG_FILE = "log4j2.xml";
	// public static final String RESOURCE_PATH = File.separator + "de" +
	// File.separator + "sgollmer" + File.separator
	// + "solvismax" + File.separator + "data";
	public static final String RESOURCE_PATH = "data";
	/**
	 * Max. number of tries, to get to the necessarry screen
	 */
	public static final int GOTO_SCREEN_TRIES = 20;
	public static final int RETRY_STARTING_SERVER_TIME = 60000;
	public static final int ALIVE_TIME = 120000;
	public static int WAIT_TIME_BEFORE_SENDING_MS = 100;
	public static int SCREENSAVER_INHIBIT_INTERVAL = 120000;
	public static final int MAX_CONNECTIONS = 50 ;

}
