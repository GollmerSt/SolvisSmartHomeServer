package de.sgollmer.solvismax.error;

public class FatalError extends Error {

	/**
	 * 
	 */
	private static final long serialVersionUID = -7009708483336767169L;

	public FatalError(String message) {
		super(message);
	}
	
	public FatalError(Throwable t) {
		super(t);
	}
}
