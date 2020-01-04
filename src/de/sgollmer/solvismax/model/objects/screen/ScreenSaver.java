/************************************************************************
 * 
 * $Id: 73_SolvisClient.pm 78 2020-01-03 17:50:08Z stefa $
 *
 * 
 ************************************************************************/

package de.sgollmer.solvismax.model.objects.screen;

import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.Collection;
import java.util.regex.Matcher;

import javax.imageio.ImageIO;
import javax.xml.namespace.QName;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

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
	private static java.util.regex.Pattern TIME_PATTERN = java.util.regex.Pattern.compile("\\d+:\\d+");
	private java.util.regex.Pattern DATE_PATTERN = java.util.regex.Pattern.compile("\\d+\\.\\d+\\.\\d\\d\\d\\d");
	
	private static final Logger logger = LogManager.getLogger(ScreenSaver.class) ;
			
	private final Coordinate timeTopLeft;
	private final Coordinate timeBottomLeft;
	private final Coordinate dateTopLeft;
	private final Coordinate dateBottomLeft;
	private final TouchPoint resetScreenSaver;

	public ScreenSaver(Coordinate timeTopLeft, Coordinate timeBottomLeft, Coordinate dateTopLeft,
			Coordinate dateBottomLeft, TouchPoint resetScreenSaver) {
		this.timeTopLeft = timeTopLeft;
		this.timeBottomLeft = timeBottomLeft;
		this.dateTopLeft = dateTopLeft;
		this.dateBottomLeft = dateBottomLeft;
		this.resetScreenSaver = resetScreenSaver;
	}

	@Override
	public void assign(SolvisDescription description) {
		this.resetScreenSaver.assign(description);
	}

	/**
	 * @return the resetScreenSaver
	 */
	public TouchPoint getResetScreenSaver() {
		return resetScreenSaver;
	}

	public static class Creator extends CreatorByXML<ScreenSaver> {

		private Coordinate timeTopLeft = null;
		private Coordinate timeBottomLeft = null;
		private Coordinate dateTopLeft = null;
		private Coordinate dateBottomLeft = null;
		private TouchPoint resetScreenSaver = null;

		private int createdIds = 0;

		private Collection<String> names = Arrays.asList("TimeTopLeft", "TimeBottomLeft", "DateTopLeft",
				"DateBottomLeft");

		public Creator(String id, BaseCreator<?> creator) {
			super(id, creator);
		}

		@Override
		public void setAttribute(QName name, String value) {
		}

		@Override

		public ScreenSaver create() throws XmlError {
			if (this.createdIds != 31) {
				throw new XmlError("Some coordinates of ScreenSaver not defined in the xml file");
			}
			return new ScreenSaver(this.timeTopLeft, this.timeBottomLeft, this.dateTopLeft, this.dateBottomLeft, this.resetScreenSaver);
		}

		@Override
		public void created(CreatorByXML<?> creator, Object created) {
			switch (creator.getId()) {
				case "TimeTopLeft":
					this.timeTopLeft = (Coordinate) created;
					this.createdIds |= 1;
					break;
				case "TimeBottomLeft":
					this.timeBottomLeft = (Coordinate) created;
					this.createdIds |= 2;
					break;
				case "DateTopLeft":
					this.dateTopLeft = (Coordinate) created;
					this.createdIds |= 4;
					break;
				case "DateBottomLeft":
					this.dateBottomLeft = (Coordinate) created;
					this.createdIds |= 8;
					break;
				case "ResetScreenSaver":
					this.resetScreenSaver = (TouchPoint) created;
					this.createdIds |= 16;
					break;
			}

		}

		@Override
		public CreatorByXML<?> getCreator(QName name) {
			String id = name.getLocalPart();
			if (names.contains(id)) {
				return new Coordinate.Creator(id, this.getBaseCreator());
			} else if ( id.equals( "ResetScreenSaver")) {
				return new TouchPoint.Creator(id, this.getBaseCreator()) ;
			}
			return null;
		}
	}

	public boolean is(MyImage image) {
		Pattern pattern = new Pattern(image);

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
		logger.debug( "Screen saver time: "+ time + "  " + date) ;
		m = DATE_PATTERN.matcher(date);
		if (!m.matches()) {
			return false;
		}
		return true;
	}

	public static void main(String[] args) {
		ScreenSaver saver = new ScreenSaver(new Coordinate(75, 0), new Coordinate(75, 20), new Coordinate(75, 21),
				new Coordinate(75, 33), null);

		File parent = new File("testFiles\\images");

		File file = new File(parent, "Bildschirmschoner2.png");

		BufferedImage bufferedImage = null;
		try {
			bufferedImage = ImageIO.read(file);
		} catch (IOException e) {
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
