package de.sgollmer.solvismax.xml;

import java.io.File;
import java.io.IOException;

import javax.xml.stream.XMLStreamException;
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
		
		XmlStreamReader<SolvisDescription> reader = new XmlStreamReader<>() ;
		
		String rootId = "SolvisDescription" ;

		return reader.read(source, rootId, new SolvisDescription.Creator(rootId), xml.getName()) ;
	}

	public static void main(String[] args) throws IOException, XmlError, XMLStreamException {

		ControlFileReader reader = new ControlFileReader(null);
		reader.read();
	}

}
