package de.sgollmer.solvismax.model.objects;

import javax.xml.namespace.QName;

import de.sgollmer.solvismax.error.XmlError;
import de.sgollmer.solvismax.imagepatternrecognition.image.MyImage;
import de.sgollmer.solvismax.objects.Rectangle;
import de.sgollmer.solvismax.xml.CreatorByXML;
import de.sgollmer.solvismax.xml.BaseCreator;

public class ScreenGrafic implements ScreenCompare, Assigner {
	private final String id;
	private final boolean exact;
	private final Rectangle rectangle;

	private ScreenGrafic(String id, boolean exact, Rectangle rectangle) {
		this.id = id;
		this.exact = exact;
		this.rectangle = rectangle;
	}

	@Override
	public boolean isElementOf(MyImage image) {
		// TODO Auto-generated method stub
		return false;
	}

	/**
	 * @return the exact
	 */
	public boolean isExact() {
		return exact;
	}

	/**
	 * @return the id
	 */
	public String getId() {
		return id;
	}

	public static class Creator extends CreatorByXML<ScreenGrafic> {

		private String id;
		private boolean exact;
		private Rectangle rectangle;

		public Creator(String id, BaseCreator<?> creator) {
			super(id, creator);
		}

		@Override
		public void setAttribute(QName name, String value) {
			switch (name.getLocalPart()) {
				case "id":
					this.id = value;
					break;
				case "exact":
					this.exact = Boolean.parseBoolean(value);
					break;
			}

		}

		@Override
		public ScreenGrafic create() throws XmlError {
			return new ScreenGrafic(id, exact, rectangle);
		}

		@Override
		public CreatorByXML<?> getCreator(QName name) {
			if ( name.getLocalPart().equals("Field")) {
				return new Rectangle.Creator( name.getLocalPart(), this.getBaseCreator() ) ;
			}
			return null;
		}

		@Override
		public void created(CreatorByXML<?> creator, Object created) {
			this.rectangle = (Rectangle) created ;

		}

	}

	@Override
	public void assign(SolvisDescription description) {
	}
}
