/************************************************************************
 * 
 * $Id$
 *
 * 
 ************************************************************************/

package de.sgollmer.solvismax.connection;

public enum ConnectionStatus {
	CLIENT_UNKNOWN(false), //
	CONNECTION_NOT_POSSIBLE(false), //
//	CONNECTED, DISCONNECTED,
	COMMAND_ERROR(false), //
	ALIVE(false);

	private final boolean humanAccess;

	private ConnectionStatus(final boolean humanAccess) {
		this.humanAccess = humanAccess;
	}

	public boolean isHumanAccess() {
		return this.humanAccess;
	}

}
