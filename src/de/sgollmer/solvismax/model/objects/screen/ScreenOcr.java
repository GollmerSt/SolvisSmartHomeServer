/************************************************************************
 * 
 * $Id$
 *
 * 
 ************************************************************************/

package de.sgollmer.solvismax.model.objects.screen;

import java.io.IOException;

import javax.xml.namespace.QName;

import de.sgollmer.solvismax.error.XmlException;
import de.sgollmer.solvismax.imagepatternrecognition.image.MyImage;
import de.sgollmer.solvismax.imagepatternrecognition.ocr.OcrRectangle;
import de.sgollmer.solvismax.model.Solvis;
import de.sgollmer.solvismax.objects.Rectangle;
import de.sgollmer.solvismax.xml.BaseCreator;
import de.sgollmer.solvismax.xml.CreatorByXML;

public class ScreenOcr implements IScreenPartCompare {

	private static final String XML_RECTANGLE = "Rectangle";

	private final Rectangle rectangle;
	private final String value;

	private ScreenOcr(Rectangle rectangle, String value) {
		this.rectangle = rectangle;
		this.value = value;
	}

	@Override
	public boolean isElementOf(MyImage image, Solvis solvis) {
		OcrRectangle ocr = new OcrRectangle(image, this.rectangle);
		String cmp = ocr.getString();
		return cmp.equals(this.value);
	}

	static class Creator extends CreatorByXML<ScreenOcr> {

		private Rectangle rectangle;
		private String value;

		Creator(String id, BaseCreator<?> creator) {
			super(id, creator);
		}

		@Override
		public void setAttribute(QName name, String value) {
			switch (name.getLocalPart()) {
				case "value":
					this.value = value;
			}

		}

		@Override
		public ScreenOcr create() throws XmlException, IOException {
			return new ScreenOcr(this.rectangle, this.value);
		}

		@Override
		public CreatorByXML<?> getCreator(QName name) {
			String id = name.getLocalPart();
			switch (id) {
				case XML_RECTANGLE:
					return new Rectangle.Creator(id, getBaseCreator());
			}
			return null;
		}

		@Override
		public void created(CreatorByXML<?> creator, Object created) {
			switch (creator.getId()) {
				case XML_RECTANGLE:
					this.rectangle = (Rectangle) created;
					break;
			}
		}

	}
}
