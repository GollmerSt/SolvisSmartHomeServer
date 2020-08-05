/************************************************************************
 * 
 * $Id$
 *
 * 
 ************************************************************************/

package de.sgollmer.solvismax.imagepatternrecognition.image;

import java.awt.Point;
import java.awt.image.BufferedImage;
import java.awt.image.DataBuffer;
import java.awt.image.MultiPixelPackedSampleModel;
import java.awt.image.Raster;
import java.awt.image.SampleModel;
import java.awt.image.WritableRaster;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import de.sgollmer.solvismax.helper.ImageHelper;
import de.sgollmer.solvismax.log.LogManager;
import de.sgollmer.solvismax.log.LogManager.ILogger;
import de.sgollmer.solvismax.objects.Coordinate;
import de.sgollmer.solvismax.objects.Rectangle;

public class MyImage {

	private static final ILogger logger = LogManager.getInstance().getLogger(MyImage.class);

	private final BufferedImage image;
	protected Coordinate origin;
	protected Coordinate size; // Zeigt auf 1. Pixel auﬂerhalb, relativ
								// zu min
	private final Collection<Rectangle> ignoreRectangles;

	private final ImageMeta meta;
	private Integer hashCode;

	protected List<Integer> histogramX = null;
	protected List<Integer> histogramY = null;

	private boolean autoInvert;

	public MyImage(BufferedImage image) {
		this.image = image;
		this.origin = new Coordinate(0, 0);
		this.size = this.getImageSize();
		this.autoInvert = false;
		this.meta = new ImageMeta(this);
		this.ignoreRectangles = null;
	}

	protected MyImage(MyImage image) {
		this.image = image.image;
		this.origin = image.origin;
		this.size = image.size;
		if (image.histogramX != null) {
			this.histogramX = new ArrayList<>(image.histogramX);
		}
		if (image.histogramY != null) {
			this.histogramY = new ArrayList<>(image.histogramY);
		}
		this.autoInvert = image.autoInvert;
		this.meta = image.meta;
		this.ignoreRectangles = null;

	}

	public MyImage(MyImage image, Rectangle rectangle, boolean createImageMeta) {
		this(image, rectangle.getTopLeft(), rectangle.getBottomRight(), createImageMeta);
	}

	protected MyImage(MyImage image, Coordinate topLeft, Coordinate bottomRight, boolean createImageMeta) {
		this(image, topLeft, bottomRight, createImageMeta, null);
	}

	public MyImage(MyImage image, boolean createImageMeta, Collection<Rectangle> ignoreRectangles) {
		this(image, null, null, false, ignoreRectangles);
	}

	private MyImage(MyImage image, Coordinate topLeft, Coordinate bottomRight, boolean createImageMeta,
			Collection<Rectangle> ignoreRectangles) {
		this.image = image.image;
		if (topLeft == null) {
			topLeft = image.origin;
		}
		if (bottomRight == null) {
			bottomRight = image.origin.add(image.size).decrement();
		}
		if (!image.isIn(topLeft) || !image.isIn(bottomRight)) {
			throw new ErrorNotInRange("UpperLeft or lowerRight not within the image");
		}
		if (ignoreRectangles != null) {
			this.ignoreRectangles = new ArrayList<>();
			for (Rectangle rectangle : ignoreRectangles) {
				if (!image.isIn(rectangle.getTopLeft()) || !image.isIn(rectangle.getBottomRight())) {
					throw new ErrorNotInRange("UpperLeft or lowerRigh not within the image");
				}
				Rectangle r = rectangle.add(image.origin);
				this.ignoreRectangles.add(r);
			}
		} else {
			this.ignoreRectangles = null;
		}
		this.size = bottomRight.diff(topLeft).increment();
		this.origin = image.origin.add(topLeft);
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
		SampleModel sm = new MultiPixelPackedSampleModel(DataBuffer.TYPE_BYTE, this.getWidth(), this.getHeight(), 4);
		WritableRaster raster = Raster.createWritableRaster(sm, new Point(0, 0));
		BufferedImage image = new BufferedImage(this.image.getColorModel(), raster, false, null);
		for (int x = 0; x < this.getWidth(); ++x) {
			for (int y = 0; y < this.getHeight(); ++y) {
				image.setRGB(x, y, this.getRGB(x, y));
			}
		}
		return image;
	}

	public boolean isIn(Coordinate coord) {
		return coord.getX() >= 0 && coord.getY() >= 0 && coord.getX() < this.size.getX()
				&& coord.getY() < this.size.getY();
	}

	protected boolean isIn(int x, int y) {
		return x >= 0 && y >= 0 && x < this.size.getX() && y < this.size.getY();
	}

	protected int getRGB(int x, int y) {
		return this.image.getRGB(x + this.origin.getX(), y + this.origin.getY());
	}

	public boolean isActive(Coordinate coord) {
		return this.isActive(coord.getX(), coord.getY());
	}

	protected boolean isActive(int x, int y) {
		if (this.isIn(x, y)) {
			return this.meta.isActive(this.getRGB(x, y));
		} else {
			return false;
		}
	}

	protected boolean isLight(int x, int y) {
		if (this.isIn(x, y)) {
			return this.meta.isLight(this.getRGB(x, y));
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
			this.histogramX.add(0);
		}

		for (int y = 0; y < this.getHeight(); ++y) {
			this.histogramY.add(0);
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
		int maxX = -1;
		int maxY = -1;

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
				maxX = x;
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
				maxY = y;
				break;
			}
		}

		this.histogramX = this.histogramX.subList(minX, maxX + 1);
		this.histogramY = this.histogramY.subList(minY, maxY + 1);

		int width = maxX + 1 - minX;
		int height = maxY + 1 - minY;
		this.size = new Coordinate(width, height);
		this.origin = new Coordinate(minX + this.origin.getX(), minY + this.origin.getY());

	}

	public int getHeight() {
		return this.size.getY();
	}

	public int getWidth() {
		return this.size.getX();
	}

	/**
	 * @return the histogramX
	 */
	public List<Integer> getHistogramX() {
		return this.histogramX;
	}

	/**
	 * @return the histogramY
	 */
	public List<Integer> getHistogramY() {
		return this.histogramY;
	}

	private class WrongFormatError extends Error {
		/**
		 * 
		 */
		private static final long serialVersionUID = 1983506390675701039L;

		private WrongFormatError(String message) {
			super(message);
		}
	}

	private class ErrorNotInRange extends Error {
		/**
		 * 
		 */
		private static final long serialVersionUID = 6325263495347327288L;

		private ErrorNotInRange(String message) {
			super(message);
		}
	}

	@Override
	public int hashCode() {
		if (this.hashCode == null) {
			this.hashCode = 269;
			for (int x = 0; x < this.getWidth(); ++x) {
				for (int y = 0; y < this.getHeight(); ++y) {
					int brighness = ImageHelper.getBrightness(this.getRGB(x, y));
					this.hashCode = this.hashCode * 643 + brighness * 193;
				}
			}
		}
		return this.hashCode;
	}

	@Override
	public boolean equals(Object obj) {
		return this.equals(obj, false);
	}

	public boolean equals(Object obj, boolean ignoreEnable) {
		if (!(obj instanceof MyImage)) {
			return false;
		}
		MyImage i = (MyImage) obj;

		if (this.getWidth() != i.getWidth() || this.getHeight() != i.getHeight()) {
			return false;
		}
		if (this.hashCode() != i.hashCode()
				&& (!ignoreEnable || this.ignoreRectangles == null && i.ignoreRectangles == null)) {
			return false;
		}
		for (int x = 0; x < this.getWidth(); ++x) {
			for (int y = 0; y < this.getHeight(); ++y) {
				if (!ignoreEnable || !this.toIgnore(x, y)) {
					int brighness1 = ImageHelper.getBrightness(this.getRGB(x, y));
					int brighness2 = ImageHelper.getBrightness(i.getRGB(x, y));
					if (brighness1 != brighness2) {
						return false;
					}
				}
			}
		}
		return true;
	}

	private boolean toIgnore(int x, int y) {
		if (this.ignoreRectangles == null) {
			return false;
		}
		for (Rectangle rectangle : this.ignoreRectangles) {
			if (rectangle.isIn(x, y)) {
				return !rectangle.isInvertFunction();
			}
		}
		return false;
	}

	public String getDebugInfo() {
		return "Origin: " + this.origin + ", max: " + this.size;
	}

	public BufferedImage getImage() {
		return this.image;
	}

	public Coordinate getOrigin() {
		return this.origin;
	}

	public final Coordinate getImageSize() {
		return new Coordinate(this.image.getWidth(), this.image.getHeight());
	}

	public boolean isWhite(Rectangle rectangle) {
		if (this.meta == null) {
			logger.error("isWhite was tried to execute on an MyImage object without meta informations");
			return false;
		}
		for (int x = rectangle.getTopLeft().getX(); x <= rectangle.getBottomRight().getX(); ++x) {
			for (int y = rectangle.getTopLeft().getY(); y <= rectangle.getBottomRight().getY(); ++y) {
				if (!this.isLight(x, y)) {
					return false;
				}
			}
		}
		return true;
	}

}
