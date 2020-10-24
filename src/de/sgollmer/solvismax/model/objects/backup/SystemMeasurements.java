/************************************************************************
 * 
 * $Id$
 *
 * 
 ************************************************************************/

package de.sgollmer.solvismax.model.objects.backup;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;

import javax.xml.namespace.QName;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamWriter;

import de.sgollmer.solvismax.model.Solvis;
import de.sgollmer.xmllibrary.BaseCreator;
import de.sgollmer.xmllibrary.CreatorByXML;
import de.sgollmer.xmllibrary.XmlException;

public class SystemMeasurements {

	private static final String XML_MEASUREMENTS_MEASUREMENT = "Measurement";

	private final String id;
	private final Collection<Measurement> measurements;
	private Solvis owner;

	private SystemMeasurements(String id, Collection<Measurement> measurements) {
		this.id = id;
		this.measurements = measurements;
	}

	SystemMeasurements(String id) {
		this(id, new ArrayList<>());
	}

	public Collection<Measurement> getMeasurements() {
		return this.measurements;
	}

	public void add(Measurement measurement) {
		this.measurements.add(measurement);
	}

	static class Creator extends CreatorByXML<SystemMeasurements> {

		private String id;
		private final Collection<Measurement> measurements = new ArrayList<>();

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
		public SystemMeasurements create() throws XmlException, IOException {
			return new SystemMeasurements(this.id, this.measurements);
		}

		@Override
		public CreatorByXML<?> getCreator(QName name) {
			String id = name.getLocalPart();
			switch (id) {
				case XML_MEASUREMENTS_MEASUREMENT:
					return new Measurement.Creator(id, this.getBaseCreator());
			}
			return null;
		}

		@Override
		public void created(CreatorByXML<?> creator, Object created) {
			switch (creator.getId()) {
				case XML_MEASUREMENTS_MEASUREMENT:
					this.measurements.add((Measurement) created);
					break;
			}

		}

	}

	void writeXml(XMLStreamWriter writer) throws XMLStreamException {
		writer.writeAttribute("id", this.id);
		for (Measurement measurement : this.measurements) {
			writer.writeStartElement(XML_MEASUREMENTS_MEASUREMENT);
			measurement.writeXml(writer);
			writer.writeEndElement();

		}

	}

	String getId() {
		return this.id;
	}

	Solvis getOwner() {
		return this.owner;
	}

	void setOwner(Solvis owner) {
		this.owner = owner;
	}

	public void clear() {
		this.measurements.clear();

	}
}
