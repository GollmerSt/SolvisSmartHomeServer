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
import java.util.Iterator;

import javax.xml.namespace.QName;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamWriter;

import de.sgollmer.solvismax.xml.ControlFileReader.Hashes;
import de.sgollmer.xmllibrary.BaseCreator;
import de.sgollmer.xmllibrary.CreatorByXML;
import de.sgollmer.xmllibrary.IXmlWriteable;
import de.sgollmer.xmllibrary.XmlException;

public class AllSolvisGrafics implements IXmlWriteable {

	private static final String XML_SYSTEM = "System";

	private final Collection<SystemGrafics> systems;
	private Long controlResourceHashCode;
	private Long controlFileHashCode;

	private AllSolvisGrafics(Collection<SystemGrafics> systems, Long controlResourceHashCode, Long controlFileHashCode) {
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
		if (this.controlResourceHashCode == null || !this.controlResourceHashCode.equals(hashes.getResourceHash())
				|| this.controlFileHashCode == null || !this.controlFileHashCode.equals(hashes.getFileHash())) {
			this.systems.clear();
			this.controlResourceHashCode = hashes.getResourceHash();
			this.controlFileHashCode = hashes.getFileHash();
		}
		SystemGrafics result = null;
		boolean finish = false;
		for (Iterator<SystemGrafics> it = this.systems.iterator(); it.hasNext() && !finish;) {
			SystemGrafics system = it.next();
			if (system.getId().equals(unitId)) {
				result = system;
				finish = true;
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
			writer.writeAttribute("controlResourceHashCode", Long.toString(this.controlResourceHashCode));
			writer.writeAttribute("controlFileHashCode", Long.toString(this.controlFileHashCode));
			writer.writeStartElement(XML_SYSTEM);
			system.writeXml(writer);
			writer.writeEndElement();
		}

	}

	public static class Creator extends BaseCreator<AllSolvisGrafics> {

		private Collection<SystemGrafics> systems = new ArrayList<>();
		private Long controlResourceHashCode = null;
		private Long controlFileHashCode = null;

		public Creator(String id) {
			super(id);
		}

		@Override
		public void setAttribute(QName name, String value) {
			switch (name.getLocalPart()) {
				case "controlResourceHashCode":
					this.controlResourceHashCode = Long.parseLong(value);
					break;
				case "controlFileHashCode":
					this.controlFileHashCode = Long.parseLong(value);
					break;
			}
		}

		@Override
		public AllSolvisGrafics create() throws XmlException, IOException {
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
