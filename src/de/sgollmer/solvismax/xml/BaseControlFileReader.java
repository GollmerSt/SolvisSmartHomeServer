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
import de.sgollmer.solvismax.error.XmlError;
import de.sgollmer.solvismax.helper.FileHelper;
import de.sgollmer.solvismax.log.LogManager;
import de.sgollmer.solvismax.log.LogManager.DelayedMessage;
import de.sgollmer.solvismax.log.LogManager.Level;

public class BaseControlFileReader {

	private static final String NAME_XML_BASEFILE = "base.xml";
	private static final String NAME_XSD_BASEFILE = "base.xsd";
	private static final String XML_ROOT_ID = "BaseData";

	private final File parent;

	public BaseControlFileReader() {
		this.parent = FileHelper.getJarDir(BaseControlFileReader.class);
	}

	public XmlStreamReader.Result<BaseData> read() throws IOException, XmlError, XMLStreamException {

		File xml = new File(this.parent, NAME_XML_BASEFILE);

		FileInputStream source = new FileInputStream(xml);

		XmlStreamReader<BaseData> reader = new XmlStreamReader<>();

		String rootId = XML_ROOT_ID;

		String resourcePath = Constants.RESOURCE_PATH + '/' + NAME_XSD_BASEFILE;
		System.err.println( "Path: " + Main.class.getCanonicalName());
		InputStream xsd = Main.class.getResourceAsStream(resourcePath);
		
		if (xsd == null ) {
			LogManager.getInstance().addDelayedErrorMessage(new DelayedMessage(Level.FATAL, "Getting of " + NAME_XSD_BASEFILE + " fails", BaseControlFileReader.class, Constants.ExitCodes.BASE_XML_ERROR));
			source.close();
			return null;
		}

		boolean verified = reader.validate(source, xsd);
		if (!verified) {
			LogManager.getInstance().addDelayedErrorMessage(new DelayedMessage(Level.FATAL, "Reading of " + NAME_XML_BASEFILE + " not successfull", BaseControlFileReader.class, Constants.ExitCodes.BASE_XML_ERROR));
			return null ;
		}

		source = new FileInputStream(xml);
		return reader.read(source, rootId, new BaseData.Creator(rootId), xml.getName());
	}

	public static void main(String[] args) throws IOException, XmlError, XMLStreamException {

		BaseControlFileReader reader = new BaseControlFileReader();
		reader.read();
	}

}
