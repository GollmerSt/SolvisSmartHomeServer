/************************************************************************
 * 
 * $Id$
 *
 * 
 ************************************************************************/

package de.sgollmer.solvismax.model.objects.backup;

import java.io.IOException;

import javax.xml.namespace.QName;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamWriter;

import de.sgollmer.solvismax.error.XmlError;
import de.sgollmer.solvismax.model.objects.data.BooleanValue;
import de.sgollmer.solvismax.model.objects.data.IntegerValue;
import de.sgollmer.solvismax.model.objects.data.ModeI;
import de.sgollmer.solvismax.model.objects.data.ModeValue;
import de.sgollmer.solvismax.model.objects.data.SingleData;
import de.sgollmer.solvismax.model.objects.data.StringData;
import de.sgollmer.solvismax.xml.BaseCreator;
import de.sgollmer.solvismax.xml.CreatorByXML;

public class Measurement {

	public static final String XML_MEASUREMENT_BOOLEAN = "BooleanValue";
	public static final String XML_MEASUREMENT_INTEGER = "IntegerValue";
	public static final String XML_MEASUREMENT_STRING = "StringValue";
	public static final String XML_MEASUREMENT_MODE = "ModeValue";

	private final String id;
	private final SingleData<?> data;

	public Measurement(String id, SingleData<?> data) {
		this.id = id;
		this.data = data;
	}

	/**
	 * @return the data
	 */
	public SingleData<?> getData() {
		return data;
	}

	public static class Creator extends CreatorByXML<Measurement> {

		private String id;
		private SingleData<?> data;

		public Creator(String id, BaseCreator<?> creator) {
			super(id, creator);
		}

		@Override
		public void setAttribute(QName name, String value) {
			switch (name.getLocalPart()) {
				case "id":
					this.id = value;
					break;
			}

		}

		@Override
		public Measurement create() throws XmlError, IOException {
			return new Measurement(id, data);
		}

		@Override
		public CreatorByXML<?> getCreator(QName name) {
			String id = name.getLocalPart();
			switch (id) {
				case XML_MEASUREMENT_BOOLEAN:
				case XML_MEASUREMENT_INTEGER:
				case XML_MEASUREMENT_MODE:
				case XML_MEASUREMENT_STRING:
					return new ValueCreator(id, getBaseCreator());
			}
			return null;
		}

		@Override
		public void created(CreatorByXML<?> creator, Object created) {
			switch (creator.getId()) {
				case XML_MEASUREMENT_BOOLEAN:
				case XML_MEASUREMENT_INTEGER:
				case XML_MEASUREMENT_MODE:
				case XML_MEASUREMENT_STRING:
					this.data = (SingleData<?>) created;
			}

		}

	}

	private static class ValueCreator extends CreatorByXML<SingleData<?>> {

		StringBuilder text = new StringBuilder();

		public ValueCreator(String id, BaseCreator<?> creator) {
			super(id, creator);
		}

		@Override
		public void setAttribute(QName name, String value) {
		}

		@Override
		public SingleData<?> create() throws XmlError, IOException {
			final String dataString = text.toString();
			switch (this.getId()) {
				case XML_MEASUREMENT_BOOLEAN:
					return new BooleanValue(Boolean.parseBoolean(dataString));
				case XML_MEASUREMENT_INTEGER:
					return new IntegerValue(Integer.parseInt(dataString));
				case XML_MEASUREMENT_MODE:
					return new ModeValue<ModeI>(new ModeI() {

						@Override
						public String getName() {
							return dataString;
						}
					});
				case XML_MEASUREMENT_STRING:
					return new StringData(dataString);
			}
			return null;
		}

		@Override
		public CreatorByXML<?> getCreator(QName name) {
			return null;
		}

		@Override
		public void created(CreatorByXML<?> creator, Object created) {
		}

		@Override
		public void addCharacters(String data) {
			this.text.append(data.trim());
		}

	}

	public void writeXml(XMLStreamWriter writer) throws XMLStreamException {

		writer.writeAttribute("id", this.id);
		writer.writeStartElement(this.data.getXmlId());
		writer.writeCharacters(data.toString());
		writer.writeEndElement();

	}

	/**
	 * @return the id
	 */
	public String getId() {
		return id;
	}

}
