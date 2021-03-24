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

public class AllSystemBackups {

	private static final String XML_MEASUREMENTS_SYSTEM_MEASUREMENTS = "SystemMeasurements";
	private static final String XML_MEASUREMENTS_SYSTEM_BACKUP = "SystemBackup";

	private final Collection<SystemBackup> systemBackups;

	private AllSystemBackups(Collection<SystemBackup> systemBackups) {
		this.systemBackups = systemBackups;
	}

	AllSystemBackups() {
		this(new ArrayList<>());
	}

	SystemBackup get(String id) {
		SystemBackup result = null;
		for (SystemBackup measurements : this.systemBackups) {
			if (id.equals(measurements.getId())) {
				result = measurements;
				break;
			}
		}
		if (result == null) {
			result = new SystemBackup(id);
			this.systemBackups.add(result);
		}
		return result;
	}

	static class Creator extends BaseCreator<AllSystemBackups> {

		private final AllSystemBackups measurements;

		public Creator(AllSystemBackups measurements, String id) {
			super(id);
			this.measurements = measurements;
			this.measurements.systemBackups.clear();
		}

		@Override
		public void setAttribute(QName name, String value) {
		}

		@Override
		public AllSystemBackups create() throws XmlException, IOException {
			return this.measurements;
		}

		@Override
		public CreatorByXML<?> getCreator(QName name) {
			String id = name.getLocalPart();
			switch (id) {
				case XML_MEASUREMENTS_SYSTEM_MEASUREMENTS:
				case XML_MEASUREMENTS_SYSTEM_BACKUP:
					return new SystemBackup.Creator(id, this.getBaseCreator());
			}
			return null;
		}

		@Override
		public void created(CreatorByXML<?> creator, Object created) {
			switch (creator.getId()) {
				case XML_MEASUREMENTS_SYSTEM_MEASUREMENTS:
				case XML_MEASUREMENTS_SYSTEM_BACKUP:
					this.measurements.systemBackups.add((SystemBackup) created);
			}
		}

	}

	void writeXml(XMLStreamWriter writer) throws XMLStreamException {
		for (SystemBackup system : this.systemBackups) {
			writer.writeStartElement(XML_MEASUREMENTS_SYSTEM_BACKUP);
			system.writeXml(writer);
			writer.writeEndElement();
		}
	}

	/**
	 * @return the systemBackups
	 */
	Collection<SystemBackup> getSystemBackups() {
		return this.systemBackups;
	}
}
