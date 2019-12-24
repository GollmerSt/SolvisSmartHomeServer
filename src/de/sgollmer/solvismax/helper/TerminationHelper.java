package de.sgollmer.solvismax.helper;

import de.sgollmer.solvismax.error.TerminationException;

public class TerminationHelper {
	
    public static TerminationHelper getInstance() {
		return TerminationHelperHolder.INSTANCE;
	}

	private static class TerminationHelperHolder {
		private static final TerminationHelper INSTANCE = new TerminationHelper();
	}
	
	private boolean terminate = false ;
	
	public  synchronized void sleep( int time ) throws TerminationException {
		if ( terminate ) {
			throw new TerminationException();
		}
		try {
			this.wait(time) ;
		} catch (InterruptedException e) {
		}
		if ( terminate ) {
			throw new TerminationException();
		}
	}
	
	public synchronized void terminate() {
		this.terminate = true ;
		this.notifyAll();
	}
}
