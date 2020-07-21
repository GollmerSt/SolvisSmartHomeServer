/************************************************************************
 * 
 * $Id$
 *
 * 
 ************************************************************************/

package de.sgollmer.solvismax.model;

import java.io.IOException;

import de.sgollmer.solvismax.error.ErrorPowerOn;
import de.sgollmer.solvismax.error.TerminationException;
import de.sgollmer.solvismax.model.objects.screen.Screen;

public abstract class Command {

	/**
	 * Executes the command
	 * 
	 * @param solvis
	 * @return true if successfull
	 * @throws IOException
	 * @throws TerminationException
	 * @throws ErrorPowerOn
	 */
	protected abstract boolean execute(Solvis solvis) throws IOException, TerminationException, ErrorPowerOn;

	protected abstract void notExecuted();

	/**
	 * Get the necessary screen for executing
	 * 
	 * @param solvis
	 * @return screen
	 */
	protected Screen getScreen(Solvis solvis) {
		return null;
	}

	/**
	 * 
	 * @return true if command is inhibited
	 */
	protected boolean isInhibit() {
		return false;
	}

	/**
	 * Set the inhibit status
	 * 
	 * @param inhibit true if the command should be inhibited
	 */

	protected void setInhibit(boolean inhibit) {
	};

	public static class Handling {
		private final boolean inQueueInhibt;
		private final boolean inhibitAdd;
		private final boolean insert;
		private final boolean mustFinished;

		/**
		 * 
		 * @param inQueueInhibit True: The Execution of the command within the queue
		 *                       isn't necessary, because the effect of new command is
		 *                       overwriting the effect of the old one.
		 * @param inhibitAppend  True: New command is ignored, because he is redundant
		 * @param insert         True: The new command must inserted after queue command
		 */

		public Handling(boolean inQueueInhibit, boolean inhibitAppend, boolean insert) {
			this(inQueueInhibit, inhibitAppend, insert, false);
		}

		Handling(boolean inQueueInhibt, boolean inhibitAdd, boolean insert, boolean mustFinished) {
			this.inQueueInhibt = inQueueInhibt;
			this.inhibitAdd = inhibitAdd;
			this.insert = insert;
			this.mustFinished = mustFinished;
		}

		boolean mustInhibitInQueue() {
			return this.inQueueInhibt;
		}

		boolean isInhibitAdd() {
			return this.inhibitAdd;
		}

		boolean mustInsert() {
			return this.insert;
		}

		boolean mustFinished() {
			return this.mustFinished;
		}

	}

	protected Handling getHandling(Command queueEntry, Solvis solvis) {
		return new Handling(false, false, false);
	}

	/**
	 * 
	 * @return true if the command should be at the first position of the queue
	 */
	public boolean first() {
		return false;
	}

	/**
	 * 
	 * @return true if the command should be moved to the end of the queue
	 */
	protected boolean toEndOfQueue() {
		return false;
	}

	/**
	 * 
	 * @return true if the command is changed a solvis parameter
	 */
	protected boolean isWriting() {
		return false;
	}

	/**
	 * 
	 * @return true if access channel is a modbus channel
	 */
	protected boolean isModbus() {
		return false;
	}

	/**
	 * 
	 * @param command
	 * @return true both commands write to the same channel
	 */
	protected boolean canBeIgnored(Command queueCommand) {
		return false;
	}
}
