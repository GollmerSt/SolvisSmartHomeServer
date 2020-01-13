/************************************************************************
 * 
 * $Id$
 *
 * 
 ************************************************************************/

package de.sgollmer.solvismax.imagepatternrecognition.ocr;

import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;

import javax.imageio.ImageIO;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import de.sgollmer.solvismax.imagepatternrecognition.image.MyImage;
import de.sgollmer.solvismax.objects.Coordinate;
import de.sgollmer.solvismax.objects.Rectangle;

public class OcrRectangle extends MyImage {

	private static final Logger logger = LogManager.getLogger(OcrRectangle.class);

	private Collection<Ocr> parts = null;

	public OcrRectangle(MyImage image, Coordinate upperLeft, Coordinate lowerRight) {
		super(image, upperLeft, lowerRight, true);
		this.split();
	}

	public OcrRectangle(MyImage image, Rectangle rectangle) {
		super(image, rectangle, true);
		this.split();
	}

	public OcrRectangle(MyImage image) {
		super(image);
		this.split();
	}

	private void split() {

		this.createHistograms(true);

		this.removeFrame();

		this.shrink();

		this.parts = new ArrayList<>();

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
					this.parts.add(new Ocr(this, start, end, false));
					start = null;
				}
			}
		}
		if (start != null) {
			Coordinate end = new Coordinate(this.getHistogramX().size() - 1, lower);
			this.parts.add(new Ocr(this, start, end, false));
		}
	}

	public String getString() {
		StringBuilder builder = new StringBuilder();

		for (Iterator<Ocr> it = this.parts.iterator(); it.hasNext();) {
			Ocr ocr = it.next();
			builder.append(ocr.toChar());
		}
		String result = builder.toString();
		
		//logger.debug("String detected by OCR: " + result );

		return result;
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
		this.maxRel = new Coordinate(remainingX, remainingY);

		this.histogramX = this.histogramX.subList(left, right + 1);
		this.histogramY = this.histogramY.subList(top, bottom + 1);

		for (i = 0; i < this.histogramX.size(); ++i) {
			this.histogramX.set(i, this.histogramX.get(i) - thicknessY);
		}

		for (i = 0; i < this.histogramY.size(); ++i) {
			this.histogramY.set(i, this.histogramY.get(i) - thicknessX);
		}

	}

	public static void main(String[] args) {

		File parent = new File("testFiles\\images");
		
		File file = new File(parent, "4 Solar.png");

		BufferedImage bufferedImage = null;
		try {
			bufferedImage = ImageIO.read(file);
		} catch (IOException e) {
			e.printStackTrace();
		}

		MyImage myImage = new MyImage(bufferedImage);

		OcrRectangle rectangle = new OcrRectangle(myImage, new Coordinate(170, 30), new Coordinate(235, 40));

		String returnTemperature = rectangle.getString();
		
		System.out.println("ReturnTemperature is: " + returnTemperature );
		
		//-----------------------------------------------

		file = new File(parent, "bildschirmschoner.png");

		bufferedImage = null;
		try {
			bufferedImage = ImageIO.read(file);
		} catch (IOException e) {
			e.printStackTrace();
		}

		myImage = new MyImage(bufferedImage);
		myImage.createHistograms(true);
		myImage.shrink();
		rectangle = new OcrRectangle(myImage, new Coordinate(73, 0), new Coordinate(139, 20));

		String uhrzeit = rectangle.getString();

		rectangle = new OcrRectangle(myImage, new Coordinate(77, 21), new Coordinate(136, 33));

		String date = rectangle.getString();

		System.out.println("Time is: " + uhrzeit + ", date is " + date);

		file = new File(parent, "Home.png");

		try {
			bufferedImage = ImageIO.read(file);
		} catch (IOException e) {
			e.printStackTrace();
		}

		myImage = new MyImage(bufferedImage);

		rectangle = new OcrRectangle(myImage, new Coordinate(82, 10), new Coordinate(155, 47));

		String temperature = rectangle.getString();

		rectangle = new OcrRectangle(myImage, new Coordinate(85, 50), new Coordinate(103, 73));

		String adjust = rectangle.getString();

		rectangle = new OcrRectangle(myImage, new Coordinate(190, 61), new Coordinate(237, 73));

		uhrzeit = rectangle.getString();

		rectangle = new OcrRectangle(myImage, new Coordinate(190, 80), new Coordinate(237, 91));

		date = rectangle.getString();

		System.out.println("Time is: " + uhrzeit + ", date is " + date + ", temperature is " + temperature
				+ ", adjustment is " + adjust);

		file = new File(parent, "raumeinfluss.png");

		try {
			bufferedImage = ImageIO.read(file);
		} catch (IOException e) {
			e.printStackTrace();
		}

		myImage = new MyImage(bufferedImage);

		rectangle = new OcrRectangle(myImage, new Coordinate(162, 0), new Coordinate(200, 14));

		String screenId = rectangle.getString();

		rectangle = new OcrRectangle(myImage, new Coordinate(150, 75), new Coordinate(190, 88));

		String raumeinfluss = rectangle.getString();

		System.out.println("ScreenId is: " + screenId + ", raumeinfluss is " + raumeinfluss);

		// -------------------------------------------------------------

		file = new File(parent, "Night Temperatur selected.png");

		try {
			bufferedImage = ImageIO.read(file);
		} catch (IOException e) {
			e.printStackTrace();
		}

		myImage = new MyImage(bufferedImage);

		rectangle = new OcrRectangle(myImage, new Coordinate(55, 30), new Coordinate(110, 60));

		String nightTemp = rectangle.getString();

		System.out.println("Night temperature is " + nightTemp);

		// -------------------------------------------------------------

		file = new File(parent, "Zaehlfunktion.png");

		try {
			bufferedImage = ImageIO.read(file);
		} catch (IOException e) {
			e.printStackTrace();
		}

		myImage = new MyImage(bufferedImage);

		rectangle = new OcrRectangle(myImage, new Coordinate(145, 75), new Coordinate(200, 88));

		String brennerStarts = rectangle.getString();

		System.out.println("Burner starts is " + brennerStarts);


		// -------------------------------------------------------------

		file = new File(parent, "-4 fein.png");

		try {
			bufferedImage = ImageIO.read(file);
		} catch (IOException e) {
			e.printStackTrace();
		}

		myImage = new MyImage(bufferedImage);

		rectangle = new OcrRectangle(myImage);

		String feineinstellung = rectangle.getString();

		System.out.println("Feineinstellung Soll: [-4], Ist: " + feineinstellung);
	}
	
	public static void dummy() {
		logger.debug("Nur ein Dummy");
	}

}
