package de.sgollmer.solvismax.error;

public class CryptDefaultValueException extends Exception {

	/**
	 * 
	 */
	private static final long serialVersionUID = 4693912289461142575L;

	public CryptDefaultValueException() {
		super("The crypted password has not yet been entered and is not encrypted.");
	}
}
