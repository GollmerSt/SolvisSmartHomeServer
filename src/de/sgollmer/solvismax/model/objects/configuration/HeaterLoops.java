/************************************************************************
 * 
 * $Id$
 *
 * 
 ************************************************************************/

package de.sgollmer.solvismax.model.objects.configuration;

import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.Collection;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.imageio.ImageIO;
import javax.xml.namespace.QName;

import de.sgollmer.solvismax.error.XmlError;
import de.sgollmer.solvismax.imagepatternrecognition.image.MyImage;
import de.sgollmer.solvismax.imagepatternrecognition.ocr.OcrRectangle;
import de.sgollmer.solvismax.model.Solvis;
import de.sgollmer.solvismax.model.objects.configuration.Configurations.Configuration;
import de.sgollmer.solvismax.model.objects.screen.Screen;
import de.sgollmer.solvismax.model.objects.screen.SolvisScreen;
import de.sgollmer.solvismax.objects.Rectangle;
import de.sgollmer.solvismax.xml.BaseCreator;
import de.sgollmer.solvismax.xml.CreatorByXML;

public class HeaterLoops implements Configuration {

	private static final String XML_HEATER_LOOPS_HK1 = "HK1Button";
	private static final String XML_HEATER_LOOPS_HK2 = "HK2Button";
	private static final String XML_HEATER_LOOPS_HK3 = "HK3Button";

	private static final Pattern DIGIT = Pattern.compile("\\d");

	private final String screenRef;
	private final Collection<Rectangle> buttons;

	public HeaterLoops(String screenRef, Rectangle hk1, Rectangle hk2, Rectangle hk3) {
		this.screenRef = screenRef;
		this.buttons = Arrays.asList(hk1, hk2, hk3);

	}

	private int getConfiguration(SolvisScreen screen) throws IOException {
		int circle = 1;
		int result = 0;
		for (Rectangle rectangle : this.buttons) {
			OcrRectangle ocr = new OcrRectangle(screen.getImage(), rectangle);
			String s = ocr.getString();
			Matcher m = DIGIT.matcher(s);
			if (m.matches() && circle == Integer.parseInt(s)) {
				result |= 1 << (circle - 1);
				++circle;
			}
		}
		if (result == 0) {
			result = 1;
		}
		return result;
	}

	@Override
	public int getConfiguration(Solvis solvis) throws IOException {
		return this.getConfiguration(solvis.getCurrentScreen());
	}

	public String getScreenRef() {
		return this.screenRef;
	}

	public static class Creator extends CreatorByXML<HeaterLoops> {

		private String screenRef;
		private Rectangle hk1;
		private Rectangle hk2;
		private Rectangle hk3;

		public Creator(String id, BaseCreator<?> creator) {
			super(id, creator);
		}

		@Override
		public void setAttribute(QName name, String value) {
			switch (name.getLocalPart()) {
				case "screenRef":
					this.screenRef = value;
			}

		}

		@Override
		public HeaterLoops create() throws XmlError, IOException {
			return new HeaterLoops(this.screenRef, this.hk1, this.hk2, this.hk3);
		}

		@Override
		public CreatorByXML<?> getCreator(QName name) {
			String id = name.getLocalPart();
			switch (id) {
				case XML_HEATER_LOOPS_HK1:
				case XML_HEATER_LOOPS_HK2:
				case XML_HEATER_LOOPS_HK3:
					return new Rectangle.Creator(id, getBaseCreator());
			}
			return null;
		}

		@Override
		public void created(CreatorByXML<?> creator, Object created) {
			switch (creator.getId()) {
				case XML_HEATER_LOOPS_HK1:
					this.hk1 = (Rectangle) created;
					break;
				case XML_HEATER_LOOPS_HK2:
					this.hk2 = (Rectangle) created;
					break;
				case XML_HEATER_LOOPS_HK3:
					this.hk3 = (Rectangle) created;
					break;
			}

		}

	}

	public MyImage getTestImage() throws IOException {

		File parent = new File("testFiles\\images");

		File file = new File(parent, "Home 2 Heizkreise.png");

		BufferedImage bufferedImage = null;
		bufferedImage = ImageIO.read(file);

		return new MyImage(bufferedImage);
	}

	@Override
	public Screen getScreen(Solvis solvis) {
		return solvis.getSolvisDescription().getScreens().get(this.getScreenRef(), 0);
	}

}
