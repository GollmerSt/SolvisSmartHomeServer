package de.sgollmer.solvismax.model.objects;

import java.util.HashMap;
import java.util.Map;

import de.sgollmer.solvismax.imagepatternrecognition.image.MyImage;

public class AllScreens {
	private Map< String, Screen > screens = new HashMap<>() ;
	
	public void add( Screen screen ) {
		this.screens.put(screen.getId(), screen) ;
	}
	
	public Screen get( String id ) {
		return this.screens.get(id) ;
	}
	
	public Screen getScreen( MyImage image) {
		for ( Screen screen : screens.values()) {
			if ( screen.isScreen(image)) {
				return screen ;
			}
		}
		return null ;
	}
}
