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
import de.sgollmer.solvismax.model.objects.Screen;

public interface CommandI {
	public boolean execute(Solvis solvis) throws IOException, TerminationException, ErrorPowerOn;

	public Screen getScreen(Solvis solvis);

	public boolean isInhibit();

	public void setInhibit(boolean inhibit);
	
	public Boolean isScreenRestore() ;
	
	public static class Handling {
		public boolean isInQueueInhibt() {
			return inQueueInhibt;
		}

		public boolean isAppendInhibit() {
			return appendInhibit;
		}

		private final boolean inQueueInhibt ;
		private final boolean appendInhibit ;
			
		public Handling(boolean inQueueInhibt, boolean appendInhibit ) {
			this.inQueueInhibt = inQueueInhibt ;
			this.appendInhibit = appendInhibit ;
		}
	}
	
	public Handling getHandling( CommandI queueEntry ) ;
	
	public boolean first() ;

}
