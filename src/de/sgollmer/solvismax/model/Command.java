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

	public abstract boolean execute(Solvis solvis) throws IOException, TerminationException, ErrorPowerOn;
	
	public abstract void notExecuted() ;

	public Screen getScreen(Solvis solvis) {
		return null;
	}

	public boolean isInhibit() {
		return false;
	}

	public void setInhibit(boolean inhibit) {
	};

	public static class Handling {
		private final boolean inQueueInhibt;
		private final boolean inhibitAdd;
		private final boolean insert;
		private final boolean mustFinished;
		
		/**
		 * 
		 * @param inQueueInhibit		True: The Execution of the command within the queue isn't necessary,
		 * 									  because the effect of new command is overwriting the effect of
		 * 									  the old one.
		 * @param inhibitAppend			True: New command is ignored, because he is redundant
		 * @param insert				True: The new command must inserted after queue command
		 */

		public Handling(boolean inQueueInhibit, boolean inhibitAppend, boolean insert) {
			this(inQueueInhibit, inhibitAppend, insert, false);
		}

		public Handling(boolean inQueueInhibt, boolean inhibitAdd, boolean insert, boolean mustFinished) {
			this.inQueueInhibt = inQueueInhibt;
			this.inhibitAdd = inhibitAdd;
			this.insert = insert;
			this.mustFinished = mustFinished;
		}

		public boolean mustInhibitInQueue() {
			return this.inQueueInhibt;
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
		
	}

	public Handling getHandling(Command queueEntry, Solvis solvis) {
		return new Handling(false, false, false);
	}

	public boolean first() {
		return false;
	}

	public boolean toEndOfQueue() {
		return false ;
	}

	public boolean isWriting() {
		return false ;
	}

	public boolean isModbus() {
		return false;
	}
}
