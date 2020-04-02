/************************************************************************
 * 
 * $Id$
 *
 * 
 ************************************************************************/

package de.sgollmer.solvismax.imagepatternrecognition.image;

public class Maxima {
	private final int coord ;
	private final int value ;
	
	public Maxima( int x, int value ) {
		this.coord = x ;
		this.value = value ;
	}

	/**
	 * @return the coord
	 */
	public int getCoord() {
		return this.coord;
	}

	/**
	 * @return the value
	 */
	public int getValue() {
		return this.value;
	}
}

