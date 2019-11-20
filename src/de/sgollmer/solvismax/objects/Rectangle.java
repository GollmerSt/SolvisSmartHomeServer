package de.sgollmer.solvismax.objects;

public class Rectangle {
	private final Coordinate topLeft ;
	private final Coordinate bottomRight ;
	
	public Rectangle( Coordinate topLeft, Coordinate bottomRight ) {
		this.topLeft = topLeft ;
		this.bottomRight = bottomRight ;
	}

	/**
	 * @return the topLeft
	 */
	public Coordinate getTopLeft() {
		return topLeft;
	}

	/**
	 * @return the bottomRight
	 */
	public Coordinate getBottomRight() {
		return bottomRight;
	}
}
