/************************************************************************
 * 
 * $Id$
 *
 * 
 ************************************************************************/

package de.sgollmer.solvismax.model.objects.screen;

import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.Collection;
import java.util.Iterator;
import java.util.regex.Matcher;

import javax.imageio.ImageIO;
import javax.xml.namespace.QName;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import de.sgollmer.solvismax.Constants;
import de.sgollmer.solvismax.error.XmlError;
import de.sgollmer.solvismax.imagepatternrecognition.image.MyImage;
import de.sgollmer.solvismax.imagepatternrecognition.ocr.OcrRectangle;
import de.sgollmer.solvismax.imagepatternrecognition.pattern.Pattern;
import de.sgollmer.solvismax.model.objects.Assigner;
import de.sgollmer.solvismax.model.objects.SolvisDescription;
import de.sgollmer.solvismax.model.objects.TouchPoint;
import de.sgollmer.solvismax.objects.Coordinate;
import de.sgollmer.solvismax.objects.Rectangle;
import de.sgollmer.solvismax.xml.BaseCreator;
import de.sgollmer.solvismax.xml.CreatorByXML;

public class ScreenSaver implements Assigner {

	private static final boolean DEBUG = false;

	private static final java.util.regex.Pattern TIME_PATTERN = java.util.regex.Pattern.compile("\\d+:\\d+");
	private static final java.util.regex.Pattern DATE_PATTERN = java.util.regex.Pattern
			.compile("\\d+\\.\\d+\\.\\d\\d\\d\\d");

	private static final String XML_RESET_SCREEN_SAVER = "ResetScreenSaver";
	private static final String XML_TIME_DATA_RECTANGLE = "TimeDateRectangle";

	private static final Logger logger = LogManager.getLogger(ScreenSaver.class);

	private String debugInfo = "";

	private final int timeHeight;
	private final Rectangle timeDateRectangle;
	private final TouchPoint resetScreenSaver;

	public ScreenSaver(int timeHeight, Rectangle timeDateRectangle, TouchPoint resetScreenSaver) {
		this.timeHeight = timeHeight;
		this.timeDateRectangle = timeDateRectangle;
		this.resetScreenSaver = resetScreenSaver;
	}

	@Override
	public void assign(SolvisDescription description) {
		if (this.resetScreenSaver != null) {
			this.resetScreenSaver.assign(description);
		}
	}

	/**
	 * @return the resetScreenSaver
	 */
	public TouchPoint getResetScreenSaver() {
		return this.resetScreenSaver;
	}

	public static class Creator extends CreatorByXML<ScreenSaver> {

		private int timeHeight;
		private Rectangle timeDateRectangle;
		private TouchPoint resetScreenSaver = null;

		public Creator(String id, BaseCreator<?> creator) {
			super(id, creator);
		}

		@Override
		public void setAttribute(QName name, String value) {
			switch (name.getLocalPart()) {
				case "timeHeight":
					this.timeHeight = Integer.parseInt(value);
					break;
			}
		}

		@Override

		public ScreenSaver create() throws XmlError {
			return new ScreenSaver(this.timeHeight, this.timeDateRectangle, this.resetScreenSaver);
		}

		@Override
		public void created(CreatorByXML<?> creator, Object created) {
			switch (creator.getId()) {
				case XML_TIME_DATA_RECTANGLE:
					this.timeDateRectangle = (Rectangle) created;
					break;
				case XML_RESET_SCREEN_SAVER:
					this.resetScreenSaver = (TouchPoint) created;
					break;
			}

		}

		@Override
		public CreatorByXML<?> getCreator(QName name) {
			String id = name.getLocalPart();
			switch (id) {
				case XML_TIME_DATA_RECTANGLE:
					return new Rectangle.Creator(id, this.getBaseCreator());
				case XML_RESET_SCREEN_SAVER:
					return new TouchPoint.Creator(id, this.getBaseCreator());
			}
			return null;
		}
	}

	public boolean is(SolvisScreen screen) {
		MyImage original = SolvisScreen.getImage(screen);
		return this.is(original);
	}

	public boolean is(MyImage image) {

		int fs = Constants.SCREEN_SAVER_IGNORED_FRAME_SIZE;

		if (image == null) {
			return false;
		}

		Pattern pattern = new Pattern(image, new Coordinate(fs, fs),
				new Coordinate(image.getWidth() - 2 * fs, image.getHeight() - 2 * fs));

		Coordinate timeTopLeft = this.timeDateRectangle.getTopLeft();
		Coordinate dateBottomRight = this.timeDateRectangle.getBottomRight();
		Coordinate check = new Coordinate(timeTopLeft.getX(), dateBottomRight.getY() );

		if (!pattern.isIn(check)) {
			if (DEBUG) {
				this.debugInfo = "Not in <timeDateRectangle>. " + pattern.getDebugInfo();
			}
			return false;
		}

		dateBottomRight = new Coordinate(pattern.getWidth() - 1, dateBottomRight.getY());

		Rectangle timeDateRectangle = new Rectangle(timeTopLeft, dateBottomRight);

		Pattern timeDatePattern = new Pattern(pattern, timeDateRectangle);

		Rectangle timeRectangle = new Rectangle(new Coordinate(0, 0),
				new Coordinate(timeDatePattern.getWidth() - 1, this.timeHeight));

		if (!timeDatePattern.isIn(timeRectangle.getBottomRight())) {
			if (DEBUG) {
				this.debugInfo = "Not in <timeRectangle>. " + pattern.getDebugInfo();
			}
			return false;
		}

		OcrRectangle ocrRectangle = new OcrRectangle(timeDatePattern, timeRectangle);
		String time = ocrRectangle.getString();
		Matcher m = TIME_PATTERN.matcher(time);
		if (!m.matches()) {
			if (DEBUG) {
				this.debugInfo = pattern.getDebugInfo() + ", time = " + time;
			}
			return false;
		}

		Rectangle dateRectangle = new Rectangle(new Coordinate(0, this.timeHeight),
				new Coordinate(timeDatePattern.getWidth() - 1, timeDatePattern.getHeight() - 1));

		if (!timeDatePattern.isIn(dateRectangle.getBottomRight())) {
			if (DEBUG) {
				this.debugInfo = "Not in <dateRectangle>. " + pattern.getDebugInfo();
			}
			return false;
		}

		ocrRectangle = new OcrRectangle(timeDatePattern, dateRectangle);
		String date = ocrRectangle.getString();
		logger.debug("Screen saver time: " + time + "  " + date);
		m = DATE_PATTERN.matcher(date);
		if (!m.matches()) {
			if (DEBUG) {
				this.debugInfo = pattern.getDebugInfo() + ", date = " + date;
			}
			return false;
		}
		return true;
	}

	public String getDebugInfo() {
		return this.debugInfo;
	}

	public static void main(String[] args) {
		Rectangle timeDateRectangle = new Rectangle(new Coordinate(75, 0), new Coordinate(135, 32));
		ScreenSaver saver = new ScreenSaver(18, timeDateRectangle, null);

		File parent = new File("testFiles\\images");

		Collection<String> names = Arrays.asList("Bildschirmschoner V1.bmp", "Bildschirmschoner V1 2.bmp", "bildschirmschoner.png",
				"bildschirmschoner1.png", "bildschirmschoner2.png");

		BufferedImage image = null;

		for (Iterator<String> it = names.iterator(); it.hasNext();) {
			File file = new File(parent, it.next());
			try {
				image = ImageIO.read(file);
			} catch (IOException e) {
				System.err.println("File: " + file.getName());
				e.printStackTrace();
			}

			MyImage myImage = new MyImage(image);

			boolean isScreenSaver = saver.is(myImage);

			System.out.println(file.getName() + " isScreenSaver? " + Boolean.toString(isScreenSaver));
		}

	}

}
