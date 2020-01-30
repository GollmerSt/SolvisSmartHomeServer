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

public interface CommandI {
	public boolean execute(Solvis solvis) throws IOException, TerminationException, ErrorPowerOn;

	public Screen getScreen(Solvis solvis);

	public boolean isInhibit();

	public void setInhibit(boolean inhibit);
	
	public Boolean isScreenRestore() ;
	
	public static class Handling {
		private final boolean inQueueInhibt ;
		private final boolean inhibitAppend ;
		private final boolean insert ;
			
		public Handling(boolean inQueueInhibt, boolean inhibitAppend, boolean insert ) {
			this.inQueueInhibt = inQueueInhibt ;
			this.inhibitAppend = inhibitAppend ;
			this.insert = insert;
		}

		public boolean mustInhibitInQueue() {
			return inQueueInhibt;
		}

		public boolean isInhibitAppend() {
			return inhibitAppend;
		}
		
		public boolean mustInsert() {
			return insert ;
		}

	}
	
	public Handling getHandling( CommandI queueEntry, Solvis solvis  ) ;
	
	public boolean first() ;

}
