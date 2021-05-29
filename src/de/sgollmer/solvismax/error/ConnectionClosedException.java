package de.sgollmer.solvismax.error;

import java.io.IOException;

public class ConnectionClosedException extends IOException {

	/**
	 * 
	 */
	private static final long serialVersionUID = 4833489637308363035L;

	public ConnectionClosedException(final String message) {
		super(message);
	}

}
