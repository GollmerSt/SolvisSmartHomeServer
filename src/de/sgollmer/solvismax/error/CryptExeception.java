package de.sgollmer.solvismax.error;

public class CryptExeception extends Exception {

	/**
	 * 
	 */
	private static final long serialVersionUID = 7353872631845585761L;

	public CryptExeception() {
		super("Decryption error");
	}

}
