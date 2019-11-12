package de.sgollmer.solvismax.imagepatternrecognition.ocr;

import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;

import javax.imageio.ImageIO;

import de.sgollmer.solvismax.imagepatternrecognition.image.Coordinate;
import de.sgollmer.solvismax.imagepatternrecognition.image.MyImage;

public class OcrField extends MyImage {
	private Collection<Ocr> parts = null;

	public OcrField(MyImage image, Coordinate upperLeft, Coordinate lowerRight) {
		super(image, upperLeft, lowerRight);
		this.split();
	}

	private void split() {

		this.convertToBlackWhite(true);

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
					this.parts.add(new Ocr(this, start, end));
					start = null;
				}
			}
		}
		if (start != null) {
			Coordinate end = new Coordinate(this.getHistogramX().size() - 1, lower);
			this.parts.add(new Ocr(this, start, end));
		}
	}

	public String getString() {
		StringBuilder builder = new StringBuilder();

		for (Iterator<Ocr> it = this.parts.iterator(); it.hasNext();) {
			Ocr ocr = it.next();
			builder.append(ocr.toChar());
		}
		return builder.toString();
	}

	public static void main(String[] args) {

		File parent = new File( "src\\de\\sgollmer\\solvismax\\dokus\\images");

		File file = new File(parent, "bildschirmschoner.png");

		BufferedImage bufferedImage = null;
		try {
			bufferedImage = ImageIO.read(file);
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

		MyImage myImage = new MyImage(bufferedImage);
		myImage.convertToBlackWhite(true);
		myImage.shrink();
		OcrField field = new OcrField(myImage, new Coordinate(73, 0), new Coordinate(139, 20));

		String uhrzeit = field.getString();

		field = new OcrField(myImage, new Coordinate(77, 21), new Coordinate(136, 33));

		String date = field.getString();

		System.out.println("Time is: " + uhrzeit + ", date is " + date);

		file = new File(parent, "Home.png");

		try {
			bufferedImage = ImageIO.read(file);
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

		myImage = new MyImage(bufferedImage);

		field = new OcrField(myImage, new Coordinate(82, 10), new Coordinate(155, 47));

		String temperature = field.getString();

		field = new OcrField(myImage, new Coordinate(85, 50), new Coordinate(103, 73));

		String adjust = field.getString();

		field = new OcrField(myImage, new Coordinate(190, 61), new Coordinate(237, 73));

		uhrzeit = field.getString();

		field = new OcrField(myImage, new Coordinate(190, 80), new Coordinate(237, 91));

		date = field.getString();

		System.out.println("Time is: " + uhrzeit + ", date is " + date + ", temperature is " + temperature
				+ ", adjustment is " + adjust);

		file = new File(parent, "raumeinfluss.png");

		try {
			bufferedImage = ImageIO.read(file);
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

		myImage = new MyImage(bufferedImage);

		field = new OcrField(myImage, new Coordinate(162, 0), new Coordinate(200, 14));

		String screenId = field.getString();

		field = new OcrField(myImage, new Coordinate(150, 75), new Coordinate(190, 88));

		String raumeinfluss = field.getString();

		System.out.println("ScreenId is: " + screenId + ", raumeinfluss is " + raumeinfluss);

	}

}
