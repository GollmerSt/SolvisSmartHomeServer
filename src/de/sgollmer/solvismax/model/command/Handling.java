package de.sgollmer.solvismax.model.command;

import java.util.HashMap;
import java.util.Map;

public class Handling {
	/**
	 * True: The Execution of the command within the queue isn't necessary and was
	 * inhibited, because the effect of new command is overwriting the effect of the
	 * old one.
	 */
	private final boolean inQueueInhibited;
	/**
	 * True: New command is ignored, because he is redundant
	 */
	private final boolean inhibitAdd;
	/**
	 * True: The new command must inserted after queue command
	 */
	private final boolean insert;
	/**
	 * True: no previous entries are of interest
	 */
	private final boolean mustFinished;

	/**
	 * 
	 * @param inQueueInhibited True: The Execution of the command within the queue
	 *                         isn't necessary, because the effect of new command is
	 *                         overwriting the effect of the old one.
	 * @param inhibitAppend    True: New command is ignored, because he is redundant
	 * @param insert           True: The new command must inserted after queue
	 *                         command
	 */

	public Handling(boolean inQueueInhibited, boolean inhibitAppend, boolean insert) {
		this(inQueueInhibited, inhibitAppend, insert, false);
	}

	/**
	 * 
	 * @param inQueueInhibit True: The Execution of the command within the queue
	 *                       isn't necessary, because the effect of new command is
	 *                       overwriting the effect of the old one.
	 * @param inhibitAppend  True: New command is ignored, because he is redundant
	 * @param insert         True: The new command must inserted after queue command
	 * @param mustFinished   True: no previous entries are of interest
	 */

	Handling(boolean inQueueInhibited, boolean inhibitAdd, boolean insert, boolean mustFinished) {
		this.inQueueInhibited = inQueueInhibited;
		this.inhibitAdd = inhibitAdd;
		this.insert = insert;
		this.mustFinished = mustFinished;
	}

	public boolean isInhibitedInQueue() {
		return this.inQueueInhibited;
	}

	public boolean isInhibitAdd() {
		return this.inhibitAdd;
	}

	public boolean mustInsert() {
		return this.insert;
	}

	public boolean mustFinished() {
		return this.mustFinished;
	}

	public static class QueueStatus {
		private Integer currentPriority;

		private Map<Object, Integer> priorities = new HashMap<>();

		public synchronized Integer getCurrentPriority() {
			return this.currentPriority;
		}

		public synchronized void setCurrentPriority(final Integer priority, final Object setObject) {

			if (priority == null) {
				this.priorities.remove(setObject);
			} else {
				this.priorities.put(setObject, priority);
			}

			if (this.priorities.isEmpty()) {
				this.currentPriority = null;
			} else {
				this.currentPriority = 0;

				for (int i : this.priorities.values()) {
					if ( i > this.currentPriority ) {
						this.currentPriority = i;
					}
				}
			}
		}

	}
}