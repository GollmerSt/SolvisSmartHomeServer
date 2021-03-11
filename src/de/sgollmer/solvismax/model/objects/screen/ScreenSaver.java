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

import de.sgollmer.solvismax.Constants;
import de.sgollmer.solvismax.Constants.Debug;
import de.sgollmer.solvismax.error.AssignmentException;
import de.sgollmer.solvismax.imagepatternrecognition.image.MyImage;
import de.sgollmer.solvismax.imagepatternrecognition.ocr.OcrRectangle;
import de.sgollmer.solvismax.imagepatternrecognition.pattern.Pattern;
import de.sgollmer.solvismax.log.LogManager;
import de.sgollmer.solvismax.log.LogManager.ILogger;
import de.sgollmer.solvismax.model.Solvis;
import de.sgollmer.solvismax.model.objects.IAssigner;
import de.sgollmer.solvismax.model.objects.SolvisDescription;
import de.sgollmer.solvismax.model.objects.TouchPoint;
import de.sgollmer.solvismax.objects.Coordinate;
import de.sgollmer.solvismax.objects.Rectangle;
import de.sgollmer.xmllibrary.BaseCreator;
import de.sgollmer.xmllibrary.CreatorByXML;
import de.sgollmer.xmllibrary.XmlException;

public class ScreenSaver implements IAssigner {

	private static final java.util.regex.Pattern TIME_PATTERN = java.util.regex.Pattern.compile("\\d+:\\d+");
	private static final java.util.regex.Pattern DATE_PATTERN = java.util.regex.Pattern
			.compile("\\d+\\.\\d+\\.\\d\\d\\d\\d");

	private static final String XML_RESET_SCREEN_SAVER = "ResetScreenSaver";
	private static final String XML_TIME_DATA_RECTANGLE = "TimeDateRectangle";
	private static final String XML_MAX_GRAFIC_SIZE = "MaxGraficSize";

	private static final ILogger logger = LogManager.getInstance().getLogger(ScreenSaver.class);

	private final int timeHeight;
	private final Rectangle timeDateRectangle;
	private final Coordinate maxGraficSize;
	private final TouchPoint resetScreenSaver;

	private ScreenSaver(int timeHeight, Rectangle timeDateRectangle, Coordinate maxGraficSize,
			TouchPoint resetScreenSaver) {
		this.timeHeight = timeHeight;
		this.timeDateRectangle = timeDateRectangle;
		this.resetScreenSaver = resetScreenSaver;
		this.maxGraficSize = maxGraficSize;
	}

	@Override
	public void assign(SolvisDescription description) throws AssignmentException {
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

	public Exec createExecutable(Solvis solvis) {
		return new Exec(solvis);
	}

	public static class Creator extends CreatorByXML<ScreenSaver> {

		private int timeHeight;
		private Rectangle timeDateRectangle;
		private TouchPoint resetScreenSaver = null;
		private Coordinate maxGraficSize;

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

		public ScreenSaver create() throws XmlException {
			return new ScreenSaver(this.timeHeight, this.timeDateRectangle, this.maxGraficSize, this.resetScreenSaver);
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
				case XML_MAX_GRAFIC_SIZE:
					this.maxGraficSize = (Coordinate) created;
					break;
			}

		}

		@Override
		public CreatorByXML<?> getCreator(QName name) {
			String id = name.getLocalPart();
			switch (id) {
				case XML_TIME_DATA_RECTANGLE:
					return new Rectangle.Creator(XML_TIME_DATA_RECTANGLE, id, this.getBaseCreator());
				case XML_RESET_SCREEN_SAVER:
					return new TouchPoint.Creator(id, this.getBaseCreator());
				case XML_MAX_GRAFIC_SIZE:
					return new Coordinate.Creator(id, this.getBaseCreator());
			}
			return null;
		}
	}

	public enum State {
		NONE, SCREENSAVER, POSSIBLE
	}

	public class Exec {

		private final int frameThickness;
		private String debugInfo = "";
		private Coordinate margins = null;
		private PositionState lastState = PositionState.NONE;
		private long firstOutsideTime = -1;

		private Exec(Solvis solvis) {
			if (solvis != null) {
				this.frameThickness = solvis.getUnit().getIgnoredFrameThicknesScreenSaver();
			} else {
				this.frameThickness = Constants.SCREEN_SAVER_IGNORED_FRAME_SIZE;
			}

		}

		public State getSaverState(SolvisScreen screen) {
			MyImage original = SolvisScreen.getImage(screen);
			return this.getSaverState(original);
		}

		private State getSaverState(MyImage image) {

			if (image == null) {
				return State.NONE;
			}
			Rectangle scanArea;

			int fs = this.frameThickness;
			scanArea = new Rectangle(new Coordinate(fs, fs),
					new Coordinate(image.getWidth() - 2 * fs, image.getHeight() - 2 * fs));

			Pattern pattern = new Pattern(image, scanArea);

			PositionState state = this.isInPicture(pattern, scanArea);

			switch (state) {
				case INSIDE:
					this.lastState = PositionState.INSIDE;
					this.firstOutsideTime = -1;
					break;
				case NONE:
					this.lastState = PositionState.NONE;
					this.firstOutsideTime = -1;
					return State.NONE;
				case OUTSIDE:
					long now = System.currentTimeMillis();
					if (this.firstOutsideTime < 0) {
						this.firstOutsideTime = now;
					} else if (now - this.firstOutsideTime > Constants.MAX_OUTSIDE_TIME) {
						this.lastState = PositionState.NONE;
						return State.NONE;
					}
					return this.lastState == PositionState.NONE ? State.POSSIBLE : State.SCREENSAVER;
			}

			Coordinate timeTopLeft = ScreenSaver.this.timeDateRectangle.getTopLeft();
			Coordinate dateBottomRight = ScreenSaver.this.timeDateRectangle.getBottomRight();
			Coordinate check = new Coordinate(timeTopLeft.getX(), dateBottomRight.getY());

			this.lastState = PositionState.NONE;

			if (!pattern.isIn(check)) {
				if (Debug.SCREEN_SAVER_DETECTION) {
					this.debugInfo = "Not in <timeDateRectangle>. " + pattern.getDebugInfo();
				}
				return State.NONE;
			}

			dateBottomRight = new Coordinate(pattern.getWidth() - 1, dateBottomRight.getY());

			Rectangle timeDateRectangle = new Rectangle(timeTopLeft, dateBottomRight);

			Pattern timeDatePattern = new Pattern(pattern, timeDateRectangle);

			Rectangle timeRectangle = new Rectangle(new Coordinate(0, 0),
					new Coordinate(timeDatePattern.getWidth() - 1, ScreenSaver.this.timeHeight));

			String time = getContentsOfRectangle(timeDatePattern, timeRectangle, TIME_PATTERN);
			if (time == null) {
				return State.NONE;
			}

			Rectangle dateRectangle = new Rectangle(new Coordinate(0, ScreenSaver.this.timeHeight),
					new Coordinate(timeDatePattern.getWidth() - 1, timeDatePattern.getHeight() - 1));

			String date = getContentsOfRectangle(timeDatePattern, dateRectangle, DATE_PATTERN);
			if (date == null) {
				return State.NONE;
			}

			logger.debug("Screen saver time: " + time + "  " + date);

			this.createMargins(pattern);
			this.lastState = PositionState.INSIDE;
			return State.SCREENSAVER;
		}

		private String getContentsOfRectangle(Pattern timeDatePattern, Rectangle rectangle,
				java.util.regex.Pattern regex) {

			if (!timeDatePattern.isIn(rectangle.getBottomRight())) {
				if (Debug.SCREEN_SAVER_DETECTION) {
					this.debugInfo = "Not in <" + rectangle.getName() + ">. " + timeDatePattern.getDebugInfo();
				}
				return null;
			}

			OcrRectangle ocrRectangle = new OcrRectangle(timeDatePattern, rectangle);
			String time = ocrRectangle.getString();
			Matcher m = regex.matcher(time);
			if (!m.matches()) {
				if (Debug.SCREEN_SAVER_DETECTION) {
					this.debugInfo = timeDatePattern.getDebugInfo() + ", " + rectangle.getName() + " contents = "
							+ time;
				}
				return null;
			}
			return time;
		}

		@SuppressWarnings("unused")
		private String getDebugInfo() {
			return this.debugInfo;
		}

		private void createMargins(Pattern pattern) {
			if (this.margins == null) {

				if (pattern.getOrigin().getX() <= this.frameThickness) {
					return;
				}

				if (pattern.getOrigin().getY() <= this.frameThickness) {
					return;
				}

				if (pattern.getOrigin().getX() + ScreenSaver.this.maxGraficSize.getX() >= pattern.getImageSize().getX()
						- this.frameThickness) {
					return;
				}

				if (pattern.getOrigin().getY() + ScreenSaver.this.maxGraficSize.getY() >= pattern.getImageSize().getY()
						- this.frameThickness) {
					return;
				}

				this.margins = new Coordinate(pattern.getWidth(), pattern.getHeight());
			}
		}

		private PositionState isInPicture(Pattern pattern, Rectangle scanArea) {
			if (this.margins == null) {
				return PositionState.INSIDE;
			}
			if (pattern.getHeight() > this.margins.getY() + Constants.SCREEN_SAVER_HEIGHT_INACCURACY) {
				return PositionState.NONE;
			}
			if (pattern.getWidth() > this.margins.getX() + Constants.SCREEN_SAVER_WIDTH_INACCURACY) {
				return PositionState.NONE;
			}
			if (pattern.getWidth() < this.margins.getX() - Constants.SCREEN_SAVER_WIDTH_INACCURACY) {
				if (pattern.getOrigin().getX() > scanArea.getTopLeft().getX() //
						&& pattern.getOrigin().getX() + pattern.getWidth() < scanArea.getBottomRight().getX() + 1) {
					return PositionState.NONE;
				} else {
					return PositionState.OUTSIDE;
				}
			}
			if (pattern.getHeight() < this.margins.getY()) {
				if (pattern.getOrigin().getY() > scanArea.getTopLeft().getY() //
						&& pattern.getOrigin().getY() + pattern.getHeight() < scanArea.getBottomRight().getY() + 1) {
					return PositionState.NONE;
				} else {
					return PositionState.OUTSIDE;
				}
			}
			return PositionState.INSIDE;
		}

	}

	private enum PositionState {
		OUTSIDE, INSIDE, NONE
	}

	public static void main(String[] args) {
		Rectangle timeDateRectangle = new Rectangle(new Coordinate(75, 0), new Coordinate(135, 32));
		ScreenSaver saver = new ScreenSaver(18, timeDateRectangle, new Coordinate(150, 80), null);

		File parent = new File("testFiles\\images");

		final class Test {
			private final State soll;
			private final boolean newSaver;
			private final String name;

			private Test(State soll, boolean newSaver, String name) {
				this.soll = soll;
				this.newSaver = newSaver;
				this.name = name;
			}
		}

		Collection<Test> names = Arrays.asList( //
				new Test(State.SCREENSAVER, true, "Bildschirmschoner V1 Artefakte.bmp"), //
				new Test(State.SCREENSAVER, false, "Bildschirmschoner 2 V1 Artefakte.bmp"), //
				new Test(State.SCREENSAVER, false, "Bildschirmschoner V1.bmp"), //
				new Test(State.SCREENSAVER, false, "Bildschirmschoner V1 2.bmp"), //
				new Test(State.SCREENSAVER, false, "Bildschirmschoner V1 2 auﬂerhalb.bmp"), //
				new Test(State.SCREENSAVER, false, "Bildschirmschoner V1 auﬂerhalb.bmp"), //
				new Test(State.NONE, false, "raumeinfluss.png"), //
				new Test(State.POSSIBLE, false, "Bildschirmschoner V1 1 none.bmp"), //
				new Test(State.POSSIBLE, false, "Bildschirmschoner V1 3 auﬂerhalb.bmp"), //
				new Test(State.SCREENSAVER, false, "Bildschirmschoner V1 Artefakte.bmp"), //
				new Test(State.SCREENSAVER, false, "Bildschirmschoner V1 3.bmp"), //
				new Test(State.SCREENSAVER, false, "Bildschirmschoner V1 3 auﬂerhalb.bmp"), //
				new Test(State.SCREENSAVER, true, "bildschirmschoner.png"), //
				new Test(State.SCREENSAVER, false, "bildschirmschoner1.png"), //
				new Test(State.SCREENSAVER, false, "bildschirmschoner2.png"));

		BufferedImage image = null;

		ScreenSaver.Exec executable = saver.createExecutable(null);

		boolean failed = false;

		int i = 0;
		for (Iterator<Test> it = names.iterator(); it.hasNext();) {
			Test test = it.next();
			if (test.newSaver) {
				executable = saver.createExecutable(null);
			}
			File file = new File(parent, test.name);
			try {
				image = ImageIO.read(file);
			} catch (IOException e) {
				System.err.println("File: " + file.getName());
				e.printStackTrace();
			}

			MyImage myImage = new MyImage(image);

			State state = executable.getSaverState(myImage);

			boolean pass = state == test.soll;
			if (!pass) {
				failed = true;
			}

			System.out.println("" + ++i + ". " + file.getName() + " SolvisStatus: " + state.name() + ", Soll: "
					+ test.soll.name() + ", Check: " + pass);
		}

		if (failed) {
			System.err.println("Test failed");
		} else {
			System.out.println("Test pass");
		}

	}

}
