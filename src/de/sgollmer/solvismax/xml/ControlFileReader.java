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
import de.sgollmer.solvismax.log.Logger2;
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

		if (!this.parent.exists()) {
			success = this.parent.mkdir();
		}

		if (!success) {
			throw new FileError("Error on creating directory <" + this.parent.getAbsolutePath() + ">");
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
			return this.hashes;
		}

		public SolvisDescription getSolvisDescription() {
			return this.solvisDescription;
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
			return this.resourceHash;
		}

		public int getFileHash() {
			return this.fileHash;
		}

	}

	public Result read(Integer formerResourceHash, boolean learn) throws IOException, XmlError, XMLStreamException {

		File xml = new File(this.parent, NAME_XML_CONTROLFILE);

		XmlStreamReader<SolvisDescription> reader = new XmlStreamReader<>();
		String rootId = XML_ROOT_ID;

		XmlStreamReader.Result<SolvisDescription> xmlFromFile = null;

		boolean mustWrite = false;
		boolean createBackup = false;
		boolean xmlExits = false;

		Throwable e = null;

		if (xml.exists()) {

			xmlExits = true;
			try {

				xmlFromFile = reader.read(new FileInputStream(xml), rootId, new SolvisDescription.Creator(rootId),
						xml.getName());
			} catch (Throwable e1) {
				e = e1;
				createBackup = true;
			}
		} else {
			mustWrite = true;
		}

		String resourcePath = Constants.RESOURCE_PATH + File.separator + NAME_XML_CONTROLFILE;
		InputStream source = Main.class.getResourceAsStream(resourcePath);
		XmlStreamReader.Result<SolvisDescription> xmlFromResource = reader.read(source, rootId,
				new SolvisDescription.Creator(rootId), NAME_XML_CONTROLFILE);

		int newResourceHash = xmlFromResource.getHash();

		mustWrite |= formerResourceHash == null || newResourceHash != formerResourceHash
				|| !xmlExits ;
		// wenn nicht vorhanden oder Hashes unterscheiden sich

		createBackup |= xmlFromFile != null
				&& (formerResourceHash == null || xmlFromFile.getHash() != formerResourceHash); //
		// wenn keine graphics.xml oder unterschiedlich zur alten

		if (mustWrite && learn || !xmlExits ) {

			if (createBackup) {

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
		} else if (mustWrite) {
			return new Result(xmlFromResource, newResourceHash);
		} else if (e != null) {
			Logger2.out(logger, Level.ERROR, "Error on reading control.xml: ", e.getStackTrace());
			System.exit(Constants.ExitCodes.READING_CONFIGURATION_FAIL);
			return null;
		} else {
			return new Result(xmlFromFile, newResourceHash);
		}
	}

}
