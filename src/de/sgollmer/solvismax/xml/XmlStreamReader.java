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

import org.slf4j.LoggerFactory;

import de.sgollmer.solvismax.error.XmlError;

public class XmlStreamReader<D> {

	@SuppressWarnings("unused")
	private static final org.slf4j.Logger logger = LoggerFactory.getLogger(XmlStreamReader.class);

	public static class Result<T> {
		private final T tree;
		private final int hash;

		public Result(T tree, int hash) {
			this.tree = tree;
			this.hash = hash;
		}

		public T getTree() {
			return tree;
		}

		public int getHash() {
			return hash;
		}
	}

	private static class HashContainer {
		private int hash = 61;

		private void put(Object obj) {
			this.hash = 397 * this.hash + 43 * obj.hashCode();
		}
	}

	public Result<D> read(InputStream inputStream, String rootId, BaseCreator<D> rootCreator, String streamId)
			throws IOException, XmlError, XMLStreamException {

		final HashContainer hash = new HashContainer();

		XMLEventReader reader = null;
		XMLInputFactory inputFactory = XMLInputFactory.newInstance();
		try {
			reader = inputFactory.createXMLEventReader(inputStream);
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
					hash.put(name);
					String localName = name.getLocalPart();
					if (localName.equals(rootId)) {
						destination = this.create(rootCreator, reader, ev, streamId, hash);
					} else {
						NullCreator nullCreator = new NullCreator(localName, rootCreator);
						this.create(nullCreator, reader, ev, streamId, hash);
					}
					break;
			}
		}
		reader.close();

		inputStream.close();

		return new Result<D>(destination, hash.hash);
	}

	private <T> T create(CreatorByXML<T> creator, XMLEventReader reader, XMLEvent startEvent, String streamId,
			HashContainer hash) throws XmlError, IOException {

		for (@SuppressWarnings("unchecked")
		Iterator<Attribute> it = startEvent.asStartElement().getAttributes(); it.hasNext();) {
			Attribute attr = it.next();
			QName name = attr.getName();
			hash.put(name);
			String value = attr.getValue();
			hash.put(value);
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
					hash.put(name);
					CreatorByXML<?> child = creator.getCreator(name);
					if (child == null) {
						child = new NullCreator(name.getLocalPart(), creator.getBaseCreator());
					}
					creator.created(child, this.create(child, reader, ev, streamId, hash));
					break;
				case XMLStreamConstants.CHARACTERS:
					String text = ev.asCharacters().getData();
					hash.put(text);
					creator.addCharacters(text);
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
