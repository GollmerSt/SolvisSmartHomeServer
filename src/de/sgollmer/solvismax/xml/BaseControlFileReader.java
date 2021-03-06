/************************************************************************
 * 
 * $Id$
 *
 * 
 ************************************************************************/

package de.sgollmer.solvismax.xml;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;

import javax.xml.stream.XMLStreamException;

import de.sgollmer.solvismax.BaseData;
import de.sgollmer.solvismax.Constants;
import de.sgollmer.solvismax.Main;
import de.sgollmer.solvismax.error.AssignmentException;
import de.sgollmer.solvismax.error.ReferenceException;
import de.sgollmer.solvismax.helper.FileHelper;
import de.sgollmer.solvismax.log.LogManager;
import de.sgollmer.solvismax.log.LogManager.ILogger;
import de.sgollmer.solvismax.log.LogManager.Level;
import de.sgollmer.xmllibrary.XmlException;
import de.sgollmer.xmllibrary.XmlStreamReader;

public class BaseControlFileReader {

	private static final ILogger logger = LogManager.getInstance().getLogger(BaseControlFileReader.class);

	private static final String NAME_XML_BASEFILE = "base.xml";
	private static final String NAME_XSD_BASEFILE = "base.xsd";
	private static final String XML_ROOT_ID = "BaseData";

	private final File parent;
	private final File baseXml;

	public BaseControlFileReader(final String baseXmlString) {
		this.parent = FileHelper.getJarDir(BaseControlFileReader.class);
		if (baseXmlString == null) {
			this.baseXml = new File(this.parent, NAME_XML_BASEFILE);
		} else {
			this.baseXml = new File(de.sgollmer.solvismax.helper.Helper.replaceEnvironments(baseXmlString));
		}
	}

	public BaseData read()
			throws IOException, XMLStreamException, AssignmentException, ReferenceException, XmlException {

		FileInputStream source = new FileInputStream(this.baseXml);

		XmlStreamReader<BaseData> reader = new XmlStreamReader<>();

		String rootId = XML_ROOT_ID;

		String resourcePath = Constants.Files.RESOURCE + '/' + NAME_XSD_BASEFILE;
		InputStream xsd = Main.class.getResourceAsStream(resourcePath);

		if (xsd == null) {
			logger.log(Level.FATAL, "Getting of " + NAME_XSD_BASEFILE + " fails", null,
					Constants.ExitCodes.BASE_XML_ERROR);
			source.close();
			return null;
		}

		boolean verified = reader.validate(source, xsd);
		if (!verified) {
			logger.log(Level.FATAL, "Reading of " + NAME_XML_BASEFILE + " not successfull", null,
					Constants.ExitCodes.BASE_XML_ERROR);
			return null;
		}

		source = new FileInputStream(this.baseXml);
		return reader.read(source, rootId, new BaseData.Creator(rootId), this.baseXml.getName()).getObject();
	}

	public static void main(final String[] args)
			throws IOException, XmlException, XMLStreamException, AssignmentException, ReferenceException {

		BaseControlFileReader reader = new BaseControlFileReader(null);
		reader.read();
	}

}
