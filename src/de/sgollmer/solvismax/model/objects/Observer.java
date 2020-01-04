/************************************************************************
 * 
 * $Id: 73_SolvisClient.pm 78 2020-01-03 17:50:08Z stefa $
 *
 * 
 ************************************************************************/

package de.sgollmer.solvismax.model.objects;

import java.util.ArrayList;
import java.util.Collection;

public class Observer<D> {

	public static class Observable<D> {
		private Collection<ObserverI<D>> observers = null;

		public synchronized void register(ObserverI<D> observer) {
			if (this.observers == null) {
				this.observers = new ArrayList<>();
			}
			this.observers.add(observer);
		}

		public synchronized void unregister(ObserverI<D> observer) {
			this.observers.remove(observer);
		}
		
		public void notify(D data) {
			this.notify(data, null);
		}

		public void notify(D data, Object source) {
			if (this.observers != null) {
				Collection<ObserverI<D>> copy;
				synchronized (this) {
					copy = new ArrayList<>(observers);
				}
				for (ObserverI<D> observer : copy) {
					observer.update(data, source);
				}
			}
		}
	}

	public interface ObserverI<D> {
		public void update(D data, Object source ) ;
	}

}
