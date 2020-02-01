/************************************************************************
 * 
 * $Id$
 *
 * 
 ************************************************************************/

package de.sgollmer.solvismax;

public class Constants {
	public static final int DEBUG_USER_ACCESS_TIME = 60000;
	/**
	 * In case of an error the connection to the client ist closed delayed in case
	 * of sending the cause tro the client
	 */
	public static final int DELAYED_CLOSING_TIME = 1000;
	public static final String LOG4J_CONFIG_FILE = "log4j2.xml";
	public static final String RESOURCE_DESTINATION_PATH = "SolvisServerData";
	public static final String RESOURCE_PATH = "data";
	/**
	 * Max. number of tries, to get to the necessarry screen
	 */
	public static final int FAIL_REPEATS = 3;
	public static final int SET_REPEATS = 3;
	public static final int MAX_GOTO_DEEPTH = 10;
	public static final int LEARNING_RETRIES = 10;

	public static final int MAX_CONNECTIONS = 50;
	public static final int RETRY_STARTING_SERVER_TIME = 60000;
	public static final int WAIT_TIME_AFTER_IO_ERROR = 10000;
	public static final int ALIVE_TIME = 120000;
	public static final int WAIT_TIME_BEFORE_SENDING_MS = 100;
	/**
	 * Maximum of time, within the screen isn't read again, if no other screen is
	 * selected
	 */
	public static final int TIME_COMMAND_SCREEN_VALID = 5000;

	public static final int SCREENSAVER_INHIBIT_INTERVAL = 120000;
	public static final int WAIT_AFTER_SCREEN_SAVER_FINISHED_DETECTED = 100;

	public static final long SETTING_TIME_RANGE_LOWER = 5000;
	public static final long SETTING_TIME_RANGE_UPPER = 50000;
	public static final int TIME_ADJUSTMENT_PROPOSAL_FACTOR_PERCENT = 150;
	public static final int TIME_ADJUSTMENT_MS_N = 10 * 60000; // Possible time adjustment of the solvis time every full
																// 10
																// minutes
	public static final int TIME_FINE_ADJUSTMENT_MS_N = 5 * 60000; // Possible time adjustment of the solvis time every
																	// full 10
	// minutes
	public static final int AVERAGE_COUNT_SOLVIS_CLOCK_PRECISION_CALCULATION = 10;

	public static final int MAX_WAIT_TIME_TERMINATING_OTHER_SERVER = 60000;

	public static class ExitCodes {
		public static final int OK = 0;
		public static final int READING_CONFIGURATION_FAIL = 10;
		public static final int SERVER_PORT_IN_USE = 11;
		public static final int SERVER_TERMINATION_FAIL = 12;
		public static final int RESTART_FAILURE = 13;
		public static final int LEARNING_FAILED = 14;
		public static final int LEARNING_NECESSARY = 15;
	}
}
