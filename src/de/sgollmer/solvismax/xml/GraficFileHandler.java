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

import de.sgollmer.solvismax.Constants;
import de.sgollmer.solvismax.error.FileException;
import de.sgollmer.solvismax.error.XmlException;
import de.sgollmer.solvismax.helper.FileHelper;
import de.sgollmer.solvismax.log.LogManager;
import de.sgollmer.solvismax.log.LogManager.Level;
import de.sgollmer.solvismax.log.LogManager.ILogger;
import de.sgollmer.solvismax.model.Solvis;
import de.sgollmer.solvismax.model.objects.AllSolvisGrafics;

public class GraficFileHandler {

	private static final ILogger logger = LogManager.getInstance().getLogger(Solvis.class);
	private static final String NAME_XSD_GRAFICSFILE = "graficData.xsd";
	private static final String NAME_XML_GRAFICSFILE = "graficData.xml";

	private final File parent;

	public GraficFileHandler(File writePath) {

		if (writePath == null) {
			String pathName = System.getProperty("user.home");
			if (System.getProperty("os.name").startsWith("Windows")) {
				pathName = System.getenv("APPDATA");
			}
			writePath = new File(pathName);
		}

		this.parent = new File(writePath, Constants.Pathes.RESOURCE_DESTINATION);
	}

	private void copyFiles() throws IOException, FileException {

		boolean success = true;

		if (!this.parent.exists()) {
			success = FileHelper.mkdir(this.parent);
		}

		if (!success) {
			throw new FileException("Error on creating directory <" + this.parent.getAbsolutePath() + ">");
		}

		File xsd = new File(this.parent, NAME_XSD_GRAFICSFILE);

		FileHelper.copyFromResource(Constants.Pathes.RESOURCE + '/' + NAME_XSD_GRAFICSFILE, xsd);

	}

	public AllSolvisGrafics read() throws IOException, XmlException, XMLStreamException, FileException {

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

		} catch (IOException | XmlException | XMLStreamException e1) {
			logger.error("Warning: Read error on grafics.xml file. A new one will be created.");
			result = new AllSolvisGrafics();
		} catch (Throwable e2) {
			LogManager.out(logger, Level.ERROR,
					"Unexpected error found on reading the grafics.xml, a new on will be cerated.\n"
							+ "If the error still exits, please conact the developer.",
					e2.getStackTrace());
			result = new AllSolvisGrafics();
		}

		return result;
	}

	public void write(AllSolvisGrafics grafics) throws IOException, XMLStreamException, FileException {
		this.copyFiles();

		File output = new File(this.parent, NAME_XML_GRAFICSFILE);

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
