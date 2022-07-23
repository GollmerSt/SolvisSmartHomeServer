package de.sgollmer.solvismax.error;

import java.util.Collection;

public class ObserverException extends RuntimeException {

	/**
	 * 
	 */
	private static final long serialVersionUID = 296108852963074677L;

	private final Collection<ObserverException> exceptions;

	public ObserverException(final Collection<ObserverException> exceptions) {
		this.exceptions = exceptions;
	}

	public ObserverException() {
		this(null);
	}

	public Collection<ObserverException> getExceptions() {
		return this.exceptions;
	}
}
