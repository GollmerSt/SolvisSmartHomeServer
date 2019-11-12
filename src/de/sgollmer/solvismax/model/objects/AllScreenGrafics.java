package de.sgollmer.solvismax.model.objects;

import java.util.HashMap;
import java.util.Map;

public class AllScreenGrafics {
	private final Map< String, ScreenGrafic > screenGrafics = new HashMap<>() ;
	
	public void add( ScreenGrafic grafic ) {
		this.screenGrafics.put(grafic.getId(), grafic) ;
	}
	
	public ScreenGrafic get( String id ) {
		return this.screenGrafics.get(id) ;
	}
}
