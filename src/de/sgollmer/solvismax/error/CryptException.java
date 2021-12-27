package de.sgollmer.solvismax.error;

public class CryptException extends Exception {
	
	public enum Type {
		ERROR, DEFAULT,NONE
	}
	private final Type type;

	/**
	 * 
	 */
	private static final long serialVersionUID = 7353872631845585761L;

	CryptException(Type type) {
		super("Decryption error");
		this.type = type;
	}
	
	public CryptException() {
		this(Type.ERROR);
	}

	public Type getType() {
		return this.type;
	}

}
