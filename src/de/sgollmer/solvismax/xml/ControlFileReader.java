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

import de.sgollmer.solvismax.Constants;
import de.sgollmer.solvismax.Main;
import de.sgollmer.solvismax.error.AssignmentException;
import de.sgollmer.solvismax.error.FileException;
import de.sgollmer.solvismax.error.ReferenceException;
import de.sgollmer.solvismax.error.XmlException;
import de.sgollmer.solvismax.helper.FileHelper;
import de.sgollmer.solvismax.log.LogManager;
import de.sgollmer.solvismax.log.LogManager.Level;
import de.sgollmer.solvismax.log.LogManager.ILogger;
import de.sgollmer.solvismax.model.objects.SolvisDescription;
import de.sgollmer.solvismax.model.objects.control.Control;

public class ControlFileReader {

	private static final ILogger logger = LogManager.getInstance().getLogger(Control.class);
	private static final Level LEARN = Level.getLevel("LEARN");

	private static final String NAME_XML_CONTROLFILE = "control.xml";
	private static final String NAME_XSD_CONTROLFILE = "control.xsd";
	private static final String XML_ROOT_ID = "SolvisDescription";

	private final File parent;

	public ControlFileReader(File path) {

		if (path == null) {
			String pathName = System.getProperty("user.home");
			if (System.getProperty("os.name").startsWith("Windows")) {
				pathName = System.getenv("APPDATA");
			}
			path = new File(pathName);
		}

		this.parent = new File(path, Constants.Pathes.RESOURCE_DESTINATION);
	}

	private void copyFiles(boolean copyXml) throws IOException, FileException {

		boolean success = true;

		if (!this.parent.exists()) {
			success = FileHelper.mkdir(this.parent);
		}

		if (!success) {
			throw new FileException("Error on creating directory <" + this.parent.getAbsolutePath() + ">");
		}

		File xml = new File(this.parent, NAME_XML_CONTROLFILE);

		if (copyXml) {
			FileHelper.copyFromResource(Constants.Pathes.RESOURCE + '/' + NAME_XML_CONTROLFILE, xml);
		}

		File xsd = new File(this.parent, NAME_XSD_CONTROLFILE);

		FileHelper.copyFromResource(Constants.Pathes.RESOURCE + '/' + NAME_XSD_CONTROLFILE, xsd);

	}

	public static class Result {

		private final SolvisDescription solvisDescription;
		private final Hashes hashes;

		private Result(XmlStreamReader.Result<SolvisDescription> result, int resourceHash) {
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
		private final Integer resourceHash;
		private final Integer fileHash;

		public Hashes(Integer resourceHash, Integer fileHash) {
			this.resourceHash = resourceHash;
			this.fileHash = fileHash;
		}

		public Integer getResourceHash() {
			return this.resourceHash;
		}

		public Integer getFileHash() {
			return this.fileHash;
		}

	}

	public Result read(Hashes former, boolean learn) throws IOException, XmlException, XMLStreamException,
			AssignmentException, FileException, ReferenceException {

		File xml = new File(this.parent, NAME_XML_CONTROLFILE);

		XmlStreamReader<SolvisDescription> reader = new XmlStreamReader<>();
		String rootId = XML_ROOT_ID;

		XmlStreamReader.Result<SolvisDescription> xmlFromFile = null;

		boolean mustWrite = false; // Wenn im Verzeichnis nicht vorhanden, nicht lesbar oder �lter
									// oder Checksumme unbekannt
		boolean modifiedByUser = false; // Wenn vom User modifiziert oder nicht lesbar

		Throwable e = null;

		boolean xmlExits = xml.exists();
		if (xmlExits) {

			boolean mustVerify = false; // Wenn zu verifizieren

			try {

				xmlFromFile = reader.read(new FileInputStream(xml), rootId, new SolvisDescription.Creator(rootId),
						xml.getName());
				mustVerify = xmlFromFile.getHash() != former.getFileHash();
				modifiedByUser = former.getFileHash() == null || former.getResourceHash() != xmlFromFile.getHash();
			} catch (Throwable e1) {
				e = e1;
				modifiedByUser = true;
				mustVerify = true;
				mustWrite = true;
			}
			if (mustVerify) {
				String xsdPath = Constants.Pathes.RESOURCE + '/' + NAME_XSD_CONTROLFILE;
				InputStream xsd = Main.class.getResourceAsStream(xsdPath);
				boolean validated = reader.validate(new FileInputStream(xml), xsd);
				if (!validated) {
					if (!mustWrite) {
						LogManager.exit(Constants.ExitCodes.READING_CONFIGURATION_FAIL);
					} else {
						logger.warn("Not valid control.xml will be overwriten by a newer version");
					}
				}
			}

		} else {
			mustWrite = true;
		}

		String resourcePath = Constants.Pathes.RESOURCE + '/' + NAME_XML_CONTROLFILE;
		InputStream source = Main.class.getResourceAsStream(resourcePath);
		XmlStreamReader.Result<SolvisDescription> xmlFromResource = reader.read(source, rootId,
				new SolvisDescription.Creator(rootId), NAME_XML_CONTROLFILE);

		int newResourceHash = xmlFromResource.getHash();

		mustWrite |= former.getResourceHash() == null || newResourceHash != former.getResourceHash();
		// wenn alter Resource-Hash nicht vorhanden oder Resource-Hashs unterscheiden
		// sich

		if (mustWrite && learn || !xmlExits) {

			if (modifiedByUser) {

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
			logger.error(
					"Error on reading control.xml. Learning is necessary, start parameter \"--server-learn\" must be used.");
			LogManager.exit(Constants.ExitCodes.READING_CONFIGURATION_FAIL);
			return null;
		} else {
			return new Result(xmlFromFile, newResourceHash);
		}
	}

}
