/************************************************************************
 * 
 * $Id$
 *
 * 
 ************************************************************************/

package de.sgollmer.solvismax.model.objects;

import java.util.ArrayList;
import java.util.Collection;

import de.sgollmer.solvismax.error.ObserverException;

public class Observer<D> {

	public static class Observable<D> {
		private Collection<IObserver<D>> observers = null;

		public synchronized void register(IObserver<D> observer) {
			if (this.observers == null) {
				this.observers = new ArrayList<>();
			}
			this.observers.add(observer);
		}

		public synchronized void unregister(IObserver<D> observer) {
			this.observers.remove(observer);
		}

		public void notify(D data) {
			this.notify(data, null);
		}

		/**
		 * 
		 * @param data
		 * @param source
		 * @return true, if successfull
		 */
		public boolean notify(D data, Object source) {
			boolean status = true;
			if (this.observers != null) {
				Collection<IObserver<D>> copy;
				synchronized (this) {
					copy = new ArrayList<>(this.observers);
				}
				for (IObserver<D> observer : copy) {
					try {
						observer.update(data, source);
					} catch (ObserverException e) {
						status = false;
					}
				}
			}
			return status;
		}
		
		public synchronized boolean isEmpty() {
			return this.observers == null || this.observers.isEmpty();
		}
		
		public IObserver<D> getObserver( IObserver<D> object) {
			if ( isEmpty() ) {
				return null;
			}
			synchronized (this) {
				for (IObserver<D> observer : this.observers) {
					if (observer.getClass() == object.getClass() ) {
						return observer;
					}
				}
			}
			return null;
		}

	}

	public interface IObserver<D> {
		public void update(D data, Object source);
	}

}
