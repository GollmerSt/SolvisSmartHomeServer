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

import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import de.sgollmer.solvismax.Constants;
import de.sgollmer.solvismax.Main;
import de.sgollmer.solvismax.error.FileError;
import de.sgollmer.solvismax.error.XmlError;
import de.sgollmer.solvismax.helper.FileHelper;
import de.sgollmer.solvismax.model.objects.SolvisDescription;
import de.sgollmer.solvismax.model.objects.control.Control;

public class ControlFileReader {

	private static final Logger logger = LogManager.getLogger(Control.class);
	private static final Level LEARN = Level.getLevel("LEARN");

	private static final String NAME_XML_CONTROLFILE = "control.xml";
	private static final String NAME_XSD_CONTROLFILE = "control.xsd";
	private static final String XML_ROOT_ID = "SolvisDescription";

	private final File parent;

	public ControlFileReader(String pathName) {
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

	private void copyFiles(boolean copyXml) throws IOException {

		boolean success = true;

		if (!parent.exists()) {
			success = parent.mkdir();
		}

		if (!success) {
			throw new FileError("Error on creating directory <" + parent.getAbsolutePath() + ">");
		}

		File xml = new File(this.parent, NAME_XML_CONTROLFILE);

		if (copyXml) {
			FileHelper.copyFromResource(Constants.RESOURCE_PATH + File.separator + NAME_XML_CONTROLFILE, xml);
		}

		File xsd = new File(this.parent, NAME_XSD_CONTROLFILE);

		FileHelper.copyFromResource(Constants.RESOURCE_PATH + File.separator + NAME_XSD_CONTROLFILE, xsd);

	}

	public static class Result {

		private final SolvisDescription solvisDescription;
		private final Hashes hashes;

		public Result(XmlStreamReader.Result<SolvisDescription> result, int resourceHash) {
			this.solvisDescription = result.getTree();
			this.hashes = new Hashes(resourceHash, result.getHash());
		}

		public Hashes getHashes() {
			return hashes;
		}

		public SolvisDescription getSolvisDescription() {
			return solvisDescription;
		}

	}

	public static class Hashes {
		private final int resourceHash;
		private final int fileHash;

		public Hashes(int resourceHash, int fileHash) {
			this.resourceHash = resourceHash;
			this.fileHash = fileHash;
		}

		public int getResourceHash() {
			return resourceHash;
		}

		public int getFileHash() {
			return fileHash;
		}

	}

	public Result read(Integer formerResourceHash, boolean learn) throws IOException, XmlError, XMLStreamException {

		File xml = new File(this.parent, NAME_XML_CONTROLFILE);

		XmlStreamReader<SolvisDescription> reader = new XmlStreamReader<>();
		String rootId = XML_ROOT_ID;

		XmlStreamReader.Result<SolvisDescription> xmlFromFile = null;

		if (xml.exists()) {

			xmlFromFile = reader.read(new FileInputStream(xml), rootId, new SolvisDescription.Creator(rootId),
					xml.getName());
		}

		String resourcePath = Constants.RESOURCE_PATH + File.separator + NAME_XML_CONTROLFILE;
		InputStream source = Main.class.getResourceAsStream(resourcePath);
		XmlStreamReader.Result<SolvisDescription> xmlFromResource = reader.read(source, rootId,
				new SolvisDescription.Creator(rootId), NAME_XML_CONTROLFILE);

		int newResourceHash = xmlFromResource.getHash();

		boolean isNewResource = formerResourceHash == null || newResourceHash != formerResourceHash;

		if (isNewResource && learn || xmlFromFile == null) {

			if (xmlFromFile != null && formerResourceHash != null && xmlFromFile.getHash() != formerResourceHash) {

				FileHelper.renameDuplicates(xml, Constants.NUMBER_OF_CONTROL_FILE_DUPLICATES);

				logger.log(LEARN, "***********************************************************************");
				logger.log(LEARN, "                     A T T E N T I O N");
				logger.log(LEARN, "");
				logger.log(LEARN, "The file <control.xml> was manually changed. It's renamed to");
				logger.log(LEARN, "<control.xml.1>.The new one of the new server version is used!");
				logger.log(LEARN, "***********************************************************************");
			}

			this.copyFiles(true);
			return new Result(xmlFromResource, newResourceHash);
		} else if (isNewResource) {
			return new Result(xmlFromResource, newResourceHash);
		} else {
			if (learn) {
				this.copyFiles(false);
			}
			return new Result(xmlFromFile, newResourceHash);
		}

	}

}
