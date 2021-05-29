/************************************************************************
 * 
 * $Id$
 *
 * 
 ************************************************************************/

package de.sgollmer.solvismax.helper;

public class ImageHelper {

	public static int getBrightness(final int rgb) {
		return (rgb & 0xff) + (rgb >> 8 & 0xff) + (rgb >> 16 & 0xff);
	}

}
