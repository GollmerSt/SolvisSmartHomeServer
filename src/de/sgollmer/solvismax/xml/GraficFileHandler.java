/************************************************************************
 * 
 * $Id$
 *
 * 
 ************************************************************************/

package de.sgollmer.solvismax.xml;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;

import javax.xml.stream.XMLOutputFactory;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamWriter;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import de.sgollmer.solvismax.Constants;
import de.sgollmer.solvismax.error.FileError;
import de.sgollmer.solvismax.error.XmlError;
import de.sgollmer.solvismax.helper.FileHelper;
import de.sgollmer.solvismax.model.Solvis;
import de.sgollmer.solvismax.model.objects.AllSolvisGrafics;

public class GraficFileHandler {

	private static final Logger logger = LogManager.getLogger(Solvis.class);
	private static final String NAME_XSD_GRAFICSFILE = "graficData.xsd";
	private static final String NAME_XML_GRAFICSFILE = "graficData.xml";

	private final File parent;

	public GraficFileHandler(String pathName) {
		File parent;

		if (pathName == null) {
			pathName = System.getProperty("user.home");
			if (System.getProperty("os.name").startsWith("Windows")) {
				pathName = System.getenv("APPDATA");
			}

		}

		pathName += File.separator + Constants.RESOURCE_DESTINATION_PATH;
		parent = new File(pathName);
		this.parent = parent;
	}

	private void copyFiles() throws IOException {

		boolean success = true;

		if (!parent.exists()) {
			success = parent.mkdir();
		}

		if (!success) {
			throw new FileError("Error on creating directory <" + parent.getAbsolutePath() + ">");
		}

		File xsd = new File(this.parent, NAME_XSD_GRAFICSFILE);

		FileHelper.copyFromResource(Constants.RESOURCE_PATH + File.separator + NAME_XSD_GRAFICSFILE, xsd);

	}

	public AllSolvisGrafics read() throws IOException, XmlError, XMLStreamException {

		this.copyFiles();

		File xml = new File(this.parent, NAME_XML_GRAFICSFILE);

		if (!xml.exists()) {
			AllSolvisGrafics grafics = new AllSolvisGrafics();
			return grafics;
		}

		InputStream source = new FileInputStream(xml);

		XmlStreamReader<AllSolvisGrafics> reader = new XmlStreamReader<>();

		String rootId = "SolvisGrafics";

		AllSolvisGrafics result;

		try {

			result = reader.read(source, rootId, new AllSolvisGrafics.Creator(rootId), xml.getName()).getTree();

		} catch (IOException | XmlError | XMLStreamException e) {
			logger.error("Warning: Read error on graficx.xml file. A new one will be created.");
			result = new AllSolvisGrafics();
		}

		return result;
	}

	public void write(AllSolvisGrafics grafics) throws IOException, XMLStreamException {
		this.copyFiles();

		File output = new File(parent, NAME_XML_GRAFICSFILE);

		XMLOutputFactory factory = XMLOutputFactory.newInstance();
		XMLStreamWriter writer = factory.createXMLStreamWriter(new FileOutputStream(output));
		writer.writeStartDocument();
		writer.writeStartElement("SolvisGrafics");
		grafics.writeXml(writer);
		writer.writeEndDocument();
		writer.flush();
		writer.close();
	}

}
