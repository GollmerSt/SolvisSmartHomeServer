package de.sgollmer.solvismax.error;

public class CryptDefaultValueException extends CryptException {

	/**
	 * 
	 */
	private static final long serialVersionUID = 4693912289461142575L;

	public CryptDefaultValueException() {
		super(Type.DEFAULT);
	}
}
