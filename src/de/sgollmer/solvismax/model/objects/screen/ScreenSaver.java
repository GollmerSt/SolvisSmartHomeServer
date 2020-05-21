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
import de.sgollmer.solvismax.model.Solvis;
import de.sgollmer.solvismax.model.SolvisState;
import de.sgollmer.solvismax.model.objects.Assigner;
import de.sgollmer.solvismax.model.objects.Observer.ObserverI;
import de.sgollmer.solvismax.model.objects.SolvisDescription;
import de.sgollmer.solvismax.model.objects.TouchPoint;
import de.sgollmer.solvismax.objects.Coordinate;
import de.sgollmer.solvismax.objects.Rectangle;
import de.sgollmer.solvismax.xml.BaseCreator;
import de.sgollmer.solvismax.xml.CreatorByXML;

public class ScreenSaver implements Assigner {

	private static final int WIDTH_INACCURACY = 5;
	private static final int HEIGHT_INACCURACY = 5;
	private static final int MAX_OUTSIDE_TIME = 300000;

	private static boolean DEBUG = true;

	private static final java.util.regex.Pattern TIME_PATTERN = java.util.regex.Pattern.compile("\\d+:\\d+");
	private static final java.util.regex.Pattern DATE_PATTERN = java.util.regex.Pattern
			.compile("\\d+\\.\\d+\\.\\d\\d\\d\\d");

	private static final String XML_RESET_SCREEN_SAVER = "ResetScreenSaver";
	private static final String XML_TIME_DATA_RECTANGLE = "TimeDateRectangle";

	private static final Logger logger = LogManager.getLogger(ScreenSaver.class);

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

	public Executable createExecutable(Solvis solvis) {
		return new Executable(solvis);
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
					return new Rectangle.Creator(XML_TIME_DATA_RECTANGLE, id, this.getBaseCreator());
				case XML_RESET_SCREEN_SAVER:
					return new TouchPoint.Creator(id, this.getBaseCreator());
			}
			return null;
		}
	}

	public class Executable implements ObserverI<SolvisState> {

		private Rectangle scanArea = null;
		private String debugInfo = "";
		private Coordinate margins = null;
		private State lastState = State.NONE;
		private long firstOutsideTime = -1;

		public Executable(Solvis solvis) {
			if (solvis != null) {
				solvis.getSolvisState().register(this);
			}
		}

		@Override
		public void update(SolvisState data, Object source) {
			if (data.isError() || data.getState() == SolvisState.State.POWER_OFF) {
				this.scanArea = null;
				this.margins = null;
			}

		}

		public boolean is(SolvisScreen screen) {
			MyImage original = SolvisScreen.getImage(screen);
			return this.is(original);
		}

		public boolean is(MyImage image) {

			if (image == null) {
				return false;
			}

			Rectangle scanArea = this.scanArea;
			if (scanArea == null) {
				int fs = Constants.SCREEN_SAVER_IGNORED_FRAME_SIZE;
				scanArea = new Rectangle(new Coordinate(fs, fs),
						new Coordinate(image.getWidth() - 2 * fs, image.getHeight() - 2 * fs));
			}

			MyImage myImage = new MyImage(image);
			myImage.createHistograms(false);

			Pattern pattern = new Pattern(image, scanArea);

			State state = this.isInPicture(pattern);

			switch (state) {
				case NONE:
					this.lastState = State.NONE;
					return false;
				case OUTSIDE:
					if (this.lastState == State.NONE) {
						return false;
					}
					long now = System.currentTimeMillis();
					if (this.lastState == State.INSIDE) {
						this.firstOutsideTime = now;
					}
					if (now - this.firstOutsideTime > MAX_OUTSIDE_TIME) {
						this.lastState = State.NONE;
						return false;
					}
					return true;
			}

			Coordinate timeTopLeft = ScreenSaver.this.timeDateRectangle.getTopLeft();
			Coordinate dateBottomRight = ScreenSaver.this.timeDateRectangle.getBottomRight();
			Coordinate check = new Coordinate(timeTopLeft.getX(), dateBottomRight.getY());

			this.lastState = State.NONE;

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
					new Coordinate(timeDatePattern.getWidth() - 1, ScreenSaver.this.timeHeight));

			String time = getContentsOfRectangle(timeDatePattern, timeRectangle, TIME_PATTERN);
			if (time == null) {
				return false;
			}

			Rectangle dateRectangle = new Rectangle(new Coordinate(0, ScreenSaver.this.timeHeight),
					new Coordinate(timeDatePattern.getWidth() - 1, timeDatePattern.getHeight() - 1));

			String date = getContentsOfRectangle(timeDatePattern, dateRectangle, DATE_PATTERN);
			if (date == null) {
				return false;
			}
			
			logger.debug("Screen saver time: " + time + "  " + date);

			this.setScanArea(pattern);
			this.createMargins(pattern);
			this.lastState = State.INSIDE;
			return true;
		}

		private String getContentsOfRectangle(Pattern timeDatePattern, Rectangle rectangle,
				java.util.regex.Pattern regex) {

			if (!timeDatePattern.isIn(rectangle.getBottomRight())) {
				if (DEBUG) {
					this.debugInfo = "Not in <" + rectangle.getName() + ">. " + timeDatePattern.getDebugInfo();
				}
				return null;
			}

			OcrRectangle ocrRectangle = new OcrRectangle(timeDatePattern, rectangle);
			String time = ocrRectangle.getString();
			Matcher m = regex.matcher(time);
			if (!m.matches()) {
				if (DEBUG) {
					this.debugInfo = timeDatePattern.getDebugInfo() + ", " + rectangle.getName() + " contents = "
							+ time;
				}
				return null;
			}
			return time;
		}

		private void setScanArea(Pattern pattern) {
			if (this.scanArea != null) {
				return;
			}
			Coordinate origin = pattern.getOrigin();
			int fs = Constants.SCREEN_SAVER_IGNORED_FRAME_SIZE;

			if (origin.getX() <= fs || origin.getY() <= fs) {
				return;
			}

			MyImage screen = new MyImage(pattern.getImage());

			if (origin.getX() + pattern.getWidth() >= screen.getWidth() - fs - 1
					|| origin.getY() + pattern.getHeight() >= screen.getHeight() - fs - 1) {
				return;
			}

			screen.createHistograms(false);

			int left = 0;
			int right = screen.getWidth() - 1;
			int top = 0;
			int bottom = screen.getHeight() - 1;

			for (int i = 0; i < fs; ++i) {
				if (screen.getHistogramX().get(i) > 0) {
					left = i + 1;
				}
			}

			for (int i = screen.getWidth() - 1; i >= screen.getWidth() - fs; --i) {
				if (screen.getHistogramX().get(i) > 0) {
					right = i - 1;
				}
			}

			for (int i = 0; i < fs; ++i) {
				if (screen.getHistogramY().get(i) > 0) {
					top = i + 1;
				}
			}

			for (int i = screen.getHeight() - 1; i >= screen.getHeight() - fs; --i) {
				if (screen.getHistogramY().get(i) > 0) {
					bottom = i - 1;
				}
			}

			this.scanArea = new Rectangle(new Coordinate(left, top), new Coordinate(right, bottom));
		}

		public String getDebugInfo() {
			return this.debugInfo;
		}

		public void createMargins(Pattern pattern) {
			if (this.margins == null) {
				this.margins = new Coordinate(pattern.getWidth(), pattern.getHeight());
			}
		}

		public State isInPicture(Pattern pattern) {
			if (this.margins == null) {
				return State.INSIDE;
			}
			if (pattern.getHeight() > this.margins.getY() + HEIGHT_INACCURACY) {
				return State.NONE;
			}
			if (pattern.getWidth() > this.margins.getX() + WIDTH_INACCURACY) {
				return State.NONE;
			}
			if (pattern.getWidth() < this.margins.getX() - WIDTH_INACCURACY) {
				if (pattern.getOrigin().getX() > this.scanArea.getTopLeft().getX() //
						&& pattern.getOrigin().getX() + pattern.getWidth() < this.scanArea.getBottomRight().getX()
								+ 1) {
					return State.NONE;
				} else {
					return State.OUTSIDE;
				}
			}
			if (pattern.getHeight() < this.margins.getY()) {
				if (pattern.getOrigin().getY() > this.scanArea.getTopLeft().getY() //
						&& pattern.getOrigin().getY() + pattern.getHeight() < this.scanArea.getBottomRight().getY()
								+ 1) {
					return State.NONE;
				} else {
					return State.OUTSIDE;
				}
			}
			return State.INSIDE;
		}

	}

	public enum State {
		OUTSIDE, INSIDE, NONE
	}

	public static void main(String[] args) {
		Rectangle timeDateRectangle = new Rectangle(new Coordinate(75, 0), new Coordinate(135, 32));
		ScreenSaver saver = new ScreenSaver(18, timeDateRectangle, null);

		File parent = new File("testFiles\\images");

		final class Test {
			private final boolean newSaver;
			private final String name;

			public Test(boolean newSaver, String name) {
				this.newSaver = newSaver;
				this.name = name;
			}
		}

		Collection<Test> names = Arrays.asList(new Test(true, "Bildschirmschoner V1 Artefakte.bmp"),
				new Test(false, "Bildschirmschoner 2 V1 Artefakte.bmp"), new Test(true, "Bildschirmschoner V1.bmp"),
				new Test(false, "Bildschirmschoner V1 2.bmp"), new Test(false, "Bildschirmschoner V1 2 auﬂerhalb.bmp"),
				new Test(false, "Bildschirmschoner V1 auﬂerhalb.bmp"),
				new Test(false, "Bildschirmschoner V1 1 none.bmp"),
				new Test(false, "Bildschirmschoner V1 3 auﬂerhalb.bmp"), new Test(false, "Bildschirmschoner V1 3.bmp"),
				new Test(false, "Bildschirmschoner V1 3 auﬂerhalb.bmp"), new Test(true, "bildschirmschoner.png"),
				new Test(false, "bildschirmschoner1.png"), new Test(false, "bildschirmschoner2.png"));

		BufferedImage image = null;

		ScreenSaver.Executable executable = saver.createExecutable(null);

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

			boolean state = executable.is(myImage);

			System.out.println(file.getName() + " isScreenSaver? " + Boolean.toString(state));
		}

	}

}
