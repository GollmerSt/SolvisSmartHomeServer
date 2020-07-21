/************************************************************************
 * 
 * $Id$
 *
 * 
 ************************************************************************/

package de.sgollmer.solvismax.xml;

import java.io.IOException;
import java.io.InputStream;
import java.util.Iterator;

import javax.xml.XMLConstants;
import javax.xml.namespace.QName;
import javax.xml.stream.XMLEventReader;
import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLStreamConstants;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.events.Attribute;
import javax.xml.stream.events.XMLEvent;
import javax.xml.transform.stream.StreamSource;
import javax.xml.validation.Schema;
import javax.xml.validation.SchemaFactory;
import javax.xml.validation.Validator;

import org.xml.sax.SAXParseException;

import de.sgollmer.solvismax.Constants;
import de.sgollmer.solvismax.error.XmlError;
import de.sgollmer.solvismax.log.LogManager;
import de.sgollmer.solvismax.log.LogManager.DelayedMessage;
import de.sgollmer.solvismax.log.LogManager.Level;

public class XmlStreamReader<D> {

	public static class Result<T> {
		private final T tree;
		private final int hash;

		private Result(T tree, int hash) {
			this.tree = tree;
			this.hash = hash;
		}

		public T getTree() {
			return this.tree;
		}

		int getHash() {
			return this.hash;
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
		return this.read(inputStream, rootId, rootCreator, streamId, false);
	}

	private Result<D> read(InputStream inputStream, String rootId, BaseCreator<D> rootCreator, String streamId,
			boolean hashOnly) throws IOException, XmlError, XMLStreamException {

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
					if (localName.equals(rootId) || hashOnly) {
						destination = this.create(rootCreator, reader, ev, streamId, hash, hashOnly);
					} else {
						NullCreator nullCreator = new NullCreator(localName, rootCreator);
						this.create(nullCreator, reader, ev, streamId, hash, hashOnly);
					}
					break;
			}
		}
		reader.close();

		inputStream.close();

		return new Result<D>(destination, hash.hash);
	}

	private <T> T create(CreatorByXML<T> creator, XMLEventReader reader, XMLEvent startEvent, String streamId,
			HashContainer hash, boolean hashOnly) throws XmlError, IOException {

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
			try {
				switch (ev.getEventType()) {
					case XMLStreamConstants.END_ELEMENT:
						finished = true;
						break;
					case XMLStreamConstants.START_ELEMENT:
						QName name = ev.asStartElement().getName();
						hash.put(name);
						CreatorByXML<?> child = creator.getCreator(name);
						if (child == null || hashOnly) {
							child = new NullCreator(name.getLocalPart(), creator.getBaseCreator());
						}
						creator.created(child, this.create(child, reader, ev, streamId, hash, hashOnly));
						break;
					case XMLStreamConstants.CHARACTERS:
						String text = ev.asCharacters().getData();
						hash.put(text);
						creator.addCharacters(text);
				}
			} catch (XmlError e) {
				throw new XmlError(
						"XML error on line " + ev.getLocation().getLineNumber() + " occured:\n" + e.getMessage());
			}
		}
		return creator.create();
	}

	private static class NullCreator extends CreatorByXML<Object> {

		private NullCreator(String id, BaseCreator<?> creator) {
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

	boolean validate(InputStream xml, InputStream xsd) {
		LogManager logManager = LogManager.getInstance();
		try {
			SchemaFactory factory = SchemaFactory.newInstance(XMLConstants.W3C_XML_SCHEMA_NS_URI);
			Schema schema;
			schema = factory.newSchema(new StreamSource(xsd));
			Validator validator = schema.newValidator();
			validator.validate(new StreamSource(xml));
			xml.close();
			xsd.close();
		} catch (SAXParseException ex) {
			String message = "Line: " + ex.getLineNumber() + ", Column: " + ex.getColumnNumber() + " Error: "
					+ ex.getMessage();
			logManager.addDelayedErrorMessage(new DelayedMessage(Level.WARN, message, XmlStreamReader.class,
					Constants.ExitCodes.XML_VERIFICATION_ERROR));
			return false;
		} catch (Exception e) {
			logManager.addDelayedErrorMessage(new DelayedMessage(Level.WARN, e.getMessage(), XmlStreamReader.class,
					Constants.ExitCodes.XML_VERIFICATION_ERROR));
			return false;
		}
		return true;
	}
}
