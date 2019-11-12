package de.sgollmer.solvismax.model.objects;

import de.sgollmer.solvismax.imagepatternrecognition.image.MyImage;
import de.sgollmer.solvismax.objects.Field;

public class ScreenOcr implements ScreenCompare {
	private final Field field ;
	private final String value ;
	
	public ScreenOcr( Field field, String value ) {
		this.field = field ;
		this.value = value ;
	}

	/**
	 * @return the field
	 */
	public Field getField() {
		return field;
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
