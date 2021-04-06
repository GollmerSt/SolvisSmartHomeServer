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
import de.sgollmer.solvismax.model.update.Correction;
import de.sgollmer.xmllibrary.BaseCreator;
import de.sgollmer.xmllibrary.CreatorByXML;
import de.sgollmer.xmllibrary.XmlException;

public class SystemBackup {

	private final String id;
	private final Collection<IValue> values;
	private Solvis owner;

	private SystemBackup(String id, Collection<IValue> values) {
		this.id = id;
		this.values = values;
	}

	SystemBackup(String id) {
		this(id, new ArrayList<>());
	}

	public Collection<IValue> getValues() {
		return this.values;
	}

	public void add(IValue value) {
		this.values.add(value);
	}

	static class Creator extends CreatorByXML<SystemBackup> {

		private String id;
		private final Collection<IValue> values = new ArrayList<>();

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
		public SystemBackup create() throws XmlException, IOException {
			return new SystemBackup(this.id, this.values);
		}

		@Override
		public CreatorByXML<?> getCreator(QName name) {
			String id = name.getLocalPart();
			switch (id) {
				case Measurement.XML_MEASUREMENT:
					return new Measurement.Creator(id, this.getBaseCreator());
				case Correction.XML_CORRECTION:
					return new Correction.Creator(id, this.getBaseCreator());
			}
			return null;
		}

		@Override
		public void created(CreatorByXML<?> creator, Object created) {
			switch (creator.getId()) {
				case Measurement.XML_MEASUREMENT:
					this.values.add((IValue) created);
					break;
				case Correction.XML_CORRECTION:
					this.values.add((IValue) created);
					break;
			}

		}

	}

	void writeXml(XMLStreamWriter writer) throws XMLStreamException {
		writer.writeAttribute("id", this.id);
		for (IValue value : this.values) {
			value.writeXml(writer);

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
		this.values.clear();

	}

	public interface IValue {
		void writeXml(XMLStreamWriter writer) throws XMLStreamException;

		public String getId();

	}
}