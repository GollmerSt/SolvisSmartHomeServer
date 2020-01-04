/************************************************************************
 * 
 * $Id$
 *
 * 
 ************************************************************************/

package de.sgollmer.solvismax;

public class Constants {
	public static final boolean DEBUG = true;
	public static final int DEBUG_USER_ACCESS_TIME = 60000;
	/**
	 * In case of an error the connection to the client ist closed delayed in case
	 * of sending the cause tro the client
	 */
	public static final int DELAYED_CLOSING_TIME = 1000;
	public static final String LOG4J_CONFIG_FILE = "log4j2.xml";
	public static final String RESOURCE_DESTINATION_PATH = "SolvisXml";
	public static final String RESOURCE_PATH = "data";
	/**
	 * Max. number of tries, to get to the necessarry screen
	 */
	public static final int FAIL_REPEATS = 3;
	public static final int SET_REPEATS = 3 ;
	public static final int MAX_GOTO_DEEPTH = 10;
	public static final int RETRY_STARTING_SERVER_TIME = 60000;
	public static final int ALIVE_TIME = 120000;
	public static final int WAIT_TIME_BEFORE_SENDING_MS = 100;
	public static final int SCREENSAVER_INHIBIT_INTERVAL = 120000;
	public static final int MAX_CONNECTIONS = 50;
	public static final int LEARNING_RETRIES = 10;
	public static final int WAITTIME_IF_LE_ZERO = 100;
	
	public static final int MIN_TIME_ERROR_ADJUSTMENT_S = 40000 ;
	public static final int TIME_ADJUSTMENT_PROPOSAL_FACTOR_PERCENT = 150 ;
	public static final int TIME_ADJUSTMENT_MINUTE_N = 10 ;
	
	public static final int TIME_COMMAND_SCREEN_VALID = 5000 ;
}
