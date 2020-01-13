package de.sgollmer.solvismax.helper;

public class Reference< T> {
	private T object ;
	
	public Reference( T object) {
		this.object = object ;
	}
	
	public Reference() {
		this.object = null ;
	}
	
	public T get() {
		return object ;
	}
	
	public void set( T object ) {
		this.object = object ;
	}
}
