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

	public void sleepAndLock(int time, Object syncObject) throws TerminationException {
		if (this.abort) {
			throw new TerminationException();
		}
		try {
			synchronized (this) {
				this.syncObjects.add(syncObject);
			}
			synchronized (syncObject) {
				syncObject.wait(time);
			}
			synchronized (this) {
				this.syncObjects.remove(syncObject);
			}
		} catch (InterruptedException e) {
		}
		if (this.abort) {
			throw new TerminationException();
		}
	}

	public synchronized void abort() {
		this.abort = true;
		this.notifyAll();
		for (Object obj : this.syncObjects) {
			obj.notifyAll();
		}
	}

	public boolean isAbort() {
		return this.abort;
	}

}
