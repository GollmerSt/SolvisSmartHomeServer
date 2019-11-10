package de.sgollmer.solvismax.imagepatternrecognition.image;

public class Coordinate {
	private final int x ;
	private final int y ; 
	
	public Coordinate( int x, int y ) {
		this.x = x ;
		this.y = y ;
	}
	
	public Coordinate clone() {
		try {
			return (Coordinate) super.clone() ;
		} catch (CloneNotSupportedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return null ;
	}

	/**
	 * @return the x
	 */
	public int getX() {
		return x;
	}

	/**
	 * @return the y
	 */
	public int getY() {
		return y;
	}
	
	public Coordinate add( Coordinate coord ) {
		return new Coordinate( this.getX() + coord.getX(), this.getY() + coord.getY() ) ;
	}
	
	public Coordinate diff( Coordinate coord ) {
		return new Coordinate( this.getX() - coord.getX(), this.getY() - coord.getY() ) ;
	}
	
	public Coordinate div( int value ) {
		return new Coordinate( this.getX() / value, this.getY() / value ) ;
	}
	
	public Coordinate increment() {
		return new Coordinate( this.getX()+1, this.getY()+1 ) ;
	}
	
	public boolean equals(  Object coord ) {
		if ( coord == null ) {
			return false ;
		}
		if (!(coord instanceof Coordinate )) {
			return false ;
		}
		Coordinate cmp = (Coordinate) coord ;
		return this.getX() == cmp.getX() && this.getY() == cmp.getY() ;
	}
	
	public boolean approximately( Coordinate coord, int n ) {
		if ( coord == null ) {
			return false ;
		}
		
		int diffX = this.getX() - coord.getX() ;
		int diffY = this.getY() - coord.getY() ;
		diffX = diffX > 0 ? diffX : -diffX ;
		diffY = diffY > 0 ? diffY : -diffY ;
		
		return diffX <= n && diffY <= n ;
	}
	
	public String toString() {
		return "(" + this.x + "|" + this.y + ")" ;
	}
}
	