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
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.regex.Matcher;

import javax.imageio.ImageIO;
import javax.xml.namespace.QName;

import de.sgollmer.solvismax.Constants;
import de.sgollmer.solvismax.Constants.Debug;
import de.sgollmer.solvismax.imagepatternrecognition.image.MyImage;
import de.sgollmer.solvismax.imagepatternrecognition.ocr.OcrRectangle;
import de.sgollmer.solvismax.imagepatternrecognition.pattern.Pattern;
import de.sgollmer.solvismax.log.LogManager;
import de.sgollmer.solvismax.log.LogManager.ILogger;
import de.sgollmer.solvismax.model.Solvis;
import de.sgollmer.solvismax.model.objects.TouchPoint;
import de.sgollmer.solvismax.objects.Coordinate;
import de.sgollmer.solvismax.objects.Rectangle;
import de.sgollmer.xmllibrary.BaseCreator;
import de.sgollmer.xmllibrary.CreatorByXML;
import de.sgollmer.xmllibrary.XmlException;

public class ScreenSaver {

	private static final java.util.regex.Pattern TIME_PATTERN = java.util.regex.Pattern.compile("\\d+?:\\d+?");
	private static final java.util.regex.Pattern DATE_PATTERN = java.util.regex.Pattern
			.compile("\\d+?\\.\\d+?\\.\\d\\d\\d\\d");

	private static final String XML_RESET_SCREEN_SAVER = "ResetScreenSaver";
	private static final String XML_MAX_GRAFIC_SIZE = "MaxGraficSize";

	private static final ILogger logger = LogManager.getInstance().getLogger(ScreenSaver.class);

	private final int xCoordinateWithinTimedate;

	private final Coordinate maxGraficSize;
	private final TouchPoint resetScreenSaver;

	private ScreenSaver(final int xCoordinateWithinTimedate, Coordinate maxGraficSize, TouchPoint resetScreenSaver) {
		this.xCoordinateWithinTimedate = xCoordinateWithinTimedate;
		this.resetScreenSaver = resetScreenSaver;
		this.maxGraficSize = maxGraficSize;
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

		private int xCoordinateWithinTimedate;
		private TouchPoint resetScreenSaver = null;
		private Coordinate maxGraficSize;

		public Creator(String id, BaseCreator<?> creator) {
			super(id, creator);
		}

		@Override
		public void setAttribute(QName name, String value) {
			switch (name.getLocalPart()) {
				case "xCoordinateWithinTimedate":
					this.xCoordinateWithinTimedate = Integer.parseInt(value);
					break;
			}
		}

		@Override

		public ScreenSaver create() throws XmlException {
			return new ScreenSaver(this.xCoordinateWithinTimedate, this.maxGraficSize, this.resetScreenSaver);
		}

		@Override
		public void created(CreatorByXML<?> creator, Object created) {
			switch (creator.getId()) {
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
				case XML_RESET_SCREEN_SAVER:
					return new TouchPoint.Creator(id, this.getBaseCreator());
				case XML_MAX_GRAFIC_SIZE:
					return new Coordinate.Creator(id, this.getBaseCreator());
			}
			return null;
		}
	}

	public enum SaverEvent {
		NONE, //
		SCREENSAVER, //
		POSSIBLE
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
				this.frameThickness = Constants.SCREEN_IGNORED_FRAME_SIZE;
			}

		}

		public SaverEvent getSaverState(SolvisScreen screen) {
			MyImage original = SolvisScreen.getImage(screen);
			return this.getSaverState(original);
		}

		private SaverEvent getSaverState(MyImage image) {

			if (image == null) {
				return SaverEvent.NONE;
			}
			Rectangle scanArea;

			int fs = this.frameThickness;
			scanArea = new Rectangle(new Coordinate(fs, fs),
					new Coordinate(image.getWidth() - 2 * fs - 1, image.getHeight() - 2 * fs - 1));

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
					return SaverEvent.NONE;
				case OUTSIDE:
					long now = System.currentTimeMillis();
					if (this.firstOutsideTime < 0) {
						this.firstOutsideTime = now;
					} else if (now - this.firstOutsideTime > Constants.MAX_OUTSIDE_TIME) {
						this.lastState = PositionState.NONE;
						return SaverEvent.NONE;
					}
					return this.lastState == PositionState.NONE ? SaverEvent.POSSIBLE : SaverEvent.SCREENSAVER;
			}

			Coordinate search = new Coordinate(ScreenSaver.this.xCoordinateWithinTimedate, 0);
			int timeTop = pattern.searchFramefromOuter(search, pattern.getWidth() - 1, false, true);
			search = new Coordinate(ScreenSaver.this.xCoordinateWithinTimedate, timeTop);
			int timeBottom = pattern.searchFramefromInner(search, pattern.getWidth() - 1, false, true);

			String time = this.getContents(pattern, timeTop, timeBottom, TIME_PATTERN, 5);

			if (time == null) {
				return SaverEvent.NONE;
			}

			search = new Coordinate(ScreenSaver.this.xCoordinateWithinTimedate, timeBottom + 1);
			int dateTop = pattern.searchFramefromOuter(search, pattern.getWidth() - 1, false, true);
			search = new Coordinate(ScreenSaver.this.xCoordinateWithinTimedate, dateTop);
			int dateBottom = pattern.searchFramefromInner(search, pattern.getWidth() - 1, false, true);

			String date = this.getContents(pattern, dateTop, dateBottom, DATE_PATTERN, 10);

			if (date == null) {
				return SaverEvent.NONE;
			}

			logger.debug("Screen saver time: " + time + "  " + date);

			this.createMargins(pattern);
			this.lastState = PositionState.INSIDE;
			return SaverEvent.SCREENSAVER;
		}

		private String getContents(Pattern pattern, int topY, int bottomY, java.util.regex.Pattern regex, int charCnt) {
			Pattern p = new Pattern(pattern,
					new Rectangle(new Coordinate(0, topY), new Coordinate(pattern.getWidth() - 1, bottomY)));

			List<MyImage> list = p.split();
			if (list.size() < charCnt) {
				return null;
			}

			Collection<MyImage> ocrImages = new ArrayList<>(charCnt);
			for (int i = 0; i < charCnt; ++i) {
				ocrImages.add(list.get(i + list.size() - charCnt));
			}
			String value = new OcrRectangle(pattern, ocrImages).getString();

			Matcher m = regex.matcher(value);
			if (!m.matches()) {
				if (Debug.SCREEN_SAVER_DETECTION) {
					this.debugInfo = pattern.getDebugInfo() + " contents = " + value;
				}
				return null;
			}

			return value;
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
		ScreenSaver saver = new ScreenSaver(80, new Coordinate(150, 80), null);

		File parent = new File("testFiles\\images");

		final class Test {
			private final SaverEvent soll;
			private final boolean newSaver;
			private final String name;

			private Test(SaverEvent soll, boolean newSaver, String name) {
				this.soll = soll;
				this.newSaver = newSaver;
				this.name = name;
			}
		}

		Collection<Test> names = Arrays.asList( //
				new Test(SaverEvent.SCREENSAVER, true, "bildschirmschoner Max7.bmp"), //
				new Test(SaverEvent.SCREENSAVER, true, "Bildschirmschoner V1 Artefakte.bmp"), //
				new Test(SaverEvent.SCREENSAVER, false, "Bildschirmschoner 2 V1 Artefakte.bmp"), //
				new Test(SaverEvent.SCREENSAVER, false, "Bildschirmschoner V1.bmp"), //
				new Test(SaverEvent.SCREENSAVER, false, "Bildschirmschoner V1 2.bmp"), //
				new Test(SaverEvent.SCREENSAVER, false, "Bildschirmschoner V1 2 auﬂerhalb.bmp"), //
				new Test(SaverEvent.SCREENSAVER, false, "Bildschirmschoner V1 auﬂerhalb.bmp"), //
				new Test(SaverEvent.NONE, false, "raumeinfluss.png"), //
				new Test(SaverEvent.POSSIBLE, false, "Bildschirmschoner V1 1 none.bmp"), //
				new Test(SaverEvent.POSSIBLE, false, "Bildschirmschoner V1 3 auﬂerhalb.bmp"), //
				new Test(SaverEvent.SCREENSAVER, false, "Bildschirmschoner V1 Artefakte.bmp"), //
				new Test(SaverEvent.SCREENSAVER, false, "Bildschirmschoner V1 3.bmp"), //
				new Test(SaverEvent.SCREENSAVER, false, "Bildschirmschoner V1 3 auﬂerhalb.bmp"), //
				new Test(SaverEvent.SCREENSAVER, true, "bildschirmschoner.png"), //
				new Test(SaverEvent.SCREENSAVER, false, "bildschirmschoner1.png"), //
				new Test(SaverEvent.SCREENSAVER, false, "bildschirmschoner2.png"));

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

			SaverEvent state = executable.getSaverState(myImage);

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
