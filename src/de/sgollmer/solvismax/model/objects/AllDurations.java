package de.sgollmer.solvismax.model.objects;

import java.util.HashMap;
import java.util.Map;

public class AllDurations {

	private Map< String, Duration > durations = new HashMap<>() ;
	
	public void add( Duration duration ) {
		this.durations.put(duration.getId(), duration ) ;
	}
	
	public Duration get( String id ) {
		return this.get(id) ;
	}
}
