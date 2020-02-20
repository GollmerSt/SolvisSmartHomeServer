/************************************************************************
 * 
 * $Id$
 *
 * 
 ************************************************************************/

package de.sgollmer.solvismax.model.objects.screen;

import java.util.regex.Matcher;

import javax.xml.namespace.QName;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import de.sgollmer.solvismax.error.XmlError;
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

	private static final java.util.regex.Pattern TIME_PATTERN = java.util.regex.Pattern.compile("\\d+:\\d+");
	private static final java.util.regex.Pattern DATE_PATTERN = java.util.regex.Pattern
			.compile("\\d+\\.\\d+\\.\\d\\d\\d\\d");

	private static final String XML_RESET_SCREEN_SAVER = "ResetScreenSaver";
	private static final String XML_TIME_TOP_LEFT = "TimeTopLeft";
	private static final String XML_TIME_BOTTOM_LEFT = "TimeBottomLeft";
	private static final String XML_DATE_TOP_LEFT = "DateTopLeft";
	private static final String XML_DATE_BOTTOM_LEFT = "DateBottomLeft";

	private static final Logger logger = LogManager.getLogger(ScreenSaver.class);

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
		if (resetScreenSaver != null) {
			this.resetScreenSaver.assign(description);
		}
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
			return new ScreenSaver(this.timeTopLeft, this.timeBottomLeft, this.dateTopLeft, this.dateBottomLeft,
					this.resetScreenSaver);
		}

		@Override
		public void created(CreatorByXML<?> creator, Object created) {
			switch (creator.getId()) {
				case XML_TIME_TOP_LEFT:
					this.timeTopLeft = (Coordinate) created;
					this.createdIds |= 1;
					break;
				case XML_TIME_BOTTOM_LEFT:
					this.timeBottomLeft = (Coordinate) created;
					this.createdIds |= 2;
					break;
				case XML_DATE_TOP_LEFT:
					this.dateTopLeft = (Coordinate) created;
					this.createdIds |= 4;
					break;
				case XML_DATE_BOTTOM_LEFT:
					this.dateBottomLeft = (Coordinate) created;
					this.createdIds |= 8;
					break;
				case XML_RESET_SCREEN_SAVER:
					this.resetScreenSaver = (TouchPoint) created;
					this.createdIds |= 16;
					break;
			}

		}

		@Override
		public CreatorByXML<?> getCreator(QName name) {
			String id = name.getLocalPart();
			switch (id) {
				case XML_DATE_BOTTOM_LEFT:
				case XML_DATE_TOP_LEFT:
				case XML_TIME_BOTTOM_LEFT:
				case XML_TIME_TOP_LEFT:
					return new Coordinate.Creator(id, this.getBaseCreator());
				case XML_RESET_SCREEN_SAVER:
					return new TouchPoint.Creator(id, this.getBaseCreator());
			}
			return null;
		}
	}

	public boolean is(SolvisScreen screen) {
		Pattern pattern = new Pattern(SolvisScreen.getImage(screen));

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
		logger.debug("Screen saver time: " + time + "  " + date);
		m = DATE_PATTERN.matcher(date);
		if (!m.matches()) {
			return false;
		}
		return true;
	}

}
