package de.sgollmer.solvismax.model.objects;

import de.sgollmer.solvismax.imagepatternrecognition.image.MyImage;
import de.sgollmer.solvismax.objects.Field;

public class ScreenGrafic implements ScreenCompare {
	private final String id ;
	private final boolean exact ;
	private final Field field ;
	private MyImage image = null ;
	
	private ScreenGrafic( String id, boolean exact, Field field ) {
		this.id = id ;
		this.exact = exact ;
		this.field = field ;
	}

	/**
	 * @return the image
	 */
	public MyImage getImage() {
		return image;
	}

	/**
	 * @param image the image to set
	 */
	public void setImage(MyImage image) {
		this.image = image;
	}

	/**
	 * @return the id
	 */
	public String getId() {
		return id;
	}

	/**
	 * @return the exact
	 */
	public boolean isExact() {
		return exact;
	}

	/**
	 * @return the field
	 */
	public Field getField() {
		return field;
	}

	@Override
	public boolean isElementOf(MyImage image) {
		// TODO Auto-generated method stub
		return false;
	}
}
