/************************************************************************
 * 
 * $Id$
 *
 * 
 ************************************************************************/

package de.sgollmer.solvismax;

import java.util.regex.Pattern;

import de.sgollmer.solvismax.objects.Coordinate;

public class Constants {
	/**
	 * In case of an error the connection to the client ist closed delayed in case
	 * of sending the cause tro the client
	 */

	public static final int DELAYED_CLOSING_TIME = 1000;
	public static final String LOG4J_CONFIG_FILE = "log4j2.xml";

	public static final String TINY_LOG_CONFIG_FILE = "tinylog.properties";
	public static final String TINY_LOG_FILE_PROPERTY_PREFIX = "writer";
	public static final String TINY_LOG_FILE_PROPERTY_SUFFIX = ".file";
	
	public static final String SUN_JAVA_COMMAND = "sun.java.command";

	public static final int MIN_TOUCH_TIME = 100;
	public static final int MAX_WAIT_TIME_ON_STARTUP = 120000;

	public static final int SCREEN_SAVER_IGNORED_FRAME_SIZE = 3;
	public static final int CRYPT_PREVIOUS = 233;
	/**
	 * Max. number of tries, to get to the necessarry screen
	 */
	public static final int FAIL_REPEATS = 3;
	public static final int SET_REPEATS = 3;
	public static final int PREPARATION_REPEATS = 3;
	public static final int MAX_GOTO_DEEPTH = 10;
	public static final int LEARNING_RETRIES = 10;
	public static final int COMMAND_TO_QUEUE_END_AFTER_N_FAILURES = 3;
	public static final int COMMAND_IGNORED_AFTER_N_FAILURES = 10;

	public static final int HOURLY_EQUIPMENT_WINDOW_READ_INTERVAL_FACTOR = 3;

	public static final int MAX_CONNECTIONS = 50;
	public static final int RETRY_STARTING_SERVER_TIME = 60000;
	public static final int WAIT_TIME_AFTER_QUEUE_EMPTY = 500;
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

	public static final int SYNC_TOLERANCE_PERCENT = 10;

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

	public static final String[] CRYPT_NOT_CONFIGURED_VALUES = new String[] { "none", "aes-codes" };

	public static class Solvis {
		public static final int INPUT_BUFFER_SIZE = 1024;
		public static final String DISPLAY = "/display.bmp?";
		public static final String XML = "/sc2_val.xml";
		public static final String TOUCH = "/Touch.CGI?";
		public static final String X = "x=";
		public static final String Y = "y=";
		public static final String TASTER = "/Taster.CGI?taste=";
		public static final String ID = "i=";
		public static final Coordinate RELEASE_COORDINATE = new Coordinate(260, 260);
		public static final int INTERRUPT_AFTER_N_TOUCHES = 5; // After 5 touches, the screen
																// command can be interrupted
	}

	public static class Files {
		public static final int INPUT_BUFFER_SIZE = 8192;
		public static final String RESOURCE_DESTINATION = "SolvisServerData";
		public static final String LEARN_DESTINATION = "LearnedImages";
		public static final String SOLVIS_ERROR_DESTINATION = "SolvisErrorImages";
		public static final String RESOURCE = "data";
		public static final String ERROR_SCREEN_PREFIX = "ErrorScreen-";
		public static final String GRAFIC_SUFFIX = "png";

		public static final String SOLVIS_SCREEN = "SolvisScreen." + Files.GRAFIC_SUFFIX;

		public static final Pattern ERROR_SCREEN_REGEX = Pattern
				.compile(Files.ERROR_SCREEN_PREFIX + "\\d+\\." + Files.GRAFIC_SUFFIX);
		public static final int MAX_NUMBER_OF_ERROR_SCREENS = 100;
	}

	public static class XmlStrings {
		public static final String XML_MEASUREMENT_BOOLEAN = "BooleanValue";
		public static final String XML_MEASUREMENT_INTEGER = "IntegerValue";
		public static final String XML_MEASUREMENT_STRING = "StringValue";
		public static final String XML_MEASUREMENT_MODE = "ModeValue";
	}

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
				"/+/server/cmnd", // Server commands
				"/+/online", //
				"/+/+/server/cmnd", // Server commands
				"/+/+/+/cmnd", // SET
				"/+/+/+/update", // GET
				// "/#"
		};
		public static final String DATA_SUFFIX = "/data";
		public static final String META_SUFFIX = "/meta";
		public static final String SERVER_PREFIX = "server";
		public static final String SCREEN_PREFIX = "screen";
		public static final String ONLINE_STATUS = "/online";
		public static final String STATUS = "/status";
		public static final String HUMAN_ACCESS = "/human_access";
		public static final String ERROR = "/error";
		public static final int MAX_INFLIGHT = 200;
		public static final int MIN_CONNECTION_REPEAT_TIME = 1000;
		public static final int MAX_CONNECTION_REPEAT_TIME = 120000;

	}

	public static class Debug {

		public static boolean DEBUG = false;

		public static final int USER_ACCESS_TIME = 60000;
		public static final boolean NO_MAIL = false; // kein Mailversand
		public static final boolean SCREEN_SAVER_DETECTION = false; // more debugging info
		public static final boolean DEBUG_TWO_STATIONS = false;

		public static final boolean SOLVIS_TIME_FIXED = false;
		public static final boolean SOLVIS_RESULT_NULL = false;

		public static final boolean CORRECTION = true;

		public static final boolean BURST = false;

		public static boolean isSolvisTimeFixed() {
			return DEBUG && SOLVIS_TIME_FIXED;
		}

		public static boolean isSolvisResultNull() {
			return DEBUG && SOLVIS_RESULT_NULL;
		}

		public static final boolean OVERWRITE_ONLY_ON_LEARN = false;

	}

	public static class Csv {
		public static final String ID = "id";
		public static final String NAME = "NAME";
		public static final String ALIAS = "ALIAS";
		public static final String MQTT = "MQTT topic";
		public static final String CHANNEL_TYPE = "Channel type";
		public static final String BUFFERED = "Buffered";
		public static final String UNIT = "Unit";
		public static final String GLITCH_INHIBIT = "Glitch inhibit";
		public static final String DIVISOR = "Divisor";
		public static final String AVERAGE = "Average";
		public static final String DELAY_AFTER_ON = "Delay after on";
		public static final String FAST = "Fast";
		public static final String STRATEGY = "Strategy";
		public static final String CORRECTION = "Correction";
		public static final String OPTIONAL = "Optional";
		public static final String INCREMENT = "Increment";
		public static final String LEAST = "Least";
		public static final String MOST = "Most";
		public static final String INCREMENT_CHANGE = "Increment change";
		public static final String CHANGED_INCREMENT = "Changed increment";
		public static final String MODES = "Modes";
		public static final String[] HEADER = new String[] { ID, NAME, ALIAS, MQTT, CHANNEL_TYPE, BUFFERED, UNIT,
				GLITCH_INHIBIT, DIVISOR, AVERAGE, DELAY_AFTER_ON, FAST, STRATEGY, CORRECTION, OPTIONAL, INCREMENT,
				LEAST, MOST, INCREMENT_CHANGE, CHANGED_INCREMENT, MODES };

	}
}
