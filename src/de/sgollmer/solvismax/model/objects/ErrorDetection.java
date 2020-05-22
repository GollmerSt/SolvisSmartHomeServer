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

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import de.sgollmer.solvismax.error.XmlError;
import de.sgollmer.solvismax.imagepatternrecognition.image.MyImage;
import de.sgollmer.solvismax.imagepatternrecognition.ocr.OcrRectangle;
import de.sgollmer.solvismax.model.Solvis;
import de.sgollmer.solvismax.model.objects.Observer.ObserverI;
import de.sgollmer.solvismax.model.objects.data.SolvisData;
import de.sgollmer.solvismax.model.objects.screen.SolvisScreen;
import de.sgollmer.solvismax.objects.Coordinate;
import de.sgollmer.solvismax.objects.Range;
import de.sgollmer.solvismax.objects.Rectangle;
import de.sgollmer.solvismax.xml.BaseCreator;
import de.sgollmer.solvismax.xml.CreatorByXML;

public class ErrorDetection {

	private static final Logger logger = LogManager.getLogger(Solvis.class);

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

	public ErrorDetection(Range leftBorder, Range rightBorder, Range topBorder, Range middleBorder, Range bottomBorder,
			Rectangle hhMm, Rectangle ddMmYy, Collection<ErrorCondition> errorConditions) {
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
		NONE, HOME_NONE, MESSAGE_BOX, ERROR_BUTTON
	}
	
	public Type getType(SolvisScreen screen ) {
		return this.getType(screen, false);
	}

	private Type getType(SolvisScreen screen, boolean isHome ) {

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

		if (isHome || screen.getSolvis().getHomeScreen().equals(screen.get())) {
			OcrRectangle ocrRectangle = new OcrRectangle(image, this.hhMm) ;
			if ( ocrRectangle.getWidth() == 0 || ocrRectangle.getHeight() == 0 ) {
				return Type.HOME_NONE;
			}
			String hhmm = ocrRectangle.getString();
			ocrRectangle = new OcrRectangle(image, this.ddMmYy) ;
			if ( ocrRectangle.getWidth() == 0 || ocrRectangle.getHeight() == 0 ) {
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

	private int getTreshold(Range upper, Range lower) {
		return upper.getLower() - lower.getHigher();
	}

	public static class Creator extends CreatorByXML<ErrorDetection> {

		private Range leftBorder;
		private Range rightBorder;
		private Range topBorder;
		private Range middleBorder;
		private Range bottomBorder;
		private Rectangle hhMm;
		private Rectangle ddMmYy;
		private final Collection<ErrorCondition> errorConditions = new ArrayList<>();

		public Creator(String id, BaseCreator<?> creator) {
			super(id, creator);
		}

		@Override
		public void setAttribute(QName name, String value) {
		}

		@Override
		public ErrorDetection create() throws XmlError, IOException {
			return new ErrorDetection(this.leftBorder, this.rightBorder, this.topBorder, this.middleBorder,
					this.bottomBorder, this.hhMm, this.ddMmYy, this.errorConditions);
		}

		@Override
		public CreatorByXML<?> getCreator(QName name) {
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
		public void created(CreatorByXML<?> creator, Object created) {
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

		public ErrorCondition(String channelId, boolean errorValue) {
			this.channelId = channelId;
			this.errorValue = errorValue;
		}

		public String getChannelId() {
			return this.channelId;
		}

		public boolean getErrorValue() {
			return this.errorValue;
		}

		public static class Creator extends CreatorByXML<ErrorCondition> {
			private String channelId;
			private boolean value;

			public Creator(String id, BaseCreator<?> creator) {
				super(id, creator);
			}

			@Override
			public void setAttribute(QName name, String value) {
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
			public ErrorCondition create() throws XmlError, IOException {
				return new ErrorCondition(this.channelId, this.value);
			}

			@Override
			public CreatorByXML<?> getCreator(QName name) {
				return null;
			}

			@Override
			public void created(CreatorByXML<?> creator, Object created) {
			}
		}
	}

	public void instantiate(Solvis solvis) {
		Execute execute = new Execute(solvis);
		solvis.registerObserver(execute);
	}

	private class Execute implements ObserverI<SolvisData> {

		private Collection<SolvisData> errorSpecificDatas = new ArrayList<>();

		public Execute(Solvis solvis) {
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
		public void update(SolvisData data, Object source) {
			for (ErrorCondition condition : ErrorDetection.this.errorConditions) {
				String channelId = condition.getChannelId();
				if (channelId.equals(data.getDescription().getId())) {
					boolean error = data.getBool() == condition.getErrorValue();
					data.getSolvis().getSolvisState().setError(error, data.getDescription());
				}
			}

		}

	}

	public static void main(String[] args) {

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

			public Test(boolean isHome, String name) {
				this.isHome = isHome;
				this.name = name;
			}
		}

		Collection<Test> names = Arrays.asList(	//
				new Test(false, "Stoerung 1.png"),
				new Test(false, "Stoerung 2.png"),
				new Test(false, "Stoerung 3.png"),
				new Test(false, "Stoerung 4.png"),
				new Test(false, "Stoerung 5.png"),
				new Test(true, "Stoerung h keine.png"),
				new Test(true, "Stoerung h1.bmp"),
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
			
			SolvisScreen screen = new SolvisScreen(myImage, null) ;
			
			Type type = errorDetection.getType(screen, test.isHome) ;

			System.out.println(file.getName() + " errorType? " + type.name());
		}

	}

}
