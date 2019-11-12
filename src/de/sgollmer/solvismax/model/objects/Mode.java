package de.sgollmer.solvismax.model.objects;

public class Mode {
	private final String id ;
	private final TouchPoint touch ;
	private final ScreenGrafic grafic ;
	
	public Mode( String id, TouchPoint touch, ScreenGrafic grafic ) {
		this.id = id ;
		this.touch = touch ;
		this.grafic = grafic ;
	}
}
