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

import de.sgollmer.solvismax.error.TerminationException;
import de.sgollmer.solvismax.imagepatternrecognition.image.MyImage;
import de.sgollmer.solvismax.imagepatternrecognition.ocr.OcrRectangle;
import de.sgollmer.solvismax.model.Solvis;
import de.sgollmer.solvismax.model.objects.configuration.Configurations.IConfiguration;
import de.sgollmer.solvismax.model.objects.screen.AbstractScreen;
import de.sgollmer.solvismax.model.objects.screen.SolvisScreen;
import de.sgollmer.solvismax.objects.Rectangle;
import de.sgollmer.xmllibrary.BaseCreator;
import de.sgollmer.xmllibrary.CreatorByXML;
import de.sgollmer.xmllibrary.XmlException;

public class HeaterLoops implements IConfiguration {

	private static final String XML_HEATER_LOOPS_HK1 = "HK1Button";
	private static final String XML_HEATER_LOOPS_HK2 = "HK2Button";
	private static final String XML_HEATER_LOOPS_HK3 = "HK3Button";

	private static final Pattern DIGIT = Pattern.compile("\\d");

	private final String screenRef;
	private final Collection<Rectangle> buttons;

	private HeaterLoops(final String screenRef, final Rectangle hk1, final Rectangle hk2, final Rectangle hk3) {
		this.screenRef = screenRef;
		this.buttons = Arrays.asList(hk1, hk2, hk3);

	}

	private int getConfiguration(final SolvisScreen screen) throws IOException {
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
	public int getConfiguration(final Solvis solvis) throws IOException, TerminationException {
		return this.getConfiguration(solvis.getCurrentScreen());
	}

	private String getScreenRef() {
		return this.screenRef;
	}

	static class Creator extends CreatorByXML<HeaterLoops> {

		private String screenRef;
		private Rectangle hk1;
		private Rectangle hk2;
		private Rectangle hk3;

		Creator(final String id, final BaseCreator<?> creator) {
			super(id, creator);
		}

		@Override
		public void setAttribute(final QName name, final String value) {
			switch (name.getLocalPart()) {
				case "screenRef":
					this.screenRef = value;
			}

		}

		@Override
		public HeaterLoops create() throws XmlException, IOException {
			return new HeaterLoops(this.screenRef, this.hk1, this.hk2, this.hk3);
		}

		@Override
		public CreatorByXML<?> getCreator(final QName name) {
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
		public void created(final CreatorByXML<?> creator, final Object created) {
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

	@SuppressWarnings("unused")
	private MyImage getTestImage() throws IOException {

		File parent = new File("testFiles\\images");

		File file = new File(parent, "Home 2 Heizkreise.png");

		BufferedImage bufferedImage = null;
		bufferedImage = ImageIO.read(file);

		return new MyImage(bufferedImage);
	}

	@Override
	public AbstractScreen getScreen(final Solvis solvis) {
		return solvis.getSolvisDescription().getScreens().get(this.getScreenRef(), solvis);
	}

}
