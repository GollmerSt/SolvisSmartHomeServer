package de.sgollmer.solvismax.error;

public class ModbusError extends Error {
	
	/**
	 * 
	 */
	private static final long serialVersionUID = -1449257291698741886L;

	public ModbusError(Throwable error) {
		super( error ) ;
	}

	public ModbusError(String message,Throwable error) {
		super( message, error ) ;
	}

}
