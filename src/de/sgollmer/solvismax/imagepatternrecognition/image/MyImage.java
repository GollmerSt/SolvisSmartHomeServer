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
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Set;

import javax.imageio.ImageIO;
import javax.mail.util.ByteArrayDataSource;

import de.sgollmer.solvismax.Constants;
import de.sgollmer.solvismax.log.LogManager;
import de.sgollmer.solvismax.log.LogManager.ILogger;
import de.sgollmer.solvismax.objects.Coordinate;
import de.sgollmer.solvismax.objects.Rectangle;

public class MyImage {

	private static final ILogger logger = LogManager.getInstance().getLogger(MyImage.class);

	private static final int HASH_START = 269;
	private static final int HASH_HASH_MULT = 643;
	private static final int HASH_VALUE_MULT = 193;

	private final BufferedImage image;
	protected Coordinate origin;
	protected Coordinate size; // Zeigt auf 1. Pixel auﬂerhalb, relativ
								// zu min
	private final Collection<Rectangle> ignoreRectanglesWork;
	private final Set<Rectangle> ignoreRectangles;

	private final ImageMeta meta;
	private Integer hashCode;

	protected List<Integer> histogramX = null;
	protected List<Integer> histogramY = null;

	private boolean autoInvert;

	public MyImage(final BufferedImage image) {
		this.image = image;
		this.origin = new Coordinate(0, 0);
		this.size = this.getImageSize();
		this.autoInvert = false;
		this.meta = new ImageMeta(this);
		this.ignoreRectanglesWork = null;
		this.ignoreRectangles = null;
	}

	public MyImage(final MyImage image) {
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
		this.ignoreRectanglesWork = null;
		this.ignoreRectangles = null;

	}

	public MyImage(final MyImage image, final Rectangle rectangle, final boolean createImageMeta) {
		this(image, rectangle.getTopLeft(), rectangle.getBottomRight(), createImageMeta);
	}

	protected MyImage(final MyImage image, final Coordinate topLeft, final Coordinate bottomRight,
			final boolean createImageMeta) {
		this(image, topLeft, bottomRight, createImageMeta, null);
	}

	public MyImage(final MyImage image, final boolean createImageMeta, final Set<Rectangle> ignoreRectangles) {
		this(image, null, null, false, ignoreRectangles);
	}

	private MyImage(final MyImage image, final Coordinate topLeftP, final Coordinate bottomRightP,
			final boolean createImageMeta, Set<Rectangle> ignoreRectangles) {
		this.image = image.image;
		Coordinate topLeft, bottomRight;
		if (topLeftP == null) {
			topLeft = image.origin;
		} else {
			topLeft = topLeftP;
		}
		if (bottomRightP == null) {
			bottomRight = image.origin.add(image.size).decrement();
		} else {
			bottomRight = bottomRightP;
		}
		if (!image.isIn(topLeft) || !image.isIn(bottomRight)) {
			throw new ErrorNotInRange("UpperLeft or lowerRight not within the image");
		}
		this.ignoreRectangles = ignoreRectangles;
		if (ignoreRectangles != null) {
			this.ignoreRectanglesWork = new ArrayList<>();
			for (Rectangle rectangle : ignoreRectangles) {
				if (!image.isIn(rectangle.getTopLeft()) || !image.isIn(rectangle.getBottomRight())) {
					throw new ErrorNotInRange("UpperLeft or lowerRigh not within the image");
				}
				Rectangle r = rectangle.add(image.origin);
				this.ignoreRectanglesWork.add(r);
			}
		} else {
			this.ignoreRectanglesWork = null;
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

	public boolean isIn(final Coordinate coord) {
		return coord.getX() >= 0 && coord.getY() >= 0 && coord.getX() < this.size.getX()
				&& coord.getY() < this.size.getY();
	}

	protected boolean isIn(final int x, final int y) {
		return x >= 0 && y >= 0 && x < this.size.getX() && y < this.size.getY();
	}

	protected int getRGB(final int x, final int y) {
		return this.image.getRGB(x + this.origin.getX(), y + this.origin.getY()) & 0x00ffffff;
	}

	protected int getBrightness(final int x, final int y) {
		int rgb = this.image.getRGB(x + this.origin.getX(), y + this.origin.getY()) & 0x00ffffff;
		return (rgb & 0xff) + (rgb >> 8 & 0xff) + (rgb >> 16 & 0xff);
	}

	public boolean isActive(final Coordinate coord) {
		return this.isActive(coord.getX(), coord.getY());
	}

	protected boolean isActive(final int x, final int y) {
		if (this.isIn(x, y)) {
			return this.meta.isActive(this.getRGB(x, y));
		} else {
			return false;
		}
	}

	protected boolean isLight(final int x, final int y) {
		if (this.isIn(x, y)) {
			return this.meta.isLight(this.getRGB(x, y));
		} else {
			return !this.meta.isInvert();
		}
	}

	public boolean createHistograms(final boolean autoInvert) {

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
		this.hashCode = null;
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
			this.hashCode = HASH_START;
			for (int x = 0; x < this.getWidth(); ++x) {
				for (int y = 0; y < this.getHeight(); ++y) {
					this.hashCode = this.hashCode * HASH_HASH_MULT + this.getRGB(x, y) * HASH_VALUE_MULT;
				}
			}
		}
		return this.hashCode;
	}

	@Override
	public boolean equals(final Object obj) {

		if (!(obj instanceof MyImage)) {
			return false;
		}

		MyImage cmp = (MyImage) obj;

		if (this.getWidth() != cmp.getWidth() || this.getHeight() != cmp.getHeight()) {
			return false;
		}

		boolean result = true;

		if (this.hashCode == null && cmp.hashCode == null) {

			this.hashCode = HASH_START;
			cmp.hashCode = HASH_START;

			for (int x = 0; x < this.getWidth(); ++x) {
				for (int y = 0; y < this.getHeight(); ++y) {

					int rgbT = this.getRGB(x, y);
					int rgbC = cmp.getRGB(x, y);

					result &= rgbT == rgbC;

					this.hashCode = this.hashCode * HASH_HASH_MULT + rgbT * HASH_VALUE_MULT;
					cmp.hashCode = cmp.hashCode * HASH_HASH_MULT + rgbC * HASH_VALUE_MULT;
				}
			}

			return result;

		} else if (this.hashCode == null || cmp.hashCode == null) {

			MyImage hash;
			MyImage other;

			if (this.hashCode == null) {
				hash = this;
				other = cmp;
			} else {
				hash = cmp;
				other = this;
			}

			hash.hashCode = HASH_START;

			int x;
			int y = 0;

			for (x = 0; x < this.getWidth(); ++x) {
				for (y = 0; y < this.getHeight(); ++y) {

					int rgbH = hash.getRGB(x, y);
					int rgbO = other.getRGB(x, y);

					result &= rgbH == rgbO;

					if (!result) {
						break;
					}

					hash.hashCode = hash.hashCode * HASH_HASH_MULT + rgbH * HASH_VALUE_MULT;
				}

				if (!result) {
					break;
				}
			}

			if (result) {
				return true;
			}

			for (; x < this.getWidth(); ++x) {
				for (; y < this.getHeight(); ++y) {
					int rgbH = hash.getRGB(x, y);
					hash.hashCode = hash.hashCode * HASH_HASH_MULT + rgbH * HASH_VALUE_MULT;
				}
				y = 0;
			}

			return false;

		}

		if (this.hashCode() != cmp.hashCode()) {
			return false;
		}

		for (int x = 0; x < this.getWidth(); ++x) {
			for (int y = 0; y < this.getHeight(); ++y) {
				if (this.getRGB(x, y) != cmp.getRGB(x, y)) {
					return false;
				}
			}
		}

		return true;
	}

	public boolean equals(final Object obj, final boolean ignoreEnable) {
		if (!(obj instanceof MyImage)) {
			return false;
		}
		MyImage cmp = (MyImage) obj;

		if (this.getWidth() != cmp.getWidth() || this.getHeight() != cmp.getHeight()) {
			return false;
		}

		if (!ignoreEnable || this.ignoreRectangles == null && cmp.ignoreRectangles == null) {
			return this.equals(obj);
		}

		MyImage ignoreObject;

		if (this.ignoreRectangles == null) {
			ignoreObject = cmp;
		} else if (cmp.ignoreRectangles == null) {
			ignoreObject = this;
		} else if (!this.ignoreRectangles.equals(cmp.ignoreRectangles)) {
			logger.debug("Warning: ignoreRectanglesWork are different of compared objects");
			return false;
		} else {
			ignoreObject = this;
		}

		for (int x = 0; x < this.getWidth(); ++x) {
			for (int y = 0; y < this.getHeight(); ++y) {
				if (!ignoreObject.toIgnore(x, y)) {
					if (this.getRGB(x, y) != cmp.getRGB(x, y)) {
						return false;
					}
				}
			}
		}
		return true;
	}

	private boolean toIgnore(final int x, final int y) {
		if (this.ignoreRectanglesWork == null) {
			return false;
		}
		for (Rectangle rectangle : this.ignoreRectanglesWork) {
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

	public boolean isWhite(final Rectangle rectangle) {
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

	public ByteArrayDataSource getByteArrayDataSource() throws IOException {
		ByteArrayOutputStream baos = new ByteArrayOutputStream();
		ImageIO.write(this.image, Constants.Files.GRAFIC_SUFFIX, baos);
		baos.flush();
		byte[] imageBytes = baos.toByteArray();
		baos.close();
		return new ByteArrayDataSource(imageBytes, "image/" + Constants.Files.GRAFIC_SUFFIX);
	}

	public void writeWhole(final File file) throws IOException {
		FileOutputStream out = new FileOutputStream(file);
		this.writeWhole(out);
		out.close();
	}

	public void write(final File file) throws IOException {
		FileOutputStream out = new FileOutputStream(file);
		this.write(out);
		out.close();
	}

	public void writeWhole(final OutputStream out) throws IOException {
		ImageIO.write(this.image, Constants.Files.GRAFIC_SUFFIX, out);
	}

	public void write(final OutputStream out) throws IOException {
		ImageIO.write(this.image.getSubimage(this.origin.getX(), this.origin.getY(), this.getWidth(), this.getHeight()),
				Constants.Files.GRAFIC_SUFFIX, out);
	}

	public List<MyImage> split() {
		this.createHistograms(true);

		this.removeFrame();

		this.shrink();

		List<MyImage> parts = new ArrayList<>();

		Coordinate start = null;

		int lower = this.getHeight() - 1;

		for (int x = 0; x < this.getHistogramX().size(); ++x) {

			int cnt = this.getHistogramX().get(x);
			if (cnt > 0) {
				if (start == null) {
					start = new Coordinate(x, 0);
				}
			} else {
				if (start != null) {
					Coordinate end = new Coordinate(x - 1, lower);
					parts.add(new MyImage(this, start, end, false));
					start = null;
				}
			}
		}
		if (start != null) {
			Coordinate end = new Coordinate(this.getHistogramX().size() - 1, lower);
			parts.add(new MyImage(this, start, end, false));
		}

		return parts;
	}

	private void removeFrame() {
		int i;
		boolean missed = true;
		for (i = 0; i < this.getWidth() && this.getHistogramX().get(i) == this.getHeight(); ++i) {
			missed = false;
		}
		if (missed) {
			return;
		}

		int left = i;
		missed = true;

		for (i = this.getWidth() - 1; i >= 0 && this.getHistogramX().get(i) == this.getHeight(); --i) {
			missed = false;
		}
		if (missed) {
			return;
		}

		int right = i;

		if (left > right) {
			return;
		}

		missed = true;
		for (i = 0; i < this.getHeight() && this.getHistogramY().get(i) == this.getWidth(); ++i) {
			missed = false;
		}
		if (missed) {
			return;
		}

		int top = i;
		missed = true;

		for (i = this.getHeight() - 1; i >= 0 && this.getHistogramY().get(i) == this.getWidth(); --i) {
			missed = false;
		}
		if (missed) {
			return;
		}

		int bottom = i;

		if (top > bottom) {
			return;
		}

		int remainingX = right - left + 1;
		int remainingY = bottom - top + 1;
		int thicknessX = this.getWidth() - remainingX;
		int thicknessY = this.getHeight() - remainingY;

		if (remainingX == 0 || remainingY == 0) {
			return;
		}
		if (this.getHistogramX().get(left) != thicknessY && this.getHistogramX().get(right) != thicknessY
				&& this.getHistogramY().get(top) != thicknessX && this.getHistogramY().get(bottom) != thicknessX) {
			return;
		}
		this.origin = new Coordinate(left + this.origin.getX(), top + this.origin.getY());
		this.size = new Coordinate(remainingX, remainingY);

		this.histogramX = this.histogramX.subList(left, right + 1);
		this.histogramY = this.histogramY.subList(top, bottom + 1);

		for (i = 0; i < this.histogramX.size(); ++i) {
			this.histogramX.set(i, this.histogramX.get(i) - thicknessY);
		}

		for (i = 0; i < this.histogramY.size(); ++i) {
			this.histogramY.set(i, this.histogramY.get(i) - thicknessX);
		}

		this.hashCode = null;
	}

	/**
	 * 
	 * @param start      Left corner of the search area (out of the frame)
	 * @param rightLimit Right limitation. Search area includes this point
	 * @param light      True: Search for a light pixel
	 * @param upToDown   True: Search from up to down
	 * @return y coordinate of the frame
	 */
	public int searchFramefromOuter(final Coordinate start, final int rightLimit, boolean light, boolean upToDown) {

		for (int y = start.getY(); upToDown ? (y < this.getHeight()) : (y >= 0); y += upToDown ? 1 : -1) {
			for (int x = start.getX(); x <= rightLimit && x < this.getWidth(); ++x) {
				if (this.isLight(x, y) == light) {
					return y;
				}
			}
		}

		return -1;
	}

	/**
	 * 
	 * @param start      Left corner of the search area (inner of the frame)
	 * @param rightLimit Right limitation. Search area includes this point
	 * @param light      True: Search for a light pixel
	 * @param upToDown   True: Search from up to down
	 * @return y coordinate of the frame
	 */
	public int searchFramefromInner(final Coordinate start, final int rightLimit, boolean light, boolean upToDown) {

		boolean found = false;

		int y = -1;

		for (y = start.getY(); !found && (upToDown ? (y < this.getHeight()) : (y >= 0)); y += upToDown ? 1 : -1) {
			for (int x = start.getX(); x <= rightLimit && x < this.getWidth(); ++x) {
				found = true;
				if (this.isLight(x, y) == light) {
					found = false;
					break;
				}
			}
		}
		if (found) {
			return y - 1;
		} else {
			return upToDown ? this.getHeight() - 1 : 0;
		}
	}

}
