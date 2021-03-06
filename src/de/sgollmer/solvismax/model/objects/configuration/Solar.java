/************************************************************************
 * 
 * $Id$
 *
 * 
 ************************************************************************/

package de.sgollmer.solvismax.model.objects.configuration;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;

import javax.xml.namespace.QName;

import de.sgollmer.solvismax.error.TerminationException;
import de.sgollmer.solvismax.helper.Helper.Format;
import de.sgollmer.solvismax.imagepatternrecognition.image.MyImage;
import de.sgollmer.solvismax.imagepatternrecognition.ocr.OcrRectangle;
import de.sgollmer.solvismax.log.LogManager;
import de.sgollmer.solvismax.log.LogManager.ILogger;
import de.sgollmer.solvismax.model.Solvis;
import de.sgollmer.solvismax.model.objects.configuration.Configurations.IConfiguration;
import de.sgollmer.solvismax.model.objects.screen.AbstractScreen;
import de.sgollmer.solvismax.objects.Rectangle;
import de.sgollmer.xmllibrary.BaseCreator;
import de.sgollmer.xmllibrary.CreatorByXML;
import de.sgollmer.xmllibrary.XmlException;

public class Solar implements IConfiguration {

	private static final ILogger logger = LogManager.getInstance().getLogger(Solar.class);

	private static final String XML_RETURN_TEMPERATURE = "ReturnTemperature";
	private static final String XML_OUTGOING_TEMPERATURE = "OutgoingTemperature";

	private final String screenRef;
	private final Format format;
	private final int maxTemperatureX10;
	private final Collection<Rectangle> rectangles = new ArrayList<>();

	private Solar(final String screenRef, final String format, final int maxTemperatureX10,
			final Rectangle returnTemperature, final Rectangle outgoingTemperature) {
		this.screenRef = screenRef;
		this.format = new Format(format);
		this.maxTemperatureX10 = maxTemperatureX10;
		this.rectangles.add(outgoingTemperature);
		this.rectangles.add(returnTemperature);
	}

	static class Creator extends CreatorByXML<Solar> {

		private String screenRef;
		private String format;
		private int maxTemperatureX10;
		private Rectangle returnTemperature;
		private Rectangle outgoingTemperature;

		Creator(String id, BaseCreator<?> creator) {
			super(id, creator);
		}

		@Override
		public void setAttribute(final QName name, final String value) {
			switch (name.getLocalPart()) {
				case "screenRef":
					this.screenRef = value;
					break;
				case "format":
					this.format = value;
					break;
				case "maxTemperatureX10":
					this.maxTemperatureX10 = Integer.parseInt(value);
					break;
			}

		}

		@Override
		public Solar create() throws XmlException, IOException {
			return new Solar(this.screenRef, this.format, this.maxTemperatureX10, this.returnTemperature,
					this.outgoingTemperature);
		}

		@Override
		public CreatorByXML<?> getCreator(final QName name) {
			String id = name.getLocalPart();
			switch (id) {
				case XML_OUTGOING_TEMPERATURE:
				case XML_RETURN_TEMPERATURE:
					return new Rectangle.Creator(id, getBaseCreator());
			}
			return null;
		}

		@Override
		public void created(final CreatorByXML<?> creator, final Object created) {
			switch (creator.getId()) {
				case XML_OUTGOING_TEMPERATURE:
					this.outgoingTemperature = (Rectangle) created;
					break;
				case XML_RETURN_TEMPERATURE:
					this.returnTemperature = (Rectangle) created;
					break;
			}

		}

	}

	@Override
	public int getConfiguration(final Solvis solvis) throws IOException, TerminationException {
		MyImage image = solvis.getCurrentScreen().getImage();
		for (Rectangle rectangle : this.rectangles) {
			OcrRectangle ocr = new OcrRectangle(image, rectangle);
			String scanned = ocr.getString();
			String intString = this.format.getString(scanned);
			if (intString == null) {
				logger.error("Scanned string <" + scanned + "> doesn't fit the regular expression");
				return 0x00;
			}
			if (Integer.parseInt(intString) <= this.maxTemperatureX10) {
				return 0x08;
			}
		}
		return 0x00;
	}

	@Override
	public AbstractScreen getScreen(final Solvis solvis) {
		return solvis.getSolvisDescription().getScreens().get(this.screenRef, solvis);
	}

}
