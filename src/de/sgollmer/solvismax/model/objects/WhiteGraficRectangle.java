package de.sgollmer.solvismax.model.objects;

import java.io.IOException;

import javax.xml.namespace.QName;

import de.sgollmer.solvismax.error.AssignmentException;
import de.sgollmer.solvismax.error.ReferenceException;
import de.sgollmer.solvismax.error.XmlException;
import de.sgollmer.solvismax.imagepatternrecognition.image.MyImage;
import de.sgollmer.solvismax.model.Solvis;
import de.sgollmer.solvismax.model.objects.screen.IScreenPartCompare;
import de.sgollmer.solvismax.objects.Rectangle;
import de.sgollmer.solvismax.xml.BaseCreator;
import de.sgollmer.solvismax.xml.CreatorByXML;

public class WhiteGraficRectangle implements IScreenPartCompare {

	private Rectangle rectangle;

	public WhiteGraficRectangle(Rectangle rectangle) {
		this.rectangle = rectangle;
	}

	@Override
	public boolean isElementOf(MyImage image, Solvis solvis) {
		return image.isWhite(this.rectangle) != this.rectangle.isInvertFunction();
	}

	public static class Creator extends CreatorByXML<WhiteGraficRectangle> {

		private final Rectangle.Creator rectangeleCreator;;

		public Creator(String id, BaseCreator<?> creator) {
			super(id, creator);
			this.rectangeleCreator = new Rectangle.Creator(id, creator);
		}

		@Override
		public void setAttribute(QName name, String value) {
			this.rectangeleCreator.setAttribute(name, value);

		}

		@Override
		public WhiteGraficRectangle create()
				throws XmlException, IOException, AssignmentException, ReferenceException {
			Rectangle rectangle = this.rectangeleCreator.create();
			return new WhiteGraficRectangle(rectangle);
		}

		@Override
		public CreatorByXML<?> getCreator(QName name) {
			return this.rectangeleCreator.getCreator(name);
		}

		@Override
		public void created(CreatorByXML<?> creator, Object created) throws XmlException {
			this.rectangeleCreator.created(creator, created);

		}

	}

}