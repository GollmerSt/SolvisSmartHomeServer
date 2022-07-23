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

		public synchronized void register(final IObserver<D> observer) {
			if (this.observers == null) {
				this.observers = new ArrayList<>();
			}
			this.observers.add(observer);
		}

		public synchronized void unregister(final IObserver<D> observer) {
			this.observers.remove(observer);
		}

		public void notify(final D data) {
			this.notify(data, null);
		}

		/**
		 * 
		 * @param data
		 * @param source
		 * @return true, if successfull
		 */
		public Collection<ObserverException> notify(final D data, final Object source) {
			Collection<ObserverException> exceptions = null;
			if (this.observers != null) {
				Collection<IObserver<D>> copy;
				synchronized (this) {
					copy = new ArrayList<>(this.observers);
				}
				for (IObserver<D> observer : copy) {
					try {
						observer.update(data, source);
					} catch (ObserverException e) {
						exceptions = this.exceptionAdd(e, exceptions);
					}
				}
			}
			return exceptions;
		}

		private Collection<ObserverException> exceptionAdd(final ObserverException exception,
				final Collection<ObserverException> exeptions) {
			Collection<ObserverException> exeptionCollection;
			if (exeptions == null) {
				exeptionCollection = new ArrayList<>();
			} else {
				exeptionCollection = exeptions;
			}
			if (exception.getExceptions() != null) {
				for (ObserverException e : exception.getExceptions()) {
					exeptionCollection = exceptionAdd(e, exeptionCollection);
				}
			} else {
				exeptionCollection = exceptionAdd(exception, exeptionCollection);
			}
			return exeptionCollection;
		}

		public synchronized boolean isEmpty() {
			return this.observers == null || this.observers.isEmpty();
		}

		public IObserver<D> getObserver(final IObserver<D> object) {
			if (isEmpty()) {
				return null;
			}
			synchronized (this) {
				for (IObserver<D> observer : this.observers) {
					if (observer.getClass() == object.getClass()) {
						return observer;
					}
				}
			}
			return null;
		}

	}

	public interface IObserver<D> {
		public void update(final D data, final Object source);
	}

}
