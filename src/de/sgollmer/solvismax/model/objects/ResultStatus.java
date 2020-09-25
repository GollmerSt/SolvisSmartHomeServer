package de.sgollmer.solvismax.model.objects;

public enum ResultStatus {
	SUCCESS(true), CONTINUE(true), VALUE_VIOLATION(true), INTERRUPTED(false), NO_SUCCESS(false);
	
	private final boolean executed;
	
	private ResultStatus(boolean executed) {
		this.executed = executed;
	}

	public boolean isExecuted() {
		return this.executed;
	}
}