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

	public static final String TINY_LOG_CONFIG_FILE = "tinylog.properties";
	public static final String TINY_LOG_FILE_PROPERTY_PREFIX = "writer";
	public static final String TINY_LOG_FILE_PROPERTY_SUFFIX = ".file";
	
	public static final int MIN_TOUCH_TIME = 100 ;

	public static final String RESOURCE_DESTINATION_PATH = "SolvisServerData";
	public static final String RESOURCE_PATH = "data";
	public static final int SCREEN_SAVER_IGNORED_FRAME_SIZE = 3;
	public static final int CRYPT_PREVIOUS = 233;
	/**
	 * Max. number of tries, to get to the necessarry screen
	 */
	public static final int FAIL_REPEATS = 3;
	public static final int SET_REPEATS = 3;
	public static final int MAX_GOTO_DEEPTH = 10;
	public static final int LEARNING_RETRIES = 10;
	public static final int COMMAND_TO_QUEUE_END_AFTER_N_FAILURES = 3;
	public static final int COMMAND_IGNORED_AFTER_N_FAILURES = 10;

	public static final int HOURLY_EQUIPMENT_SYNCHRONISATION_READ_INTERVAL_FACTOR = 6;

	public static final int MAX_CONNECTIONS = 50;
	public static final int RETRY_STARTING_SERVER_TIME = 60000;
	public static final int WAIT_TIME_AFTER_IO_ERROR = 10000;
	public static final int WAIT_TIME_AFTER_MQTT_ERROR = 100;
	public static final int ALIVE_TIME = 120000;
	public static final int FORCE_UPDATE_AFTER_N_INTERVALS = 3;
	public static final int WAIT_TIME_AFTER_THROWABLE = 30000;
	/**
	 * Maximum of time, within the screen isn't read again, if no other screen is
	 * selected
	 */
	public static final int TIME_COMMAND_SCREEN_VALID = 5000;

	public static final int MAX_OUTSIDE_TIME = 120000;
	public static final int SCREEN_SAVER_WIDTH_INACCURACY = 5;
	public static final int SCREEN_SAVER_HEIGHT_INACCURACY = 5;

	public static final int WAIT_AFTER_FIRST_ASYNC_DETECTION = 100;

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
	public static final double PRECISION_DOUBLE = 0.000001D;

	public static final int MAX_WAIT_TIME_TERMINATING_OTHER_SERVER = 60000;
	public static final int NUMBER_OF_CONTROL_FILE_DUPLICATES = 3;

	public static final int MODBUS_SLAVE_ID = 1;

	public static class ExitCodes {
		public static final int OK = 0;
		public static final int READING_CONFIGURATION_FAIL = 10;
		public static final int SERVER_PORT_IN_USE = 11;
		public static final int SERVER_TERMINATION_FAIL = 12;
		public static final int RESTART_FAILURE = 13;
		public static final int LEARNING_FAILED = 14;
		public static final int LEARNING_NECESSARY = 15;
		public static final int ARGUMENT_FAIL = 16;
		public static final int CRYPTION_FAIL = 17;
		public static final int MAILING_ERROR = 18;
		public static final int BASE_XML_ERROR = 19;
		public static final int XML_VERIFICATION_ERROR = 20;
		public static final int MQTT_ERROR = 21;
		public static final int TASK_CREATING_ERROR = 22;
	}

	public static class Mqtt {
		public static final String[] CMND_SUFFIXES = new String[] { //
				"/+/server/cmnd", //	Server commands
				"/+/online",//
				"/+/+/server/cmnd", //	Server commands
				"/+/+/+/cmnd", //	SET
				"/+/+/+/update", //	GET
				//"/#"
		};
		public static final String DATA_SUFFIX = "/data";
		public static final String META_SUFFIX = "/meta";
		public static final String SERVER_PREFIX = "server";
		public static final String ONLINE_STATUS = "/online";
		public static final String STATUS = "/status";
		public static final String HUMAN_ACCESS = "/human_access";
		public static final String ERROR = "/error";
		public static final int MAX_INFLIGHT = 200;
		public static final int MIN_CONNECTION_REPEAT_TIME = 1000;
		public static final int MAX_CONNECTION_REPEAT_TIME = 120000;

	}

}
