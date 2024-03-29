package de.sgollmer.solvismax.model.objects;

import java.io.IOException;

import javax.xml.namespace.QName;

import de.sgollmer.solvismax.error.TerminationException;
import de.sgollmer.solvismax.imagepatternrecognition.image.MyImage;
import de.sgollmer.solvismax.model.Solvis;
import de.sgollmer.solvismax.model.objects.screen.IScreenPartCompare;
import de.sgollmer.solvismax.objects.Rectangle;
import de.sgollmer.xmllibrary.BaseCreator;
import de.sgollmer.xmllibrary.CreatorByXML;
import de.sgollmer.xmllibrary.XmlException;

public class WhiteGraficRectangle implements IScreenPartCompare {

	private Rectangle rectangle;

	private WhiteGraficRectangle(final Rectangle rectangle) {
		this.rectangle = rectangle;
	}

	@Override
	public boolean isElementOf(final MyImage image, final Solvis solvis) {
		return image.isWhite(this.rectangle) != this.rectangle.isInvertFunction();
	}

	public static class Creator extends CreatorByXML<WhiteGraficRectangle> {

		private final Rectangle.Creator rectangeleCreator;;

		public Creator(final String id, final BaseCreator<?> creator) {
			super(id, creator);
			this.rectangeleCreator = new Rectangle.Creator(id, creator);
		}

		@Override
		public void setAttribute(final QName name, final String value) {
			this.rectangeleCreator.setAttribute(name, value);

		}

		@Override
		public WhiteGraficRectangle create() throws XmlException, IOException {
			Rectangle rectangle = this.rectangeleCreator.create();
			return new WhiteGraficRectangle(rectangle);
		}

		@Override
		public CreatorByXML<?> getCreator(final QName name) {
			return this.rectangeleCreator.getCreator(name);
		}

		@Override
		public void created(final CreatorByXML<?> creator, final Object created) throws XmlException {
			this.rectangeleCreator.created(creator, created);

		}

	}

	@Override
	public boolean isLearned(final Solvis solvis) {
		return true;
	}

	@Override
	public void learn(final Solvis solvis) throws IOException, TerminationException {

	}

}