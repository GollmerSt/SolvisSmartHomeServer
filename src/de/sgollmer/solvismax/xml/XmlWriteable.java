package de.sgollmer.solvismax.xml;

import java.io.IOException;

import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamWriter;

public interface XmlWriteable {
	public void writeXml(XMLStreamWriter writer) throws XMLStreamException, IOException ;
}