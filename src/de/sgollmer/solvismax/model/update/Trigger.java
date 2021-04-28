package de.sgollmer.solvismax.model.update;

import java.io.IOException;

import javax.xml.namespace.QName;

import de.sgollmer.xmllibrary.BaseCreator;
import de.sgollmer.xmllibrary.CreatorByXML;
import de.sgollmer.xmllibrary.XmlException;

public class Trigger {

	private final String id;

	private Trigger(String id) {
		this.id = id;
	}
	
	public String getId() {
		return this.id;
	}

	public static class Creator extends CreatorByXML<Trigger> {

		private String id;

		public Creator(String id, BaseCreator<?> creator) {
			super(id, creator);
		}

		@Override
		public Trigger create() throws XmlException, IOException {
			return new Trigger(this.id);
		}

		@Override
		public void created(CreatorByXML<?> arg0, Object arg1) throws XmlException {
			
		}

		@Override
		public CreatorByXML<?> getCreator(QName arg0) {
			return null;
		}

		@Override
		public void setAttribute(QName name, String value) throws XmlException {
			switch (name.getLocalPart()) {
				case "id":
					this.id = value;
			}
			
		}
		
	}
}
