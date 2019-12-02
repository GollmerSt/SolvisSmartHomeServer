package de.sgollmer.solvismax.imagepatternrecognition.image;

import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.List;

import de.sgollmer.solvismax.helper.ImageHelper;
import de.sgollmer.solvismax.objects.Coordinate;
import de.sgollmer.solvismax.objects.Rectangle;

public class MyImage {

	private final BufferedImage image;
	protected Coordinate topLeft;
	protected Coordinate maxRel; // Zeigt auf 1. Pixel auﬂerhalb, relativ
								// zu min

	private final ImageMeta meta;
	private Integer hashCode ;

	protected List<Integer> histogramX = null;
	protected List<Integer> histogramY = null;

	private boolean autoInvert;

	public MyImage(BufferedImage image) {
		this.image = image;
		this.topLeft = new Coordinate(0, 0);
		this.maxRel = new Coordinate(image.getWidth(), image.getHeight());
		this.autoInvert = false;
		this.meta = new ImageMeta(this);
	}

	public MyImage(MyImage image) {
		this.image = image.image;
		this.topLeft = image.topLeft;
		this.maxRel = image.maxRel;
		if (image.histogramX != null) {
			this.histogramX = new ArrayList<>(image.histogramX);
		}
		if (image.histogramY != null) {
			this.histogramY = new ArrayList<>(image.histogramY);
		}
		this.autoInvert = image.autoInvert;
		this.meta = image.meta;

	}

	public MyImage(MyImage image, Rectangle rectangle, boolean createImageMeta) {
		this(image, rectangle.getTopLeft(), rectangle.getBottomRight(), createImageMeta);
	}

	public MyImage(MyImage image, Coordinate topLeft, Coordinate bottomRight, boolean createImageMeta) {
		this.image = image.image;
		if (!image.isIn(topLeft) || !image.isIn(bottomRight)) {
			throw new ErrorNotInRange("UpperLeft or lowerRigh not within the image");
		}
		this.maxRel = bottomRight.diff(topLeft).increment();
		this.topLeft = image.topLeft.add(topLeft);
		this.autoInvert = image.autoInvert;
		if (createImageMeta) {
			this.meta = new ImageMeta(this);
		} else {
			this.meta = image.meta;
		}

		if (bottomRight.getX() < topLeft.getX() || bottomRight.getY() < topLeft.getY()) {
			throw new ErrorNotInRange("Upper left is not upper left");
		}
		if (this.histogramX != null) {
			this.histogramX = null;
			this.histogramY = null;
			this.createHistograms(this.autoInvert);
		}
	}

	public BufferedImage createBufferdImage() {
		return this.image.getSubimage(this.topLeft.getX(), this.topLeft.getY(), this.maxRel.getX(), this.maxRel.getY()) ;
	}

	public boolean isIn(Coordinate coord) {
		return coord.getX() >= 0 && coord.getY() >= 0 && coord.getX() < maxRel.getX() && coord.getY() < maxRel.getY();
	}

	public boolean isIn(int x, int y) {
		return x >= 0 && y >= 0 && x < maxRel.getX() && y < maxRel.getY();
	}

	int getRGB(int x, int y) {
		return this.image.getRGB(x + topLeft.getX(), y + this.topLeft.getY());
	}

	public boolean isActive(Coordinate coord) {
		return this.isActive(coord.getX(), coord.getY());
	}

	public boolean isActive(int x, int y) {
		if (this.isIn(x, y)) {
			return this.meta.isActive(image.getRGB(x + topLeft.getX(), y + this.topLeft.getY()));
		} else {
			return false;
		}
	}

	public boolean isLight(Coordinate coord) {
		return this.isLight(coord.getX(), coord.getY());
	}

	public boolean isLight(int x, int y) {
		if (this.isIn(x, y)) {
			return this.meta.isLight(image.getRGB(x + topLeft.getX(), y + this.topLeft.getY()));
		} else {
			return !this.meta.isInvert();
		}
	}

	public boolean createHistograms(boolean autoInvert) {

		if (this.histogramX != null && (autoInvert == this.autoInvert)) {
			return false;
		}

		this.histogramX = new ArrayList<Integer>(this.getWidth());
		this.histogramY = new ArrayList<Integer>(this.getHeight());

		for (int x = 0; x < this.getWidth(); ++x) {
			histogramX.add(0);
		}

		for (int y = 0; y < this.getHeight(); ++y) {
			histogramY.add(0);
		}

		if (autoInvert) {

			for (int x = 0; x < this.getWidth(); ++x) {
				for (int y = 0; y < this.getHeight(); ++y) {
					boolean active = this.isActive(x, y);
					if (active) {
						this.histogramX.set(x, 1 + this.histogramX.get(x));
						this.histogramY.set(y, 1 + this.histogramY.get(y));
					}
				}
			}
		} else {
			for (int x = 0; x < this.getWidth(); ++x) {
				for (int y = 0; y < this.getHeight(); ++y) {
					boolean dark = !this.isLight(x, y);
					if (dark) {
						this.histogramX.set(x, 1 + this.histogramX.get(x));
						this.histogramY.set(y, 1 + this.histogramY.get(y));
					}
				}
			}
		}
		this.autoInvert = autoInvert;
		return true;
	}

	public void shrink() {
		if (this.histogramX == null || this.histogramY == null) {
			throw new WrongFormatError("Not converted to black white");
		}
		int minX = 0;
		int minY = 0;
		int width = 0;
		int height = 0;

		for (int x = 0; x < this.histogramX.size(); ++x) {
			int cnt = this.histogramX.get(x);
			if (cnt > 0) {
				minX = x;
				break;
			}
		}

		for (int x = this.histogramX.size() - 1; x >= 0; --x) {
			int cnt = this.histogramX.get(x);
			if (cnt > 0) {
				width = x;
				break;
			}
		}

		for (int y = 0; y < this.histogramY.size(); ++y) {
			int cnt = this.histogramY.get(y);
			if (cnt > 0) {
				minY = y;
				break;
			}
		}

		for (int y = this.histogramY.size() - 1; y >= 0; --y) {
			int cnt = this.histogramY.get(y);
			if (cnt > 0) {
				height = y;
				break;
			}
		}

		this.histogramX = this.histogramX.subList(minX, width + 1);
		this.histogramY = this.histogramY.subList(minY, height + 1);

		width += 1 - minX;
		height += 1 - minY;
		this.maxRel = new Coordinate(width, height);
		this.topLeft = new Coordinate(minX + this.topLeft.getX(), minY + this.topLeft.getY());

	}

	public int getHeight() {
		return this.maxRel.getY();
	}

	public int getWidth() {
		return this.maxRel.getX();
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

		public WrongFormatError(String message) {
			super(message);
		}
	}

	public class ErrorNotInRange extends Error {
		/**
		 * 
		 */
		private static final long serialVersionUID = 6325263495347327288L;

		public ErrorNotInRange(String message) {
			super(message);
		}
	}
	
	@Override
	public int hashCode() {
		if (this.hashCode == null) {
			this.hashCode = 269;
			for (int x = 0; x < this.getWidth(); ++x) {
				for (int y = 0; y < this.getHeight(); ++y) {
					int brighness = ImageHelper.getBrightness( this.image.getRGB(x+topLeft.getX(), y+topLeft.getY())) ;
					this.hashCode = this.hashCode * 643 + brighness * 193 ;
				}
			}
		}
		return this.hashCode;
	}

	@Override
	public boolean equals(Object obj) {
		if (!(obj instanceof MyImage)) {
			return false;
		}
		MyImage i = (MyImage) obj;

		if (this.getWidth() != i.getWidth() || this.getHeight() != i.getHeight()) {
			return false;
		}
		if (this.hashCode() != i.hashCode()) {
			return false;
		}
		for (int x = 0; x < this.getWidth(); ++x) {
			for (int y = 0; y < this.getHeight(); ++y) {
				int brighness1 = ImageHelper.getBrightness( this.image.getRGB(x+topLeft.getX(), y+topLeft.getY())) ;
				int brighness2 = ImageHelper.getBrightness( i.image.getRGB(x+i.topLeft.getX(), y+i.topLeft.getY())) ;
				if (brighness1 != brighness2) {
					return false;
				}
			}
		}
		return true;
	}



}
