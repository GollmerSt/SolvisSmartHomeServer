/************************************************************************
 * 
 * $Id$
 *
 * 
 ************************************************************************/

package de.sgollmer.solvismax.helper;

import de.sgollmer.solvismax.error.TerminationException;

public class AbortHelper {

	public static AbortHelper getInstance() {
		return AbortHelperHolder.INSTANCE;
	}

	private static class AbortHelperHolder {
		private static final AbortHelper INSTANCE = new AbortHelper();
	}

	private boolean abort = false;

	public synchronized void sleep(int time) throws TerminationException {
		if (this.abort) {
			throw new TerminationException();
		}
		try {
			this.wait(time);
		} catch (InterruptedException e) {
		}
		if (this.abort) {
			throw new TerminationException();
		}
	}

	public synchronized void abort() {
		this.abort = true;
		this.notifyAll();
	}

}
