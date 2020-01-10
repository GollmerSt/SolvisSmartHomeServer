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
import de.sgollmer.solvismax.error.XmlError;
import de.sgollmer.solvismax.helper.FileHelper;

public class BaseControlFileReader {

	private static final String NAME_XML_BASEFILE = "base.xml";
	private static final String XML_ROOT_ID = "BaseData";

	private final File parent ;

	public BaseControlFileReader() {
		parent = FileHelper.getJarDir(BaseControlFileReader.class);
	}

	public XmlStreamReader.Result<BaseData> read() throws IOException, XmlError, XMLStreamException {

		File xml = new File(this.parent, NAME_XML_BASEFILE);

		InputStream source = new FileInputStream(xml);

		XmlStreamReader<BaseData> reader = new XmlStreamReader<>();

		String rootId = XML_ROOT_ID;

		return reader.read(source, rootId, new BaseData.Creator(rootId), xml.getName());
	}

	public static void main(String[] args) throws IOException, XmlError, XMLStreamException {

		BaseControlFileReader reader = new BaseControlFileReader();
		reader.read();
	}

}
