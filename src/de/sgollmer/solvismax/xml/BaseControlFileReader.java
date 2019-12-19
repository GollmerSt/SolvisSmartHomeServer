package de.sgollmer.solvismax.xml;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;

import javax.xml.stream.XMLStreamException;

import de.sgollmer.solvismax.Constants;
import de.sgollmer.solvismax.error.FileError;
import de.sgollmer.solvismax.error.XmlError;
import de.sgollmer.solvismax.helper.FileHelper;
import de.sgollmer.solvismax.model.BaseData;

public class BaseControlFileReader {

	private static final String NAME_XML_BASEFILE = "base.xml";
	private static final String NAME_XSD_BASEFILE = "base.xsd";
	private static final String XML_ROOT_ID = "BaseData";

	private final File parent;

	public BaseControlFileReader(String pathName) {
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

		boolean success = true;

		if (!parent.exists()) {
			success = parent.mkdir();
		}

		if (!success) {
			throw new FileError("Error on creating directory <" + parent.getAbsolutePath() + ">");
		}

		File xml = new File(this.parent, NAME_XML_BASEFILE);

		if (!xml.exists() || Constants.DEBUG) {
			FileHelper.copyFromResource(Constants.RESOURCE_PATH + File.separator + NAME_XML_BASEFILE, xml);
		}

		File xsd = new File(this.parent, NAME_XSD_BASEFILE);

		FileHelper.copyFromResource(Constants.RESOURCE_PATH + File.separator + NAME_XSD_BASEFILE, xsd);

	}

	public XmlStreamReader.Result<BaseData> read() throws IOException, XmlError, XMLStreamException {

		this.copyFiles();

		File xml = new File(this.parent, NAME_XML_BASEFILE);

		InputStream source = new FileInputStream(xml);

		XmlStreamReader<BaseData> reader = new XmlStreamReader<>();

		String rootId = XML_ROOT_ID;

		return reader.read(source, rootId, new BaseData.Creator(rootId), xml.getName());
	}

	public static void main(String[] args) throws IOException, XmlError, XMLStreamException {

		BaseControlFileReader reader = new BaseControlFileReader(null);
		reader.read();
	}

}
