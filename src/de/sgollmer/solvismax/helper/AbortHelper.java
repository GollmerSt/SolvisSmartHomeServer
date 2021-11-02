/************************************************************************
 * 
 * $Id$
 *
 * 
 ************************************************************************/

package de.sgollmer.solvismax.helper;

import java.util.ArrayList;
import java.util.Collection;

import de.sgollmer.solvismax.error.TerminationException;

public class AbortHelper {

	public static AbortHelper getInstance() {
		return AbortHelperHolder.INSTANCE;
	}

	private static class AbortHelperHolder {
		private static final AbortHelper INSTANCE = new AbortHelper();
	}

	private boolean abort = false;
	private Collection<Abortable> abortables = new ArrayList<>();

	public synchronized void sleep(final Integer time) throws TerminationException {
		if ( time == null || time <= 0 ) {
			return;
		}
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
	
	public interface Abortable {
		public void abort() ;
	}

	public void sleepAndLock(final int time, final Abortable abortable) throws TerminationException {
		if (this.abort) {
			throw new TerminationException();
		}
		try {
			synchronized (abortable) {

				synchronized (this) {
					this.abortables.add(abortable);
				}

				if (!this.abort) {
					abortable.wait(time);
				}

				synchronized (this) {
					this.abortables.remove(abortable);
				}
			}
		} catch (InterruptedException e) {
		}
		if (this.abort) {
			throw new TerminationException();
		}
	}

	public void abort() {
		this.abort = true;
		Collection<Abortable> abortables;
		synchronized (this) {

			abortables = new ArrayList<>(this.abortables);
			this.notifyAll();
		}

		for (Abortable abortable : abortables) {
			synchronized (abortable) {
				abortable.abort();
			}
		}
	}

	public boolean isAbort() {
		return this.abort;
	}

}
