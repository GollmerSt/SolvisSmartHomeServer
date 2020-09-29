package de.sgollmer.solvismax.model.objects;

import java.io.IOException;

import javax.xml.namespace.QName;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamWriter;

import de.sgollmer.solvismax.error.AssignmentException;
import de.sgollmer.solvismax.error.ReferenceException;
import de.sgollmer.solvismax.error.XmlException;
import de.sgollmer.solvismax.xml.ArrayXml;
import de.sgollmer.solvismax.xml.BaseCreator;
import de.sgollmer.solvismax.xml.CreatorByXML;

public class Feature implements ArrayXml.IElement<Feature> {
	final String id;
	final boolean value;

	public Feature(String id, boolean value) {
		this.id = id;
		this.value = value;
	}

	public Feature() {
		this(null, false);
	}

	public static class Creator extends CreatorByXML<Feature> {

		private String id;
		private boolean value = false;

		public Creator(String id, BaseCreator<?> creator) {
			super(id, creator);
		}

		@Override
		public void setAttribute(QName name, String value) {
			switch (name.getLocalPart()) {
				case "id":
					this.id = value;
					break;
				case "value":
					this.value = Boolean.parseBoolean(value);
					break;
			}

		}

		@Override
		public Feature create() throws XmlException, IOException, AssignmentException, ReferenceException {
			return new Feature(this.id, this.value);
		}

		@Override
		public CreatorByXML<?> getCreator(QName name) {
			return null;
		}

		@Override
		public void created(CreatorByXML<?> creator, Object created) throws XmlException {
		}

	}

	public String getId() {
		return this.id;
	}

	public boolean isSet() {
		return this.value;
	}

	@Override
	public CreatorByXML<Feature> getCreator(String name, BaseCreator<?> creator) {
		return new Feature.Creator(name, creator);
	}

	public void writeXml(XMLStreamWriter writer) throws XMLStreamException {
		writer.writeAttribute("id", this.id);
		writer.writeAttribute("value", Boolean.toString(this.value));
	}
}