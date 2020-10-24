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

import de.sgollmer.xmllibrary.BaseCreator;
import de.sgollmer.xmllibrary.CreatorByXML;
import de.sgollmer.xmllibrary.XmlException;

public class Measurements {

	private static final String XML_MEASUREMENTS_SYSTEM_MEASUREMENTS = "SystemMeasurements";

	private final Collection<SystemMeasurements> systemMeasurements;

	private Measurements(Collection<SystemMeasurements> systemMeasurements) {
		this.systemMeasurements = systemMeasurements;
	}

	Measurements() {
		this(new ArrayList<>());
	}

	SystemMeasurements get(String id) {
		SystemMeasurements result = null;
		for (SystemMeasurements measurements : this.systemMeasurements) {
			if (id.equals(measurements.getId())) {
				result = measurements;
				break;
			}
		}
		if (result == null) {
			result = new SystemMeasurements(id);
			this.systemMeasurements.add(result);
		}
		return result;
	}

	static class Creator extends BaseCreator<Measurements> {

		private final Measurements measurements;

		public Creator(Measurements measurements, String id) {
			super(id);
			this.measurements = measurements;
			this.measurements.systemMeasurements.clear();
		}

		@Override
		public void setAttribute(QName name, String value) {
		}

		@Override
		public Measurements create() throws XmlException, IOException {
			return this.measurements;
		}

		@Override
		public CreatorByXML<?> getCreator(QName name) {
			String id = name.getLocalPart();
			switch (id) {
				case XML_MEASUREMENTS_SYSTEM_MEASUREMENTS:
					return new SystemMeasurements.Creator(id, this.getBaseCreator());
			}
			return null;
		}

		@Override
		public void created(CreatorByXML<?> creator, Object created) {
			switch (creator.getId()) {
				case XML_MEASUREMENTS_SYSTEM_MEASUREMENTS:
					this.measurements.systemMeasurements.add((SystemMeasurements) created);
			}
		}

	}

	void writeXml(XMLStreamWriter writer) throws XMLStreamException {
		for (SystemMeasurements system : this.systemMeasurements) {
			writer.writeStartElement(XML_MEASUREMENTS_SYSTEM_MEASUREMENTS);
			system.writeXml(writer);
			writer.writeEndElement();
		}
	}

	/**
	 * @return the systemMeasurements
	 */
	Collection<SystemMeasurements> getSystemMeasurements() {
		return this.systemMeasurements;
	}
}
