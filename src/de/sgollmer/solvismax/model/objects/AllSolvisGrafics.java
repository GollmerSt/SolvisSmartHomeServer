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
import de.sgollmer.solvismax.imagepatternrecognition.image.MyImage;
import de.sgollmer.solvismax.model.objects.screen.ScreenGraficData;
import de.sgollmer.solvismax.model.objects.screen.ScreenGraficDescription;
import de.sgollmer.solvismax.xml.BaseCreator;
import de.sgollmer.solvismax.xml.CreatorByXML;
import de.sgollmer.solvismax.xml.XmlWriteable;

public class AllSolvisGrafics implements XmlWriteable {
	
	private static final String XML_SYSTEM = "System" ;

	private Collection<SystemGrafics> systems;
	private int currentControlFileHashCode = 0;

	private AllSolvisGrafics(Collection<SystemGrafics> systems) {
		this.systems = systems;
	}

	public AllSolvisGrafics() {
		this.systems = new ArrayList<>();
	}

	public SystemGrafics get(String id) {
		SystemGrafics result = null;
		for (SystemGrafics system : systems) {
			if (system.getId().equals(id)) {
				result = system;
			}
		}

		if (result != null && result.getControlFileHashCode() != this.currentControlFileHashCode) {
			result.clear();
			result.setControlFileHashCode(this.currentControlFileHashCode);
		}

		if (result == null) {
			result = new SystemGrafics(id, currentControlFileHashCode);
			this.systems.add(result);
		}

		return result;
	}

	public ScreenGraficData get(String systemId, ScreenGraficDescription description) {
		SystemGrafics system = this.get(systemId);
		return system.get(description.getId());
	}

	public void put(String systemId, ScreenGraficDescription description, MyImage image) {
		SystemGrafics system = this.get(systemId);
		if (system == null) {
			system = new SystemGrafics(systemId, this.currentControlFileHashCode);
		}
		system.put(description.getId(), image);
	}

	@Override
	public void writeXml(XMLStreamWriter writer) throws XMLStreamException, IOException {
		for (SystemGrafics system : systems) {
			writer.writeStartElement(XML_SYSTEM);
			system.writeXml(writer);
			writer.writeEndElement();
		}

	}

	public static class Creator extends BaseCreator<AllSolvisGrafics> {

		private Collection<SystemGrafics> systems = new ArrayList<>();

		public Creator(String id) {
			super(id);
		}

		@Override
		public void setAttribute(QName name, String value) {
		}

		@Override
		public AllSolvisGrafics create() throws XmlError, IOException {
			return new AllSolvisGrafics(systems);
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

	public void setCurrentControlFileHashCode(int hashCode) {
		this.currentControlFileHashCode = hashCode ;
	}

}
