package de.sgollmer.solvismax.model.objects;

public enum ResultStatus {
	SUCCESS(true, true), // (Sub-)Command executes successful and is removeFromQueue
	CONTINUE(true, false), // a sub command removeFromQueue
	VALUE_VIOLATION(true, true), // (Sub-)Command executes successful but a value violation occurs
	INTERRUPTED(false, false), // (Sub-)Command is interrupted. An other command can be executed before
								// continuing
	NO_SUCCESS(false, false), // (Sub-)Command executes not successful
	INHIBITED(false, true); // The command was inhibited

	private final boolean executed;
	private final boolean removeFromQueue;

	private ResultStatus(final boolean executed, final boolean removeFromQueue) {
		this.executed = executed;
		this.removeFromQueue = removeFromQueue;
	}

	public boolean isExecuted() {
		return this.executed;
	}

	public boolean removeFromQueue() {
		return this.removeFromQueue;
	}
}