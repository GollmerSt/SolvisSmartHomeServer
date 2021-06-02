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

public class Measurement implements SystemBackup.IValue {

	static final String XML_MEASUREMENT = "Measurement";

	private final String id;
	private final SingleData<?> data;

	public Measurement(final String id, final SingleData<?> data) {
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

		Creator(final String id, final BaseCreator<?> creator) {
			super(id, creator);
		}

		@Override
		public void setAttribute(final QName name, final String value) {
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
		public CreatorByXML<?> getCreator(final QName name) {
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
		public void created(final CreatorByXML<?> creator, final Object created) {
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

		private ValueCreator(final String id, final BaseCreator<?> creator) {
			super(id, creator);
		}

		@Override
		public void setAttribute(final QName name, final String value) {
		}

		private static class Mode implements IMode<Mode> {

			private final String data;

			public Mode(final String data) {
				this.data = data;
			}

			@Override
			public int compareTo(final Mode o) {
				return this.data.compareTo(o.data);
			}

			@Override
			public String getName() {
				return this.data;
			}

			@Override
			public ModeValue<?> create(final long timeStamp) {
				return new ModeValue<>(this, timeStamp);
			}

			@Override
			public boolean equals(final Object obj) {
				if (!(obj instanceof IMode)) {
					return false;
				}
				return this.getName().equals(((IMode<?>) obj).getName());
			}

			@Override
			public int hashCode() {
				return this.data.hashCode();
			}

			@Override
			public Handling getHandling() {
				return null;
			}

			@Override
			public String getCvsMeta() {
				return null;
			}
		}

		@Override
		public SingleData<?> create() throws XmlException, IOException {
			final String dataString = this.text.toString();
			switch (this.getId()) {
				case Constants.XmlStrings.XML_MEASUREMENT_BOOLEAN:
					return new BooleanValue(Boolean.parseBoolean(dataString), -1L);
				case Constants.XmlStrings.XML_MEASUREMENT_INTEGER:
					return new IntegerValue(Integer.parseInt(dataString), -1L);
				case Constants.XmlStrings.XML_MEASUREMENT_MODE:
					return new ModeValue<Mode>(new Mode(dataString), -1L);
				case Constants.XmlStrings.XML_MEASUREMENT_STRING:
					return new StringData(dataString, -1L);
			}
			return null;
		}

		@Override
		public CreatorByXML<?> getCreator(final QName name) {
			return null;
		}

		@Override
		public void created(final CreatorByXML<?> creator, final Object created) {
		}

		@Override
		public void addCharacters(final String data) {
			this.text.append(data.trim());
		}

	}

	@Override
	public void writeXml(final XMLStreamWriter writer) throws XMLStreamException {

		writer.writeStartElement(XML_MEASUREMENT);
		writer.writeAttribute("id", this.id);
		writer.writeStartElement(this.data.getXmlId());
		writer.writeCharacters(this.data.toString());
		writer.writeEndElement();
		writer.writeEndElement();

	}

	/**
	 * @return the id
	 */
	@Override
	public String getId() {
		return this.id;
	}

}
