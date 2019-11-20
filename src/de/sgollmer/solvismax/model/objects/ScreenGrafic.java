package de.sgollmer.solvismax.model.objects;

import de.sgollmer.solvismax.imagepatternrecognition.image.MyImage;
import de.sgollmer.solvismax.objects.Rectangle;

public class ScreenGrafic implements ScreenCompare {
	private final String id ;
	private final boolean exact ;
	private final Rectangle rectangle ;
	private MyImage image = null ;
	
	private ScreenGrafic( String id, boolean exact, Rectangle rectangle ) {
		this.id = id ;
		this.exact = exact ;
		this.rectangle = rectangle ;
	}

	@Override
	public boolean isElementOf(MyImage image) {
		// TODO Auto-generated method stub
		return false;
	}

	/**
	 * @return the exact
	 */
	public boolean isExact() {
		return exact;
	}

	/**
	 * @return the id
	 */
	public String getId() {
		return id;
	}
}
