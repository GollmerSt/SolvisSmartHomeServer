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
import de.sgollmer.solvismax.error.XmlException;
import de.sgollmer.solvismax.helper.FileHelper;
import de.sgollmer.solvismax.log.LogManager;
import de.sgollmer.solvismax.log.LogManager.DelayedMessage;
import de.sgollmer.solvismax.log.LogManager.Level;

public class BaseControlFileReader {

	private static final String NAME_XML_BASEFILE = "base.xml";
	private static final String NAME_XSD_BASEFILE = "base.xsd";
	private static final String XML_ROOT_ID = "BaseData";

	private final File parent;
	private final File baseXml;

	public BaseControlFileReader(String baseXmlString) {
		this.parent = FileHelper.getJarDir(BaseControlFileReader.class);
		if (baseXmlString == null) {
			this.baseXml = new File(this.parent, NAME_XML_BASEFILE);
		} else {
			this.baseXml = new File(de.sgollmer.solvismax.helper.Helper.replaceEnvironments(baseXmlString));
		}
	}

	public XmlStreamReader.Result<BaseData> read()
			throws IOException, XmlException, XMLStreamException, AssignmentException, ReferenceException {

		FileInputStream source = new FileInputStream(this.baseXml);

		XmlStreamReader<BaseData> reader = new XmlStreamReader<>();

		String rootId = XML_ROOT_ID;

		String resourcePath = Constants.Pathes.RESOURCE + '/' + NAME_XSD_BASEFILE;
		InputStream xsd = Main.class.getResourceAsStream(resourcePath);

		if (xsd == null) {
			LogManager.getInstance().addDelayedErrorMessage(
					new DelayedMessage(Level.FATAL, "Getting of " + NAME_XSD_BASEFILE + " fails",
							BaseControlFileReader.class, Constants.ExitCodes.BASE_XML_ERROR));
			source.close();
			return null;
		}

		boolean verified = reader.validate(source, xsd);
		if (!verified) {
			LogManager.getInstance().addDelayedErrorMessage(
					new DelayedMessage(Level.FATAL, "Reading of " + NAME_XML_BASEFILE + " not successfull",
							BaseControlFileReader.class, Constants.ExitCodes.BASE_XML_ERROR));
			return null;
		}

		source = new FileInputStream(this.baseXml);
		return reader.read(source, rootId, new BaseData.Creator(rootId), this.baseXml.getName());
	}

	public static void main(String[] args)
			throws IOException, XmlException, XMLStreamException, AssignmentException, ReferenceException {

		BaseControlFileReader reader = new BaseControlFileReader(null);
		reader.read();
	}

}
