/************************************************************************
 * 
 * $Id$
 *
 * 
 ************************************************************************/

package de.sgollmer.solvismax.connection;

public enum ConnectionStatus {
	CLIENT_UNKNOWN,
	CONNECTION_NOT_POSSIBLE,
//	CONNECTED, DISCONNECTED,
	COMMAND_ERROR,
	USER_ACCESS_DETECTED,
	SERVICE_ACCESS_DETECTED,
	HUMAN_ACCESS_FINISHED,
	ALIVE
}
