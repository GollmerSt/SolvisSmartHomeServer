/************************************************************************
 * 
 * $Id$
 *
 * 
 ************************************************************************/

package de.sgollmer.solvismax.model.objects;

import java.util.ArrayList;
import java.util.Collection;

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

		public void notify(D data, Object source) {
			if (this.observers != null) {
				Collection<IObserver<D>> copy;
				synchronized (this) {
					copy = new ArrayList<>(this.observers);
				}
				for (IObserver<D> observer : copy) {
					observer.update(data, source);
				}
			}
		}
	}

	public interface IObserver<D> {
		public void update(D data, Object source);
	}

	public interface IObserverableError {
		public void setException(Exception e);
	}

}
