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

	private int failCount = 0;

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
		private final boolean inhibitAppend;
		private final boolean insert;
		private final boolean same;
		
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

		public Handling(boolean inQueueInhibt, boolean inhibitAppend, boolean insert, boolean same) {
			this.inQueueInhibt = inQueueInhibt;
			this.inhibitAppend = inhibitAppend;
			this.insert = insert;
			this.same = same;
		}

		public boolean mustInhibitInQueue() {
			return this.inQueueInhibt;
		}

		public boolean isInhibitAppend() {
			return this.inhibitAppend;
		}

		public boolean mustInsert() {
			return this.insert;
		}

		public boolean isSame() {
			return this.same;
		}
		
	}

	public Handling getHandling(Command queueEntry, Solvis solvis) {
		return new Handling(false, false, false);
	}

	public boolean first() {
		return false;
	}

	public int getFailCount() {
		return this.failCount;
	}

	public void incrementFailCount() {
		++this.failCount;
	}
	
	public boolean isModbus(Solvis solvis) {
		return false ;
	}
	
	public boolean isWriting() {
		return false ;
	}
}
