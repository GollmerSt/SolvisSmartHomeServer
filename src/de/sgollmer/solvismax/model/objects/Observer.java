package de.sgollmer.solvismax.model.objects;

import java.util.ArrayList;
import java.util.Collection;

public class Observer< D > {
	
	public static class Observable<D> {
		private final Collection< ObserverI<D> > observers = new ArrayList<>() ;
		
		public synchronized void register( ObserverI<D> observer ) {
			this.observers.add(observer) ;
		}
		
		public synchronized void unregister( ObserverI<D> observer ) {
			this.observers.remove(observer) ;
		}
		
		public void notify( D data ) {
			Collection< ObserverI<D> > copy ;
			synchronized ( this) {
				copy = new ArrayList<>( observers ) ;
			}
			for ( ObserverI<D> observer : copy ) {
				observer.update(data);
			}
		}
	}
	
	public interface ObserverI<D> {
		public void update( D data ) ;
	}

}