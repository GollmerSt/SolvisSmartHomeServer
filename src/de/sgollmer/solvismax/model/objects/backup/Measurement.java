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

import de.sgollmer.solvismax.Constants;
import de.sgollmer.solvismax.model.objects.data.BooleanValue;
import de.sgollmer.solvismax.model.objects.data.IMode;
import de.sgollmer.solvismax.model.objects.data.IntegerValue;
import de.sgollmer.solvismax.model.objects.data.ModeValue;
import de.sgollmer.solvismax.model.objects.data.SingleData;
import de.sgollmer.solvismax.model.objects.data.StringData;
import de.sgollmer.xmllibrary.BaseCreator;
import de.sgollmer.xmllibrary.CreatorByXML;
import de.sgollmer.xmllibrary.XmlException;

public class Measurement {

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
		return this.data;
	}

	static class Creator extends CreatorByXML<Measurement> {

		private String id;
		private SingleData<?> data;

		Creator(String id, BaseCreator<?> creator) {
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
		public Measurement create() throws XmlException, IOException {
			return new Measurement(this.id, this.data);
		}

		@Override
		public CreatorByXML<?> getCreator(QName name) {
			String id = name.getLocalPart();
			switch (id) {
				case Constants.XmlStrings.XML_MEASUREMENT_BOOLEAN:
				case Constants.XmlStrings.XML_MEASUREMENT_INTEGER:
				case Constants.XmlStrings.XML_MEASUREMENT_MODE:
				case Constants.XmlStrings.XML_MEASUREMENT_STRING:
					return new ValueCreator(id, getBaseCreator());
			}
			return null;
		}

		@Override
		public void created(CreatorByXML<?> creator, Object created) {
			switch (creator.getId()) {
				case Constants.XmlStrings.XML_MEASUREMENT_BOOLEAN:
				case Constants.XmlStrings.XML_MEASUREMENT_INTEGER:
				case Constants.XmlStrings.XML_MEASUREMENT_MODE:
				case Constants.XmlStrings.XML_MEASUREMENT_STRING:
					this.data = (SingleData<?>) created;
			}

		}

	}

	private static class ValueCreator extends CreatorByXML<SingleData<?>> {

		StringBuilder text = new StringBuilder();

		private ValueCreator(String id, BaseCreator<?> creator) {
			super(id, creator);
		}

		@Override
		public void setAttribute(QName name, String value) {
		}
		
		private static class Mode implements IMode<Mode> {
			
			private final String data ;
			
			public Mode(String data) {
				this.data=data;
			}

			@Override
			public int compareTo(Mode o) {
				return this.data.compareTo(o.data);
			}

			@Override
			public String getName() {
				return this.data;
			}
			
			@Override
			public ModeValue<?> create(long timeStamp) {
				return new ModeValue<>(this, timeStamp);
			}
		}

		@Override
		public SingleData<?> create() throws XmlException, IOException {
			final String dataString = this.text.toString();
			switch (this.getId()) {
				case Constants.XmlStrings.XML_MEASUREMENT_BOOLEAN:
					return new BooleanValue(Boolean.parseBoolean(dataString), -1);
				case Constants.XmlStrings.XML_MEASUREMENT_INTEGER:
					return new IntegerValue(Integer.parseInt(dataString), -1);
				case Constants.XmlStrings.XML_MEASUREMENT_MODE:
					return new ModeValue<Mode>(new Mode(dataString), -1);
				case Constants.XmlStrings.XML_MEASUREMENT_STRING:
					return new StringData(dataString, -1);
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

	void writeXml(XMLStreamWriter writer) throws XMLStreamException {

		writer.writeAttribute("id", this.id);
		writer.writeStartElement(this.data.getXmlId());
		writer.writeCharacters(this.data.toString());
		writer.writeEndElement();

	}

	/**
	 * @return the id
	 */
	public String getId() {
		return this.id;
	}

}
