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
	private Collection<Object> syncObjects = new ArrayList<>();

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

	public void sleepAndLock(final int time, final Object syncObject) throws TerminationException {
		if (this.abort) {
			throw new TerminationException();
		}
		try {
			synchronized (syncObject) {

				synchronized (this) {
					this.syncObjects.add(syncObject);
				}

				if (!this.abort) {
					syncObject.wait(time);
				}

				synchronized (this) {
					this.syncObjects.remove(syncObject);
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
		Collection<Object> syncObjects;
		synchronized (this) {

			syncObjects = new ArrayList<>(this.syncObjects);
			this.notifyAll();
		}

		for (Object obj : syncObjects) {
			synchronized (obj) {
				obj.notifyAll();
			}
		}
	}

	public boolean isAbort() {
		return this.abort;
	}

}
