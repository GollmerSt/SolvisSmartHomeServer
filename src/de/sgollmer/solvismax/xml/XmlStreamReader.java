package de.sgollmer.solvismax.xml;

import java.io.IOException;
import java.io.InputStream;
import java.util.Iterator;

import javax.xml.namespace.QName;
import javax.xml.stream.XMLEventReader;
import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLStreamConstants;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.events.Attribute;
import javax.xml.stream.events.XMLEvent;
import javax.xml.transform.stream.StreamSource;

import de.sgollmer.solvismax.error.XmlError;

public class XmlStreamReader<D> {

	public D read(StreamSource streamSource, String rootId, BaseCreator<D> rootCreator, String streamId)
			throws IOException, XmlError, XMLStreamException {

		XMLEventReader reader = null;
		XMLInputFactory inputFactory = XMLInputFactory.newInstance();
		try {
			reader = inputFactory.createXMLEventReader(streamSource);
		} catch (XMLStreamException e2) {
			throw new XmlError(e2, "Unexpected error on opening the file \"" + streamId + "\"");
		}

		D destination = null;

		while (reader.hasNext()) {
			XMLEvent ev = null;
			try {
				ev = reader.nextEvent();
			} catch (XMLStreamException e1) {
				throw new XmlError(e1, "XML syntax error in file \"" + streamId + "\"");
			}
			switch (ev.getEventType()) {
				case XMLStreamConstants.START_ELEMENT:
					QName name = ev.asStartElement().getName();
					String localName = name.getLocalPart();
					if (localName.equals(rootId)) {
						destination = this.create(rootCreator, reader, ev, streamId);
					} else {
						NullCreator nullCreator = new NullCreator(localName, rootCreator);
						this.create(nullCreator, reader, ev, streamId);
					}
					break;
			}
		}
		reader.close();
		return destination;
	}

	private <T> T create(CreatorByXML<T> creator, XMLEventReader reader, XMLEvent startEvent, String streamId) {

		for (@SuppressWarnings("unchecked")
		Iterator<Attribute> it = startEvent.asStartElement().getAttributes(); it.hasNext();) {
			Attribute attr = it.next();
			QName name = attr.getName();
			String value = attr.getValue();
			creator.setAttribute(name, value);
		}

		boolean finished = false;
		while (reader.hasNext() && !finished) {
			XMLEvent ev = null;
			try {
				ev = reader.nextEvent();
			} catch (XMLStreamException e1) {
				throw new XmlError(e1, "XML syntax error in file \"" + streamId + "\"");
			}
			switch (ev.getEventType()) {
				case XMLStreamConstants.END_ELEMENT:
					finished = true;
					break;
				case XMLStreamConstants.START_ELEMENT:
					QName name = ev.asStartElement().getName();
					CreatorByXML<?> child = creator.getCreator(name);
					if (child == null) {
						child = new NullCreator(name.getLocalPart(), creator.getBaseCreator());
					}
					creator.created(child, this.create(child, reader, ev, streamId));
					break;
			}
		}
		return creator.create();
	}

	private static class NullCreator extends CreatorByXML<Object> {

		public NullCreator(String id, BaseCreator<?> creator) {
			super(id, creator);
		}

		@Override
		public void setAttribute(QName name, String value) {
		}

		@Override
		public Object create() throws XmlError {
			return null;
		}

		@Override
		public void created(CreatorByXML<?> creator, Object created) {
		}

		@Override
		public CreatorByXML<?> getCreator(QName name) {
			return new NullCreator(name.getLocalPart(), this.getBaseCreator());
		}

	}
}
