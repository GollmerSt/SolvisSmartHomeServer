package de.sgollmer.solvismax.model.objects.screen;

import javax.xml.namespace.QName;

import de.sgollmer.xmllibrary.BaseCreator;
import de.sgollmer.xmllibrary.CreatorByXML;
import de.sgollmer.xmllibrary.XmlException;

class ScreenGraficRef {

	public final String refId;

	private ScreenGraficRef(String refId) {
		this.refId = refId;
	}

	public static class Creator extends CreatorByXML<ScreenGraficRef> {

		private String refId;

		public Creator(String id, BaseCreator<?> creator) {
			super(id, creator);
		}

		@Override
		public void setAttribute(QName name, String value) {
			if (name.getLocalPart().equals("refId")) {
				this.refId = value;
			}

		}

		@Override
		public ScreenGraficRef create() throws XmlException {
			return new ScreenGraficRef(this.refId);
		}

		@Override
		public CreatorByXML<?> getCreator(QName name) {
			return null;
		}

		@Override
		public void created(CreatorByXML<?> creator, Object created) {
		}

	}

	public String getRefId() {
		return this.refId;
	}

}
