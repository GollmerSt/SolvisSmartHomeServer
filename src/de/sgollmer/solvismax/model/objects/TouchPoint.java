package de.sgollmer.solvismax.model.objects;

import de.sgollmer.solvismax.objects.Coordinate;

public class TouchPoint {
	private final Coordinate coordinate ;
	private final String pushTimeId;
	private final String releaseTimeId;
	private Duration pushTime = null ;
	private Duration releaseTime = null ;
	
	public TouchPoint( Coordinate coordinate, String pushTimeId, String releaseTimeId ) {
		this.coordinate = coordinate ;
		this.pushTimeId = pushTimeId ;
		this.releaseTimeId = releaseTimeId ;
	}
}
