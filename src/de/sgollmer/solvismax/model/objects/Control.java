package de.sgollmer.solvismax.model.objects;

import de.sgollmer.solvismax.objects.Field;
import de.sgollmer.solvismax.objects.ReferenceError;

public class Control {
	private final String id ;
	private final String screenId ;
	private final Field current ;
	private final ControlType controlType ;
	
	private Screen screen = null ;
	
	public Control( String id, String screenId, Field current, ControlType controlType ) {
		this.id = id ;
		this.screenId = screenId ;
		this.current = current ;
		this.controlType = controlType ;
	}

	
	public void assign( AllScreens screens ) {
		this.screen = screens.get(screenId) ;
		if ( this.screen == null ) {
			throw new ReferenceError( "Screen reference < " + this.screenId + " > not found") ;
		}
	}
}
