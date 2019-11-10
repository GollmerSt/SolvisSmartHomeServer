package de.sgollmer.solvismax.ocr;

import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;

import javax.imageio.ImageIO;

public class Ocr {
	
	private static final int BLACK = 0 ;
	private static final int WHITE = 0xffffff ;
	
	private final BufferedImage image ;
	private List< Integer > histogramX ;
	private List< Integer > histogramY ;
	
	private Maxima [] maximaX = new Maxima[] { new Maxima(0, 0), new Maxima(0, 0) } ;
	private Maxima [] maximaY = new Maxima[] { new Maxima(0, 0), new Maxima(0, 0) };
	
	private Coordinate min  ;
	private int width = -1 ;
	private int height = -1 ;
	
	private static class Coordinate implements Cloneable {
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
	
	private static class Maxima {
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
			return coord;
		}

		/**
		 * @return the value
		 */
		public int getValue() {
			return value;
		}
	}
	
	
	public Ocr( BufferedImage image ) {
		this.image = image ;
		this.histogramX = new ArrayList< Integer >( image.getWidth() ) ;
		this.histogramY = new ArrayList< Integer >( image.getHeight() ) ;
		
		for ( int x = 0 ; x < image.getWidth() ; ++x ) {
			histogramX.add(0) ;
		}
		
		for ( int y = 0 ; y < image.getHeight() ; ++y ) {
			histogramY.add(0) ;
		}
		
		this.processing();
	}
	
	private static class BaseImageData {
		private final int averageBrightness ;
		private final int treshold ;
		
		public BaseImageData( BufferedImage image ) {
			
			int minBrightness = 256 * 3 ;
			int maxBrightness = 0 ;
			long sum = 0 ;
			
			for ( int x = 0 ; x < image.getWidth(); ++x ) {
				for ( int y = 0 ; y < image.getHeight(); ++y ) {
					int rgb = image.getRGB(x, y) ; 
					int brightness = getBrightness(rgb) ;
					if ( minBrightness > brightness ) {
						minBrightness = brightness ;
					}
					if ( maxBrightness < brightness ) {
						maxBrightness = brightness ;
					}
					sum += brightness ;   
				}
			}
			
			this.treshold = ( minBrightness + maxBrightness ) / 2 ;
			this.averageBrightness = (int) (sum / ( image.getWidth() * image.getHeight() )) ;
		}

		/**
		 * @return the averageBrightness
		 */
		public int getAverageBrightness() {
			return averageBrightness;
		}

		/**
		 * @return the treshold
		 */
		public int getTreshold() {
			return treshold;
		}
	}
	
	private final void processing() {
		
		BaseImageData imageData = new BaseImageData(image) ;
		
		int toRgbHigherValue ;
		int toRgbLowervalue ;
		int cntHigherValue ;
		int cntLowerValue ;
		
		if ( imageData.getTreshold() < imageData.getAverageBrightness() ) {
			toRgbHigherValue = WHITE ;
			toRgbLowervalue = BLACK ;
			cntHigherValue = 0 ;
			cntLowerValue = 1 ;
		} else {
			toRgbHigherValue = BLACK ;
			toRgbLowervalue = WHITE ;
			cntHigherValue = 1 ;
			cntLowerValue = 0 ;
		}
		
		for ( int x = 0 ; x < image.getWidth(); ++x ) {
			for ( int y = 0 ; y < image.getHeight(); ++y ) {
				int rgb = image.getRGB(x, y) ; 
				int brightness = getBrightness(rgb) ;
				if ( brightness > imageData.getTreshold() ) {
					this.image.setRGB(x, y, toRgbHigherValue );
					this.histogramX.set(x, cntHigherValue + this.histogramX.get(x) ) ;
					this.histogramY.set(y, cntHigherValue + this.histogramY.get(y) ) ;
				} else {
					this.image.setRGB(x, y, toRgbLowervalue );
					this.histogramX.set(x, cntLowerValue + this.histogramX.get(x) ) ;
					this.histogramY.set(y, cntLowerValue + this.histogramY.get(y) ) ;
				}
			}
		}
		
		int minX = 0 ;
		int minY = 0 ;
		
		for ( int x = 0 ; x < this.histogramX.size() ; ++x ) {
			int cnt = this.histogramX.get(x) ;
			if ( cnt > 0 ) {
				minX = x ;
				break ;
			}
		}
		
		for ( int x = this.histogramX.size()-1 ; x >= 0 ; --x ) {
			int cnt = this.histogramX.get(x) ;
			if ( cnt > 0 ) {
				this.width = x ;
				break ;
			}
		}
		
		for ( int y = 0 ; y < this.histogramY.size() ; ++y ) {
			int cnt = this.histogramY.get(y) ;
			if ( cnt > 0 ) {
				minY = y ;
				break ;
			}
		}
		
		for ( int y = this.histogramY.size()-1 ; y >= 0 ; --y ) {
			int cnt = this.histogramY.get(y) ;
			if ( cnt > 0 ) {
				this.height = y ;
				break ;
			}
		}
		
		this.histogramX = this.histogramX.subList(minX, this.width + 1) ;
		this.histogramY = this.histogramY.subList(minY, this.height + 1) ;
		
		this.width += 1 - minX ;
		this.height += 1 - minY ;
		this.min = new Coordinate(minX, minY) ;
						
		this.createMaxima() ;
	}
	
	private static int getBrightness( int rgb ) {
		return (rgb & 0xff) + (rgb >> 8 & 0xff ) + ( rgb >> 16 & 0xff) ;
	}
	
	public int getRGB( int x, int y ) {
		if ( this.isIn(x, y)) {
			return this.image.getRGB( x + this.min.getX(), y + this.min.getY() ) ;
		} else {
			return WHITE ;
		}
	}
	
	
	public int getRGB( Coordinate coord ) {
		if ( this.isIn(coord)) {
			return this.image.getRGB( coord.getX() + this.min.getX(), coord.getY() + this.min.getY() ) ;
		} else {
			return WHITE ;
		}
	}
	
	public void setRGB( int x, int y, int rgb ) {
		this.image.setRGB( x + this.min.getX(), y + this.min.getY(), rgb);
	}

	/**
	 * @return the width
	 */
	public int getWidth() {
		return width;
	}

	/**
	 * @return the height
	 */
	public int getHeight() {
		return height;
	}
	
	public boolean isIn( int x, int y ) {
		return x < getWidth() && y < getHeight() && x >= 0 && y >= 0 ;
	}
	
	public boolean isIn( Coordinate coord ) {
		return coord.getX() < getWidth() && coord.getY() < getHeight() && coord.getX() >= 0 && coord.getY() >= 0 ;
	}
	
	
	private void createMaxima() {
		for ( int x = 0 ; x < this.histogramX.size() ; ++x ) {
			int value = this.histogramX.get(x) ;
			for ( int m = 0 ; m < 2 ; ++m ) {
				int cmpCoord = this.maximaX[ m ].getCoord() ;
				int cmpValue = this.maximaX[ m ].getValue() ;
				if ( cmpValue == value && cmpCoord == x-1 ) {
					break ;
				}
				if ( cmpValue <= value ) {
					this.maximaX[m] = new Maxima(x, value) ;
					break ;
				}
			}
		}

		for ( int y = 0 ; y < this.histogramY.size() ; ++y ) {
			int value = this.histogramY.get(y) ;
			for ( int m = 0 ; m < 2 ; ++m ) {
				int cmpCoord = this.maximaY[ m ].getCoord() ;
				int cmpValue = this.maximaY[ m ].getValue() ;
				if ( cmpValue == value && cmpCoord == y-1 ) {
					break ;
				}
				if ( cmpValue <= value ) {
					this.maximaY[m] = new Maxima(y, value) ;
					break ;
				}
			}
		}

	}
	
	
	
	private class BlackWhite {
		
		private Coordinate black ;
		private Coordinate white ;
				
		public BlackWhite( Coordinate black, Coordinate white ) {
			this.black = black ;
			this.white = white ;
		}
				
		public BlackWhite next() {
			Coordinate diff = this.white.diff( this.black ) ;
			diff = new Coordinate(-diff.getY(), diff.getX()) ;
			BlackWhite newBlackWhite = new BlackWhite(this.black.add(diff), this.white.add(diff)) ;
			int checkWhite = Ocr.this.getRGB(newBlackWhite.white) ;
			int checkBlack = Ocr.this.getRGB(newBlackWhite.black) ;
			
			if ( checkBlack == BLACK && checkWhite == WHITE ) {
				return newBlackWhite ;
			} else if ( checkBlack == WHITE && checkWhite == WHITE ) {
				return new BlackWhite(this.black, newBlackWhite.black) ;
			} else {
				return new BlackWhite(newBlackWhite.white, this.white) ;
			}
		}
		
		public boolean equals( Object obj ) {
			if ( obj == null ) {
				return false ;
			} else if ( ! ( obj instanceof BlackWhite ) ) {
				return false ;
			} else {
				BlackWhite bw = (BlackWhite) obj ; 
				return this.white.equals(bw.white) && this.black.equals(bw.black) ;
			}
		}
	}
	
	private Coordinate getClosedStructure(Coordinate coord) {
		
		if ( this.getRGB(coord) == BLACK ) {
			return null ;
		}
		
		boolean found = false ;

		int startX ;
		for ( startX = coord.getX() ; startX < this.getWidth() ; ++startX ) {
			if ( this.getRGB( startX, coord.getY() ) == BLACK ) {
				found = true ;
				break ;
			}
		}
		
		if ( !found ) {
			return null ;
		}
		
		Coordinate black = new Coordinate(startX, coord.getY()) ;
		Coordinate white = new Coordinate(startX-1, coord.getY()) ;
		
		BlackWhite start = new BlackWhite(black, white) ;
		
		Coordinate returnValue = white ;
		int cnt = 0 ;
		
		boolean cont = true ;
		
		BlackWhite current = start ;
		
		while( cont ) {
			BlackWhite next = current.next() ;
			if ( next == null || ! this.isIn( next.white ) ) {
				return null ;
			}
			returnValue = returnValue.add(next.white) ;
			++cnt ;
			cont = ! start.equals(next) ;
			current = next ;
		}
		
		return returnValue.div(cnt) ;
	}
	
	
	public char toChar() {
		// - Erkennung von 4, geschlossene Struktur obere Hälfte, waagerechtes Maximum 3/4 * Breite, nahe Mitte
		
		int y = this.maximaY[0].getCoord() ;
		int value = this.maximaY[0].getValue() ;
		
		if ( value > this.getWidth() * 7 / 8 && this.getHeight() / 3 < y && y < this.getHeight() * 3 / 4 ) {
			
			int x = this.maximaX[0].getCoord() ;
			
			while ( this.getRGB(x--,y--) == BLACK ) ; 
			
			Coordinate coord = new Coordinate(x, y) ;
			
			if ( this.isIn( coord ) && this.getClosedStructure( coord ) != null ) {
				return '4' ;
			}
		}
		

		Coordinate upperClosedStructure = this.getClosedStructure(new Coordinate(this.getWidth() / 2 , this.getHeight() / 4 )) ;
		Coordinate lowerClosedStructure = this.getClosedStructure(new Coordinate(this.getWidth() / 2 , this.getHeight() *3 / 4 )) ;
		
		if ( upperClosedStructure != null && lowerClosedStructure != null ) {
			if ( upperClosedStructure.approximately(lowerClosedStructure,2) ) {
				//- Erkennung von 0, geschlossene Struktur nahe Mitte
				return '0' ;
			} else {
				//- Erkennung von 8, zwei geschlossene Strukturen
				return '8' ;
			}
		} else if ( upperClosedStructure != null ) {
			//- Erkennung von 9, geschlossene Struktur obere Hälfte
			return '9' ;
		} else if ( lowerClosedStructure != null ) {
			//- Erkennung von 6, geschlossene Struktur untere Hälfte
			return '6' ;
		}
		
		if ( this.maximaY[0].getValue() == this.getWidth() && this.maximaX[0].getValue() < this.getWidth() / 2 ) {
			//- Erkennung von -, waagerechtes Maximum = Breite, senkrechtes Maximum < 1/2 Breite
			return '-' ;
		}
		
		if ( this.maximaY[0].getCoord() >= this.getHeight() - 2 && this.maximaY[0].getValue() > this.getWidth() * 5 / 6 ) {
			//- Erkennung von 2, waagerechtes Maximum unten
			return '2' ;
		}

		if ( this.maximaX[0].getCoord() >= this.getWidth() - 2 && this.maximaX[0].getValue() > this.getHeight() * 3 / 4 ) {
			//- Erkennung von 1, senkrechtes Maximum = Höhe, rechts
			return '1' ;
		}
		
		if ( this.maximaY[0].getValue() == this.getWidth() && this.maximaX[0].getValue() < this.getWidth() / 2 ) {
			//- Erkennung von -, waagerechtes Maximum = Breite, senkrechtes Maximum < 1/2 Breite
			return '-' ;
		}
		
		if ( this.maximaY[0].getValue() == this.getWidth() && this.maximaX[0].getValue() == this.getHeight() ) {
			//- Erkennung von +, senkrechtes Maximum = Höhe, waagerechtes maximum = Breite
			return '+' ;
		}

		if ( this.maximaY[0].getValue() == this.getWidth() ) {
			//- Erkennung von 7, waagerechtes Maximum = 1, oben
			return '7' ;
		}

		if ( this.histogramY.get(0) > this.getWidth() * 2 / 3 ) {
			//- Erkennung von 5, Waagerechtes Maximum oben, 2. Maximum ausgeprägt Mitte
			return '5' ;
		}

		if ( this.maximaX[0].getCoord() > this.getWidth() /2 && this.maximaX[1].getCoord() > this.getWidth() /2 ) {
			//- Erkennung von 3, Senkrechtes Maximum rechte Hälfte, 2. Maximum ebenfalls rechte Hälfte
			return '3' ;
		}
				
		return 0x00 ;
	}
	
	public static void main( String [] args ) {
		
		File parent = new File("E:\\Eigene Dateien\\Programmierung\\Java\\Workspace_Stefan\\SolvisMax\\src\\de\\sgollmer\\solvismax\\dokus\\images") ;
		
		Collection< String > names = Arrays.asList( "0.png", "1.png", "2.png", "3.png", "4.png", "4 black.png", "5.png", "6.png", "7.png", "8.png", "9.png", "9 grey small.png", "minus.png", "plus.png" ) ;  
		
		BufferedImage image = null ;
		
		for ( Iterator<String> it = names.iterator() ; it.hasNext() ; ) {
			File file = new File(parent, it.next() ) ;
			try {
				image = ImageIO.read(file) ;
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			
			Ocr ocr = new Ocr(image) ;
			
			char c = ocr.toChar() ;
			
			System.out.println( "Character is: " + c) ;
		}
		
		
	}
	
}
