/************************************************************************
 * 
 * $Id$
 *
 * 
 ************************************************************************/

package de.sgollmer.solvismax.model.objects;

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
import de.sgollmer.solvismax.error.TypeException;
import de.sgollmer.solvismax.helper.FileHelper;
import de.sgollmer.solvismax.imagepatternrecognition.image.MyImage;
import de.sgollmer.solvismax.imagepatternrecognition.ocr.OcrRectangle;
import de.sgollmer.solvismax.log.LogManager;
import de.sgollmer.solvismax.log.LogManager.ILogger;
import de.sgollmer.solvismax.model.Instances;
import de.sgollmer.solvismax.model.Solvis;
import de.sgollmer.solvismax.model.SolvisState.SolvisErrorInfo;
import de.sgollmer.solvismax.model.objects.Observer.IObserver;
import de.sgollmer.solvismax.model.objects.data.SolvisData;
import de.sgollmer.solvismax.model.objects.screen.SolvisScreen;
import de.sgollmer.solvismax.objects.Coordinate;
import de.sgollmer.solvismax.objects.Range;
import de.sgollmer.solvismax.objects.Rectangle;
import de.sgollmer.xmllibrary.BaseCreator;
import de.sgollmer.xmllibrary.CreatorByXML;
import de.sgollmer.xmllibrary.XmlException;

public class ErrorDetection {

	private static final ILogger logger = LogManager.getInstance().getLogger(ErrorDetection.class);

	private static final String XML_LEFT_BORDER = "LeftBorder";
	private static final String XML_RIGHT_BORDER = "RightBorder";
	private static final String XML_TOP_BORDER = "TopBorder";
	private static final String XML_MIDDLE_BORDER = "MiddleBorder";
	private static final String XML_BOTTOM_BORDER = "BottomBorder";
	private static final String XML_HH_MM = "HhMm";
	private static final String XML_DD_MM_YY = "DdMmYy";
	private static final String XML_ERROR_CONDITION = "ErrorCondition";

	private static final java.util.regex.Pattern TIME_PATTERN = java.util.regex.Pattern.compile("\\d+:\\d+");
	private static final java.util.regex.Pattern DATE_PATTERN = java.util.regex.Pattern.compile("\\d+\\.\\d+\\.\\d\\d");

	private final Range leftBorder;
	private final Range rightBorder;
	private final Range topBorder;
	private final Range middleBorder;
	private final Range bottomBorder;
	private final Rectangle hhMm;
	private final Rectangle ddMmYy;
	private final Collection<ErrorCondition> errorConditions;

	private ErrorDetection(final Range leftBorder, final Range rightBorder, final Range topBorder,
			final Range middleBorder, final Range bottomBorder, final Rectangle hhMm, final Rectangle ddMmYy,
			final Collection<ErrorCondition> errorConditions) {
		this.leftBorder = leftBorder;
		this.rightBorder = rightBorder;
		this.topBorder = topBorder;
		this.middleBorder = middleBorder;
		this.bottomBorder = bottomBorder;
		this.hhMm = hhMm;
		this.ddMmYy = ddMmYy;
		this.errorConditions = errorConditions;
	}

	public enum Type {
		NONE, //
		HOME_NONE, // Home without error button
		MESSAGE_BOX, //
		ERROR_BUTTON
	}

	public Type getType(final SolvisScreen screen) {
		return this.getType(screen, false);
	}

	private Type getType(final SolvisScreen screen, final boolean isHome) {

		MyImage image = screen.getImage();

		image.createHistograms(false);

		int tresholdX = this.getTreshold(this.bottomBorder, this.topBorder) * image.getHeight() / 1000;// image.getHeight()
		// * 7 / 10;
		int tresholdY = this.getTreshold(this.rightBorder, this.leftBorder) * image.getWidth() / 1000;// image.getWidth()
		// * 85 / 100;

		List<Integer> maxX = new ArrayList<>();

		boolean found = false;
		for (int x = 0; x < image.getWidth(); ++x) {
			if (image.getHistogramX().get(x) > tresholdX) {
				if (!found) {
					maxX.add(x);
					found = true;
				}
			} else {
				found = false;
			}
		}

		List<Integer> maxY = new ArrayList<>();

		found = false;
		for (int y = 0; y < image.getHeight(); ++y) {
			if (image.getHistogramY().get(y) > tresholdY) {
				if (!found) {
					maxY.add(y);
					found = true;
				}
			} else {
				found = false;
			}
		}

		int mask = 0;

		for (int x : maxX) {
			if (this.leftBorder.getLower() * image.getWidth() / 1000 < x
					&& x < this.leftBorder.getHigher() * image.getWidth() / 1000)
				mask |= 0x01;
			if (this.rightBorder.getLower() * image.getWidth() / 1000 < x
					&& x < this.rightBorder.getHigher() * image.getWidth() / 1000)
				mask |= 0x02;
		}

		for (int y : maxY) {
			if (this.topBorder.getLower() * image.getHeight() / 1000 < y
					&& y < this.topBorder.getHigher() * image.getHeight() / 1000)
				mask |= 0x04;
			if (this.middleBorder.getLower() * image.getHeight() / 1000 < y
					&& y < this.middleBorder.getHigher() * image.getHeight() / 1000)
				mask |= 0x08;
			if (this.bottomBorder.getLower() * image.getHeight() / 1000 < y
					&& y < this.bottomBorder.getHigher() * image.getWidth() / 1000)
				mask |= 0x10;
		}

		boolean error = mask == 0x1f;

		if (error) {
			return Type.MESSAGE_BOX;
		}

		if (isHome || screen.getSolvis().getHomeScreen().equals(SolvisScreen.get(screen))) {
			OcrRectangle ocrRectangle = new OcrRectangle(image, this.hhMm);
			if (ocrRectangle.getWidth() == 0 || ocrRectangle.getHeight() == 0) {
				return Type.HOME_NONE;
			}
			String hhmm = ocrRectangle.getString();
			ocrRectangle = new OcrRectangle(image, this.ddMmYy);
			if (ocrRectangle.getWidth() == 0 || ocrRectangle.getHeight() == 0) {
				return Type.HOME_NONE;
			}
			String ddMMYY = ocrRectangle.getString();

			Matcher t = TIME_PATTERN.matcher(hhmm);
			Matcher d = DATE_PATTERN.matcher(ddMMYY);

			if (!t.matches() && !d.matches()) {
				return Type.ERROR_BUTTON;
			} else {
				return Type.HOME_NONE;
			}
		}
		return Type.NONE;
	}

	private int getTreshold(final Range upper, final Range lower) {
		return upper.getLower() - lower.getHigher();
	}

	static class Creator extends CreatorByXML<ErrorDetection> {

		private Range leftBorder;
		private Range rightBorder;
		private Range topBorder;
		private Range middleBorder;
		private Range bottomBorder;
		private Rectangle hhMm;
		private Rectangle ddMmYy;
		private final Collection<ErrorCondition> errorConditions = new ArrayList<>();

		Creator(final String id, final BaseCreator<?> creator) {
			super(id, creator);
		}

		@Override
		public void setAttribute(final QName name, final String value) {
		}

		@Override
		public ErrorDetection create() throws XmlException, IOException {
			return new ErrorDetection(this.leftBorder, this.rightBorder, this.topBorder, this.middleBorder,
					this.bottomBorder, this.hhMm, this.ddMmYy, this.errorConditions);
		}

		@Override
		public CreatorByXML<?> getCreator(final QName name) {
			String id = name.getLocalPart();
			switch (id) {
				case XML_LEFT_BORDER:
				case XML_RIGHT_BORDER:
				case XML_TOP_BORDER:
				case XML_MIDDLE_BORDER:
				case XML_BOTTOM_BORDER:
					return new Range.Creator(id, getBaseCreator());
				case XML_HH_MM:
				case XML_DD_MM_YY:
					return new Rectangle.Creator(id, getBaseCreator());
				case XML_ERROR_CONDITION:
					return new ErrorCondition.Creator(id, getBaseCreator());
			}
			return null;
		}

		@Override
		public void created(final CreatorByXML<?> creator, final Object created) {
			switch (creator.getId()) {
				case XML_LEFT_BORDER:
					this.leftBorder = (Range) created;
					break;
				case XML_RIGHT_BORDER:
					this.rightBorder = (Range) created;
					break;
				case XML_TOP_BORDER:
					this.topBorder = (Range) created;
					break;
				case XML_MIDDLE_BORDER:
					this.middleBorder = (Range) created;
					break;
				case XML_BOTTOM_BORDER:
					this.bottomBorder = (Range) created;
					break;
				case XML_HH_MM:
					this.hhMm = (Rectangle) created;
					break;
				case XML_DD_MM_YY:
					this.ddMmYy = (Rectangle) created;
					break;
				case XML_ERROR_CONDITION:
					this.errorConditions.add((ErrorCondition) created);
			}
		}

	}

	private static class ErrorCondition {
		private final String channelId;
		private final boolean errorValue;

		private ErrorCondition(final String channelId, final boolean errorValue) {
			this.channelId = channelId;
			this.errorValue = errorValue;
		}

		private String getChannelId() {
			return this.channelId;
		}

		private boolean getErrorValue() {
			return this.errorValue;
		}

		private static class Creator extends CreatorByXML<ErrorCondition> {
			private String channelId;
			private boolean value;

			private Creator(final String id, final BaseCreator<?> creator) {
				super(id, creator);
			}

			@Override
			public void setAttribute(final QName name, final String value) {
				switch (name.getLocalPart()) {
					case "channelId":
						this.channelId = value;
						break;
					case "value":
						this.value = Boolean.parseBoolean(value);
						break;
				}

			}

			@Override
			public ErrorCondition create() throws XmlException, IOException {
				return new ErrorCondition(this.channelId, this.value);
			}

			@Override
			public CreatorByXML<?> getCreator(final QName name) {
				return null;
			}

			@Override
			public void created(final CreatorByXML<?> creator, final Object created) {
			}
		}
	}

	void instantiate(final Solvis solvis) {
		Execute execute = new Execute(solvis);

		AllSolvisData allData = solvis.getAllSolvisData();

		for (ErrorCondition condition : this.errorConditions) {
			SolvisData d = allData.get(condition.getChannelId());
			d.registerContinuousObserver(execute);
		}
	}

	private class Execute implements IObserver<SolvisData> {

		private Collection<SolvisData> errorSpecificDatas = new ArrayList<>();

		private Execute(final Solvis solvis) {
			for (ErrorCondition condition : ErrorDetection.this.errorConditions) {
				String name = condition.getChannelId();
				SolvisData data = solvis.getAllSolvisData().get(name);
				if (data == null) {
					logger.error("Channel Id <" + name + "> is missing. Check the control.xml file");
					return;
				}
				this.errorSpecificDatas.add(data);
				data.registerContinuousObserver(this);
			}
		}

		@Override
		public void update(final SolvisData data, final Object source) {
			for (ErrorCondition condition : ErrorDetection.this.errorConditions) {
				String channelId = condition.getChannelId();
				if (channelId.equals(data.getDescription().getId())) {
					boolean error;
					try {
						error = data.getBool() == condition.getErrorValue();
					} catch (TypeException e) {
						logger.error("Type error, update ignored", e);
						return;
					}
					data.getSolvis().getSolvisState().setError(error, data.getDescription());
				}
			}

		}

	}

	public static class WriteErrorScreens implements IObserver<SolvisErrorInfo> {

		private final File parent;

		public WriteErrorScreens(final Instances instances) {
			File parent = new File(instances.getWritePath(), Constants.Files.RESOURCE_DESTINATION);
			this.parent = new File(parent, Constants.Files.SOLVIS_ERROR_DESTINATION);
			FileHelper.mkdir(this.parent);
		}

		@Override
		public void update(final SolvisErrorInfo info, final Object source) {
			if (info.isCleared()) {
				return;
			}
			Collection<File> files = FileHelper.getSortedbyDate(this.parent, Constants.Files.ERROR_SCREEN_REGEX);
			int toDelete = files.size() - Constants.Files.MAX_NUMBER_OF_ERROR_SCREENS;
			for (Iterator<File> it = files.iterator(); it.hasNext() && toDelete >= 0; --toDelete) {
				it.next().delete();
			}
			String name = Constants.Files.ERROR_SCREEN_PREFIX + Long.toString(System.currentTimeMillis() / 100) + '.'
					+ Constants.Files.GRAFIC_SUFFIX;
			File file = new File(this.parent, name);
			try {
				MyImage errorImage = info.getImage();
				if (errorImage != null) {
					info.getImage().writeWhole(file);
				}
			} catch (IOException e) {
				logger.error("Error on writing the error image <" + name + ">.");
			}

		}

	}

	public static void main(final String[] args) {

		Range leftBorder = new Range(51, 84);
		Range rightBorder = new Range(906, 938);
		Range topBorder = new Range(92, 151);
		Range middleBorder = new Range(223, 282);
		Range bottomBorder = new Range(810, 869);
		Rectangle hhMm = new Rectangle(new Coordinate(190, 62), new Coordinate(224, 74));
		Rectangle ddMmYy = new Rectangle(new Coordinate(190, 80), new Coordinate(238, 91));
		Collection<ErrorCondition> errorConditions = Arrays.asList(new ErrorCondition("A14.Entstoerung", true));

		ErrorDetection errorDetection = new ErrorDetection(leftBorder, rightBorder, topBorder, middleBorder,
				bottomBorder, hhMm, ddMmYy, errorConditions);

		File parent = new File("testFiles\\images");

		final class Test {
			private final boolean isHome;
			private final String name;

			private Test(boolean isHome, String name) {
				this.isHome = isHome;
				this.name = name;
			}
		}

		Collection<Test> names = Arrays.asList( //
				new Test(false, "Stoerung 1.png"), new Test(false, "Stoerung 2.png"), new Test(false, "Stoerung 3.png"),
				new Test(false, "Stoerung 4.png"), new Test(false, "Stoerung 5.png"),
				new Test(true, "Stoerung h keine.png"), new Test(true, "Stoerung h1.bmp"),
				new Test(true, "Stoerung h2.png"));

		BufferedImage image = null;

		for (Iterator<Test> it = names.iterator(); it.hasNext();) {
			Test test = it.next();

			File file = new File(parent, test.name);
			try {
				image = ImageIO.read(file);
			} catch (IOException e) {
				System.err.println("File: " + file.getName());
				e.printStackTrace();
			}

			MyImage myImage = new MyImage(image);

			SolvisScreen screen = new SolvisScreen(myImage, null);

			Type type = errorDetection.getType(screen, test.isHome);

			System.out.println(file.getName() + " errorType? " + type.name());
		}

	}

}
