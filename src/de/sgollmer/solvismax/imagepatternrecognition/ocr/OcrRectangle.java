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

import de.sgollmer.solvismax.imagepatternrecognition.image.MyImage;
import de.sgollmer.solvismax.log.LogManager;
import de.sgollmer.solvismax.log.LogManager.ILogger;
import de.sgollmer.solvismax.objects.Coordinate;
import de.sgollmer.solvismax.objects.Rectangle;

public class OcrRectangle extends MyImage {

	private static final ILogger logger = LogManager.getInstance().getLogger(OcrRectangle.class);

	private Collection<Ocr> parts = null;

	private OcrRectangle(final MyImage image, final Coordinate upperLeft, final Coordinate lowerRight) {
		super(image, upperLeft, lowerRight, true);
		this.splitInParts();
	}

	public OcrRectangle(final MyImage image, final Rectangle rectangle) {
		super(image, rectangle, true);
		this.splitInParts();
	}

	private OcrRectangle(MyImage image) {
		super(image);
		this.splitInParts();
	}

	public OcrRectangle(final MyImage myImage, final Collection<MyImage> images) {
		super(myImage);
		this.parts = new ArrayList<>(images.size());
		for (MyImage image : images) {
			this.parts.add(new Ocr(image));
		}
	}

	public void splitInParts() {

		Collection<MyImage> images = this.split();

		this.parts = new ArrayList<>();

		for (MyImage image : images) {
			this.parts.add(new Ocr(image));
		}
	}

	public String getString() {
		StringBuilder builder = new StringBuilder();

		for (Iterator<Ocr> it = this.parts.iterator(); it.hasNext();) {
			Ocr ocr = it.next();
			builder.append(ocr.toChar());
		}
		String result = builder.toString();

		// logger.debug("String detected by OCR: " + result );

		return result;
	}

	public static void main(final String[] args) {

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

		System.out.println("ReturnTemperature is: " + returnTemperature);

		// -----------------------------------------------

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

		// -------------------------------------------------------------

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

		file = new File(parent, "mySolvis__Heizkreis3-1_5__0.png");

		try {
			bufferedImage = ImageIO.read(file);
		} catch (IOException e) {
			e.printStackTrace();
		}

		myImage = new MyImage(bufferedImage);

		rectangle = new OcrRectangle(myImage, new Coordinate(162, 2), new Coordinate(198, 13));

		screenId = rectangle.getString();

		rectangle = new OcrRectangle(myImage, new Coordinate(150, 75), new Coordinate(190, 88));

		System.out.println("ScreenId is: " + screenId);

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

		// -------------------------------------------------------------

		file = new File(parent, "Zaehlfunktion-Max7.png");

		try {
			bufferedImage = ImageIO.read(file);
		} catch (IOException e) {
			e.printStackTrace();
		}

		myImage = new MyImage(bufferedImage);

		rectangle = new OcrRectangle(myImage, new Coordinate(145, 75), new Coordinate(205, 87));

		String kWh = rectangle.getString();

		System.out.println("kWh: " + kWh);

		try {
			bufferedImage = ImageIO.read(file);
		} catch (IOException e) {
			e.printStackTrace();
		}

		myImage = new MyImage(bufferedImage);

		rectangle = new OcrRectangle(myImage, new Coordinate(145, 104), new Coordinate(205, 115));

		kWh = rectangle.getString();

		System.out.println("kWh: " + kWh);

	}

	@SuppressWarnings("unused")
	private static void dummy() {
		logger.debug("Nur ein Dummy");
	}

}
