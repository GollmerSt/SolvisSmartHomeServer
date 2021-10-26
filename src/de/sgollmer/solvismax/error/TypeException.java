/************************************************************************
 * 
 * $Id$
 *
 * 
 ************************************************************************/

package de.sgollmer.solvismax.error;

public class TypeException extends Exception {
	/**
	 * 
	 */
	private static final long serialVersionUID = -3880012451025478665L;

	public TypeException() {
		super();
	}

	public TypeException(final String message) {
		super(message);
	}

	public TypeException(final Throwable throwable) {
		super(throwable);
	}
}
