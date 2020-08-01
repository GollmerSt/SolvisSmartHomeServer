/************************************************************************
 * 
 * $Id$
 *
 * 
 ************************************************************************/

package de.sgollmer.solvismax.error;

public class ModbusException extends Exception {

	/**
	 * 
	 */
	private static final long serialVersionUID = -1449257291698741886L;

	public ModbusException(Throwable error) {
		super(error);
	}

	public ModbusException(String message, Throwable error) {
		super(message, error);
	}

}
