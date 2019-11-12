package de.sgollmer.solvismax.model.objects;

public class Duration {
	private final String id ;
	private final int duration_ms ;
	
	public Duration( String id, int duration_ms ) {
		this.id = id ;
		this.duration_ms = duration_ms ;
	}

	/**
	 * @return the id
	 */
	public String getId() {
		return id;
	}

	/**
	 * @return the duration_ms
	 */
	public int getDuration_ms() {
		return duration_ms;
	}
}
