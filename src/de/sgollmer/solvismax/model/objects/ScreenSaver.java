package de.sgollmer.solvismax.model.objects;

import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.regex.Matcher;

import javax.imageio.ImageIO;

import de.sgollmer.solvismax.imagepatternrecognition.image.MyImage;
import de.sgollmer.solvismax.imagepatternrecognition.ocr.OcrRectangle;
import de.sgollmer.solvismax.imagepatternrecognition.pattern.Pattern;
import de.sgollmer.solvismax.objects.Coordinate;
import de.sgollmer.solvismax.objects.Rectangle;

public class ScreenSaver {
	private static java.util.regex.Pattern TIME_PATTERN = java.util.regex.Pattern.compile("\\d+:\\d+");
	private java.util.regex.Pattern DATE_PATTERN = java.util.regex.Pattern.compile("\\d+\\.\\d+\\.\\d\\d\\d\\d");

	private final Coordinate timeTopLeft;
	private final Coordinate timeBottomLeft;
	private final Coordinate dateTopLeft;
	private final Coordinate dateBottomLeft;

	public ScreenSaver(Coordinate timeTopLeft, Coordinate timeBottomLeft, Coordinate dateTopLeft,
			Coordinate dateBottomLeft) {
		this.timeTopLeft = timeTopLeft;
		this.timeBottomLeft = timeBottomLeft;
		this.dateTopLeft = dateTopLeft;
		this.dateBottomLeft = dateBottomLeft;
	}

	public boolean is(MyImage image) {
		Pattern pattern = new Pattern(image.create(null));

		Rectangle timeRectangle = new Rectangle(timeTopLeft,
				new Coordinate(pattern.getWidth() - 1, timeBottomLeft.getY()));
		OcrRectangle ocrRectangle = new OcrRectangle(pattern, timeRectangle);
		String time = ocrRectangle.getString();
		Matcher m = TIME_PATTERN.matcher(time);
		if (!m.matches()) {
			return false;
		}

		Rectangle dateRectangle = new Rectangle(dateTopLeft,
				new Coordinate(pattern.getWidth() - 1, dateBottomLeft.getY()));
		ocrRectangle = new OcrRectangle(pattern, dateRectangle);
		String date = ocrRectangle.getString();
		System.out.print(time + "  ") ;
		System.out.println(date) ;
		m = DATE_PATTERN.matcher(date);
		if (!m.matches()) {
			return false;
		}
		return true;
	}

	public static void main(String[] args) {
		ScreenSaver saver = new ScreenSaver(new Coordinate(75, 0), new Coordinate(75, 20), new Coordinate(75, 21),
				new Coordinate(75, 33));

		File parent = new File("testFiles\\images");

		File file = new File(parent, "Bildschirmschoner1.png");

		BufferedImage bufferedImage = null;
		try {
			bufferedImage = ImageIO.read(file);
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

		MyImage myImage = new MyImage(bufferedImage);

		if (saver.is(myImage)) {
			System.out.println(file.getName() + " ist ein Screensaver");
		} else {
			System.out.println(file.getName() + " ist KEIN Screensaver");
		}

		file = new File(parent, "Home.png");

		bufferedImage = null;
		try {
			bufferedImage = ImageIO.read(file);
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		myImage = new MyImage(bufferedImage);

		if (saver.is(myImage)) {
			System.out.println(file.getName() + " ist ein Screensaver");
		} else {
			System.out.println(file.getName() + " ist KEIN Screensaver");
		}
	}
}
