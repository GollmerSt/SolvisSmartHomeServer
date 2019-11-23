package de.sgollmer.solvismax.xml;

import java.io.File;
import java.io.IOException;
import java.util.Iterator;

import javax.xml.namespace.QName;
import javax.xml.stream.XMLEventReader;
import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLStreamConstants;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.events.Attribute;
import javax.xml.stream.events.XMLEvent;
import javax.xml.transform.stream.StreamSource;

import de.sgollmer.solvismax.error.FileError;
import de.sgollmer.solvismax.error.XmlError;
import de.sgollmer.solvismax.helper.FileHelper;
import de.sgollmer.solvismax.model.objects.SolvisDescription;

public class ControlFileReader {

	private static final String NAME_XML_CONTROLFILE = "control.xml";
	private static final String NAME_XSD_CONTROLFILE = "control.xsd";
	private static final String RELATIVE_SOURCE_PATH = "data/";

	private final File parent;
	private boolean copied = false;


	public ControlFileReader(String pathName) {
		File parent;

		if (pathName == null) {
			String writeDirectory = System.getProperty("user.home");
			if (System.getProperty("os.name").startsWith("Windows")) {
				writeDirectory = System.getenv("APPDATA");
			}

			writeDirectory += File.separator + "SolvisMaxJava";

			parent = new File(writeDirectory);
		} else {
			parent = new File(pathName);
		}
		this.parent = parent;
	}

	private void copyFiles() throws IOException {

		if (copied) {
			return;
		}
		boolean success = true;

		if (!parent.exists()) {
			success = parent.mkdir();
		}

		if (!success) {
			throw new FileError("Error on creating directory <" + parent.getAbsolutePath() + ">");
		}

		File xml = new File(this.parent, NAME_XML_CONTROLFILE);

		if (!xml.exists()) {
			FileHelper.copyFromResource(RELATIVE_SOURCE_PATH + NAME_XML_CONTROLFILE, xml);
		}

		File xsd = new File(this.parent, NAME_XSD_CONTROLFILE);

		FileHelper.copyFromResource(RELATIVE_SOURCE_PATH + NAME_XSD_CONTROLFILE, xsd);

	}

	public SolvisDescription read() throws IOException, XmlError, XMLStreamException {

		this.copyFiles();

		File xml = new File(this.parent, NAME_XML_CONTROLFILE);

		StreamSource source = new StreamSource(xml);
		SolvisDescription solvisDescription = null;

		XMLEventReader reader = null;
		XMLInputFactory inputFactory = XMLInputFactory.newInstance();
		try {
			reader = inputFactory.createXMLEventReader(source);
		} catch (XMLStreamException e2) {
			throw new XmlError(e2, "Unexpected error on opening the file \"" + xml.getName() + "\"");
		}

		SolvisDescription.Creator baseCreator = new SolvisDescription.Creator("SolvisDescrition");

		while (reader.hasNext()) {
			XMLEvent ev = null;
			try {
				ev = reader.nextEvent();
			} catch (XMLStreamException e1) {
				throw new XmlError(e1, "XML syntax error in file \"" + xml.getName() + "\"");
			}
			switch (ev.getEventType()) {
				case XMLStreamConstants.START_ELEMENT:
					QName name = ev.asStartElement().getName();
					String localName = name.getLocalPart();
					if (!localName.equals("SolvisDescrition")) {
						solvisDescription = this.create(baseCreator, reader, ev, xml);
					} else {
						NullCreator nullCreator = new NullCreator(name.getLocalPart(), baseCreator);
						this.create(nullCreator, reader, ev, xml);
					}
					break;
			}
		}
		reader.close();
		return solvisDescription ;
	}

	private <T> T create(CreatorByXML<T> creator, XMLEventReader reader, XMLEvent startEvent, File xml) {

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
				throw new XmlError(e1, "XML syntax error in file \"" + xml.getName() + "\"");
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
					creator.created(child, this.create(child, reader, ev, xml));
					break;
			}
		}
		return creator.create();
	}

	public static void main(String[] args) throws IOException, XmlError, XMLStreamException {

		ControlFileReader reader = new ControlFileReader(null);
		reader.read();
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
