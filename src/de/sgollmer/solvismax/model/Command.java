/************************************************************************
 * 
 * $Id$
 *
 * 
 ************************************************************************/

package de.sgollmer.solvismax.model;

import java.io.IOException;
import java.util.Collection;

import de.sgollmer.solvismax.error.PowerOnException;
import de.sgollmer.solvismax.error.TerminationException;
import de.sgollmer.solvismax.error.TypeException;
import de.sgollmer.solvismax.error.XmlException;
import de.sgollmer.solvismax.model.objects.ChannelDescription;
import de.sgollmer.solvismax.model.objects.ResultStatus;
import de.sgollmer.solvismax.model.objects.screen.AbstractScreen;

public abstract class Command {

	/**
	 * Executes the command
	 * 
	 * @param solvis
	 * @return true if successfull
	 * @throws IOException
	 * @throws TerminationException
	 * @throws PowerOnException
	 * @throws TypeException
	 * @throws NumberFormatException
	 * @throws XmlException 
	 * @throws FieldException
	 * @throws ModbusException
	 */
	protected abstract ResultStatus execute(Solvis solvis)
			throws IOException, TerminationException, PowerOnException, NumberFormatException, TypeException, XmlException;

	protected abstract void notExecuted();

	/**
	 * Get the necessary screen for executing
	 * 
	 * @param solvis
	 * @return screen
	 */
	protected AbstractScreen getScreen(Solvis solvis) {
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
		/**
		 * True: The Execution of the command within the queue isn't necessary and was 
		 * inhibited, because
		 * the effect of new command is overwriting the effect of the old one.
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
		 *                       isn't necessary, because the effect of new command is
		 *                       overwriting the effect of the old one.
		 * @param inhibitAppend  True: New command is ignored, because he is redundant
		 * @param insert         True: The new command must inserted after queue command
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

		boolean isInhibitedInQueue() {
			return this.inQueueInhibited;
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

	protected Handling handle(Command queueEntry, Solvis solvis) {
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
	 * @param command
	 * @return true both commands write to the same channel
	 */
	protected boolean canBeIgnored(Command queueCommand) {
		return false;
	}

	ChannelDescription getRestoreChannel(Solvis solvis) {
		return null;
	}

	Collection<ChannelDescription> getReadChannels() {
		return null;
	}

	public boolean isDependencyPrepared() {
		return true;
	}

	public void setDependencyPrepared(boolean dependencyPrepared) {
	}
}
