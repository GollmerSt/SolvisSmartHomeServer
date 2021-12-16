/************************************************************************
 * 
 * $Id$
 *
 * 
 ************************************************************************/

package de.sgollmer.solvismax.error;

public class PackageException extends Exception {
	/**
	 * 
	 */
	private static final long serialVersionUID = -1025819644153485401L;

	public PackageException(final String message) {
		super(message);
	}

	public PackageException(Exception e) {
		super(e);
	}
}
