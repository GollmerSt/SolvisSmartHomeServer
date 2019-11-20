package de.sgollmer.solvismax.model.objects;

import de.sgollmer.solvismax.imagepatternrecognition.image.MyImage;
import de.sgollmer.solvismax.objects.Rectangle;

public class ScreenOcr implements ScreenCompare {
	private final Rectangle rectangle ;
	private final String value ;
	
	public ScreenOcr( Rectangle rectangle, String value ) {
		this.rectangle = rectangle ;
		this.value = value ;
	}

	/**
	 * @return the rectangle
	 */
	public Rectangle getRectangle() {
		return rectangle;
	}

	/**
	 * @return the value
	 */
	public String getValue() {
		return value;
	}

	@Override
	public boolean isElementOf(MyImage image) {
		// TODO Auto-generated method stub
		return false;
	}
}
