package de.sgollmer.solvismax.model.objects;

public class Mode implements Assigner {
	private final String id ;
	private final TouchPoint touch ;
	private final ScreenGrafic grafic ;
	
	public Mode( String id, TouchPoint touch, ScreenGrafic grafic ) {
		this.id = id ;
		this.touch = touch ;
		this.grafic = grafic ;
	}

	/**
	 * @return the grafic
	 */
	public ScreenGrafic getGrafic() {
		return grafic;
	}

	/**
	 * @return the id
	 */
	public String getId() {
		return id;
	}

	/**
	 * @return the touch
	 */
	public TouchPoint getTouch() {
		return touch;
	}

	@Override
	public void assign(SolvisDescription description) {
		this.touch.assign(description);
		
	}
}
