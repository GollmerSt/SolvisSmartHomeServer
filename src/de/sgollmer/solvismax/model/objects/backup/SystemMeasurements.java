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

import de.sgollmer.solvismax.error.XmlError;
import de.sgollmer.solvismax.model.Solvis;
import de.sgollmer.solvismax.xml.BaseCreator;
import de.sgollmer.solvismax.xml.CreatorByXML;

public class SystemMeasurements {
	
	private static final String XML_MEASUREMENTS_MEASUREMENT = "Measurement";

	private final String id ;
	private final Collection<Measurement> measurements;
	private Solvis owner ;

	public SystemMeasurements(String id, Collection<Measurement> measurements) {
		this.id = id ;
		this.measurements = measurements;
	}

	public SystemMeasurements( String id ) {
		this(id, new ArrayList<>());
	}

	public Collection<Measurement> getMeasurements() {
		return this.measurements;
	}

	public void add(Measurement measurement) {
		this.measurements.add(measurement);
	}

	public static class Creator extends CreatorByXML<SystemMeasurements> {

		private String id ;
		private final Collection<Measurement> measurements = new ArrayList<>();

		public Creator(String id, BaseCreator<?> creator) {
			super(id, creator);
		}

		@Override
		public void setAttribute(QName name, String value) {
			switch ( name.getLocalPart() ) {
				case "id":
					this.id = value ;
					break ;
			}
		}

		@Override
		public SystemMeasurements create() throws XmlError, IOException {
			return new SystemMeasurements(this.id, this.measurements);
		}

		@Override
		public CreatorByXML<?> getCreator(QName name) {
			String id = name.getLocalPart();
			switch (id) {
				case XML_MEASUREMENTS_MEASUREMENT:
					return new Measurement.Creator(id, this.getBaseCreator() );
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

	public void writeXml(XMLStreamWriter writer) throws XMLStreamException {
		writer.writeAttribute("id", this.id);
		for ( Measurement measurement: this.measurements ) {
			writer.writeStartElement(XML_MEASUREMENTS_MEASUREMENT);
			measurement.writeXml(writer);
			writer.writeEndElement();

		}
		
	}

	public String getId() {
		return this.id;
	}

	public Solvis getOwner() {
		return this.owner;
	}

	public void setOwner(Solvis owner) {
		this.owner = owner;
	}

	public void clear() {
		this.measurements.clear();
		
	}
}
