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

import de.sgollmer.solvismax.helper.Helper.Reference;
import de.sgollmer.solvismax.model.Solvis;
import de.sgollmer.xmllibrary.BaseCreator;
import de.sgollmer.xmllibrary.CreatorByXML;
import de.sgollmer.xmllibrary.XmlException;

public class SystemBackup {

	private final String id;
	private final Collection<IValue> values;
	private Solvis owner;
	private final Reference<Long> timeOfLastBackup;

	private SystemBackup(final String id, final Collection<IValue> values, final Reference<Long> timeOfLastBackup) {
		this.id = id;
		this.values = values;
		this.timeOfLastBackup = timeOfLastBackup;
	}

	SystemBackup(final String id, final Reference<Long> timeOfLastBackup) {
		this(id, new ArrayList<>(), timeOfLastBackup);
	}

	public Collection<IValue> getValues() {
		return this.values;
	}

	public void add(final IValue value) {
		this.values.add(value);
	}

	static class Creator extends CreatorByXML<SystemBackup> {

		private String id;
		private final Collection<IValue> values = new ArrayList<>();
		private final Reference<Long> timeOfLastBackup;

		Creator(final String id, final BaseCreator<?> creator, final Reference<Long> timeOfLastBackup) {
			super(id, creator);
			this.timeOfLastBackup = timeOfLastBackup;
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
		public SystemBackup create() throws XmlException, IOException {
			return new SystemBackup(this.id, this.values, this.timeOfLastBackup);
		}

		@Override
		public CreatorByXML<?> getCreator(final QName name) {
			String id = name.getLocalPart();
			switch (id) {
				case Measurement.XML_MEASUREMENT:
					return new Measurement.Creator(id, this.getBaseCreator());
			}
			return null;
		}

		@Override
		public void created(final CreatorByXML<?> creator, final Object created) {
			switch (creator.getId()) {
				case Measurement.XML_MEASUREMENT:
					this.values.add((IValue) created);
					break;
			}

		}

	}

	void writeXml(final XMLStreamWriter writer) throws XMLStreamException {
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

	void setOwner(final Solvis owner) {
		this.owner = owner;
	}

	public void clear() {
		this.values.clear();

	}

	public long getTimeOfLastBackup() {
		return this.timeOfLastBackup.get();
	}

	public interface IValue {
		void writeXml(final XMLStreamWriter writer) throws XMLStreamException;

		public String getId();

	}
}
