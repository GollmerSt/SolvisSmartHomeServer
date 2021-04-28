package de.sgollmer.solvismax.error;

public class CommandError extends Exception {
	/**
	 * 
	 */
	private static final long serialVersionUID = 5669324815229600013L;

	public CommandError(String message) {
		super(message);
	}
}
