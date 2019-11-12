package de.sgollmer.solvismax.imagepatternrecognition.image;

import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.List;

public class MyImage {

	public static final int BLACK = 0;
	public static final int WHITE = 0xffffffff;

	private BufferedImage image;
	private Coordinate min = null;
	private Coordinate maxRel = null; // Zeigt auf 1. Pixel auﬂerhalb, relativ
										// zu min

	private List<Integer> histogramX = null;
	private List<Integer> histogramY = null;
	
	private boolean blackWhite ;
	private boolean autoInvert ;

	public MyImage(BufferedImage image) {
		this.image = image;
		this.min = new Coordinate(0, 0);
		this.maxRel = new Coordinate(image.getWidth(), image.getHeight());
		this.blackWhite = false ;
		this.autoInvert = false ;
	}

	public MyImage(MyImage image) {
		this.image = image.image;
		this.min = image.min;
		this.maxRel = image.maxRel;
		if (image.histogramX != null) {
			this.histogramX = new ArrayList<>(image.histogramX) ;
		}
		if (image.histogramY != null) {
			this.histogramY = new ArrayList<>(image.histogramY) ;
		}
		this.blackWhite = image.blackWhite ;
		this.autoInvert = image.autoInvert ;

	}
	
	public MyImage(MyImage image, Coordinate topLeft, Coordinate bottomRight ) {
		this( image ) ;
		if ( ! this.isIn(topLeft) || ! this.isIn(bottomRight)) {
			throw new ErrorNotInRange("UpperLeft or lowerRigh not within the image") ;
		}
		if ( bottomRight.getX() < topLeft.getX() || bottomRight.getY() < topLeft.getY() ) {
			throw new ErrorNotInRange("Upper left is not upper left") ;
		}
		this.maxRel = bottomRight.diff(topLeft).increment() ;
		this.min = this.min.add(topLeft) ;
		this.blackWhite = image.blackWhite ;
		if ( this.blackWhite ) {
			this.createHistograms();
		}
		this.autoInvert = false ;
	}

	public int getRGB(Coordinate coord) {
		return this.getRGB(coord.getX(), coord.getY());
	}

	public boolean isIn(Coordinate coord) {
		return coord.getX() >= 0 && coord.getY() >= 0 && coord.getX() < maxRel.getX() && coord.getY() < maxRel.getY();
	}

	public boolean isIn(int x, int y) {
		return x >= 0 && y >= 0 && x < maxRel.getX() && y < maxRel.getY();
	}

	public int getRGB(int x, int y) {
		if (this.isIn(x, y)) {
			return image.getRGB(x + min.getX(), y + this.min.getY());
		} else {
			return WHITE;
		}
	}

	public void setRGB(int x, int y, int rgb) {
		if (this.isIn(x, y)) {
			this.image.setRGB(x + this.min.getX(), y + this.min.getY(), rgb);
		}
	}

	public boolean convertToBlackWhite( boolean autoInvert ) {
		
		if ( this.blackWhite && ( autoInvert == this.autoInvert || ! autoInvert ) ) {
			return false ;
		}
		
		ImageMeta imageData = new ImageMeta(this);

		int toRgbHigherValue;
		int toRgbLowervalue;
		int cntHigherValue;
		int cntLowerValue;

		if (imageData.getTreshold() < imageData.getAverageBrightness() || !autoInvert ) {
			toRgbHigherValue = WHITE;
			toRgbLowervalue = BLACK;
			cntHigherValue = 0;
			cntLowerValue = 1;
		} else {
			toRgbHigherValue = BLACK;
			toRgbLowervalue = WHITE;
			cntHigherValue = 1;
			cntLowerValue = 0;
		}

		this.histogramX = new ArrayList<Integer>(this.getWidth());
		this.histogramY = new ArrayList<Integer>(this.getHeight());

		for (int x = 0; x < this.getWidth(); ++x) {
			histogramX.add(0);
		}

		for (int y = 0; y < this.getHeight(); ++y) {
			histogramY.add(0);
		}

		for (int x = 0; x < this.getWidth(); ++x) {
			for (int y = 0; y < this.getHeight(); ++y) {
				int rgb = this.getRGB(x, y);
				int brightness = Helper.getBrightness(rgb);
				if (brightness > imageData.getTreshold()) {
					this.setRGB(x, y, toRgbHigherValue);
					this.histogramX.set(x, cntHigherValue + this.histogramX.get(x));
					this.histogramY.set(y, cntHigherValue + this.histogramY.get(y));
				} else {
					this.setRGB(x, y, toRgbLowervalue);
					this.histogramX.set(x, cntLowerValue + this.histogramX.get(x));
					this.histogramY.set(y, cntLowerValue + this.histogramY.get(y));
				}
			}
		}
		this.blackWhite = true ;
		return true ;
	}

	public void createHistograms() {
		
		if ( ! this.blackWhite ) {
			throw new WrongFormatError( "Not converted to black white") ;
		}

		this.histogramX = new ArrayList<Integer>(this.getWidth());
		this.histogramY = new ArrayList<Integer>(this.getHeight());

		for (int x = 0; x < this.getWidth(); ++x) {
			histogramX.add(0);
		}

		for (int y = 0; y < this.getHeight(); ++y) {
			histogramY.add(0);
		}

		for (int x = 0; x < this.getWidth(); ++x) {
			for (int y = 0; y < this.getHeight(); ++y) {
				int rgb = this.getRGB(x, y);
				if ( rgb == BLACK ) {
					this.histogramX.set(x, 1 + this.histogramX.get(x));
					this.histogramY.set(y, 1 + this.histogramY.get(y));
				}
			}
		}
	}

	public void shrink() {
		if ( this.histogramX == null || this.histogramY == null ) {
			throw new WrongFormatError( "Not converted to black white") ;
		}
		int minX = 0 ;
		int minY = 0 ;
		int width = 0 ;
		int height = 0 ;
		
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
				width = x ;
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
				height = y ;
				break ;
			}
		}
		
		this.histogramX = this.histogramX.subList(minX, width + 1) ;
		this.histogramY = this.histogramY.subList(minY, height + 1) ;
		
		width += 1 - minX ;
		height += 1 - minY ;
		this.maxRel = new Coordinate(width, height) ;
		this.min = new Coordinate(minX + this.min.getX(), minY + this.min.getY() ) ;

	}
	
	public int getHeight() {
		return this.maxRel.getY() ;
	}
	
	public int getWidth() {
		return this.maxRel.getX() ;
	}

	/**
	 * @return the histogramX
	 */
	public List<Integer> getHistogramX() {
		return histogramX;
	}

	/**
	 * @return the histogramY
	 */
	public List<Integer> getHistogramY() {
		return histogramY;
	}
	
	public class WrongFormatError extends Error {
		/**
		 * 
		 */
		private static final long serialVersionUID = 1983506390675701039L;

		public WrongFormatError( String message ) {
			super( message ) ;
		}
	}
	
	public class ErrorNotInRange extends Error {
		/**
		 * 
		 */
		private static final long serialVersionUID = 6325263495347327288L;

		public ErrorNotInRange( String message ) {
			super( message ) ;
		}
	}
	

}
