/************************************************************************
 * 
 * $Id$
 *
 * 
 ************************************************************************/

package de.sgollmer.solvismax.model.objects;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;

import javax.xml.namespace.QName;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamWriter;

import de.sgollmer.solvismax.error.XmlError;
import de.sgollmer.solvismax.xml.BaseCreator;
import de.sgollmer.solvismax.xml.ControlFileReader.Hashes;
import de.sgollmer.solvismax.xml.CreatorByXML;
import de.sgollmer.solvismax.xml.IXmlWriteable;

public class AllSolvisGrafics implements IXmlWriteable {

	private static final String XML_SYSTEM = "System";

	private final Collection<SystemGrafics> systems;
	private Integer controlResourceHashCode;
	private Integer controlFileHashCode;

	private AllSolvisGrafics(Collection<SystemGrafics> systems, Integer controlResourceHashCode,
			Integer controlFileHashCode) {
		this.systems = systems;
		this.controlResourceHashCode = controlResourceHashCode;
		this.controlFileHashCode = controlFileHashCode;
	}

	public AllSolvisGrafics() {
		this.systems = new ArrayList<>();
		this.controlResourceHashCode = null;
		this.controlFileHashCode = null;
	}

	public SystemGrafics get(String unitId, Hashes hashes) {
		if (this.controlResourceHashCode == null || !this.controlResourceHashCode.equals(hashes.getResourceHash())) {
			this.systems.clear();
			this.controlResourceHashCode = hashes.getResourceHash();
			this.controlFileHashCode = hashes.getFileHash();
		}
		if (this.controlFileHashCode == null || !this.controlFileHashCode.equals(hashes.getFileHash())) {
			this.systems.clear();
		}
		SystemGrafics result = null;
		for (SystemGrafics system : this.systems) {
			if (system.getId().equals(unitId)) {
				result = system;
			}
		}
		if (result == null) {
			result = new SystemGrafics(unitId);
			this.systems.add(result);
		}

		return result;
	}

	@Override
	public void writeXml(XMLStreamWriter writer) throws XMLStreamException, IOException {
		for (SystemGrafics system : this.systems) {
			writer.writeAttribute("controlResourceHashCode", Integer.toString(this.controlResourceHashCode));
			writer.writeAttribute("controlFileHashCode", Integer.toString(this.controlFileHashCode));
			writer.writeStartElement(XML_SYSTEM);
			system.writeXml(writer);
			writer.writeEndElement();
		}

	}

	public static class Creator extends BaseCreator<AllSolvisGrafics> {

		private Collection<SystemGrafics> systems = new ArrayList<>();
		private Integer controlResourceHashCode = null;
		private Integer controlFileHashCode = null;

		public Creator(String id) {
			super(id);
		}

		@Override
		public void setAttribute(QName name, String value) {
			switch (name.getLocalPart()) {
				case "controlResourceHashCode":
					this.controlResourceHashCode = Integer.parseInt(value);
					break;
				case "controlFileHashCode":
					this.controlFileHashCode = Integer.parseInt(value);
					break;
			}
		}

		@Override
		public AllSolvisGrafics create() throws XmlError, IOException {
			return new AllSolvisGrafics(this.systems, this.controlResourceHashCode, this.controlFileHashCode);
		}

		@Override
		public CreatorByXML<?> getCreator(QName name) {
			String id = name.getLocalPart();
			switch (id) {
				case XML_SYSTEM:
					return new SystemGrafics.Creator(id, this.getBaseCreator());
			}
			return null;
		}

		@Override
		public void created(CreatorByXML<?> creator, Object created) {
			switch (creator.getId()) {
				case XML_SYSTEM:
					this.systems.add((SystemGrafics) created);
					break;
			}

		}

	}

	public Hashes getControlHashCodes() {
		return new Hashes(this.controlResourceHashCode, this.controlFileHashCode);
	}
}
