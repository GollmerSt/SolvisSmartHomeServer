package de.sgollmer.solvismax.imagepatternrecognition.image;

public class Helper {

	public static int getBrightness( int rgb ) {
		return (rgb & 0xff) + (rgb >> 8 & 0xff ) + ( rgb >> 16 & 0xff) ;
	}
	
}
