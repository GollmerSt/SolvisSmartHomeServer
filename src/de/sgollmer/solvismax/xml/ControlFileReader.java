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
import de.sgollmer.solvismax.helper.FileHelper.ChecksumInputStream;
import de.sgollmer.solvismax.log.LogManager;
import de.sgollmer.solvismax.log.LogManager.Level;
import de.sgollmer.solvismax.log.LogManager.ILogger;
import de.sgollmer.solvismax.model.objects.SolvisDescription;
import de.sgollmer.solvismax.model.objects.control.Control;

public class ControlFileReader {
	
	private static final boolean OVERWWRITE_ONLY_ON_LEARN = true;

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

		this.parent = new File(path, Constants.Files.RESOURCE_DESTINATION);
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
			FileHelper.copyFromResourceBinary(Constants.Files.RESOURCE + '/' + NAME_XML_CONTROLFILE, xml);
		}

		File xsd = new File(this.parent, NAME_XSD_CONTROLFILE);

		FileHelper.copyFromResourceText(Constants.Files.RESOURCE + '/' + NAME_XSD_CONTROLFILE, xsd);

	}

	public static class Result {

		private final SolvisDescription solvisDescription;
		private final Hashes hashes;

		private Result(SolvisDescription description, int resourceHash, int resultHash) {
			this.solvisDescription = description;
			this.hashes = new Hashes(resourceHash, resultHash);
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

		SolvisDescription fromFile = null;
		ChecksumInputStream inputStreamFromFile = new ChecksumInputStream(new FileInputStream(xml));

		boolean mustWrite = false; // Wenn im Verzeichnis nicht vorhanden, nicht lesbar oder älter
									// oder Checksumme unbekannt
		boolean modifiedByUser = false; // Wenn vom User modifiziert oder nicht lesbar

		Throwable e = null;

		String resourcePath = Constants.Files.RESOURCE + '/' + NAME_XML_CONTROLFILE;
		ChecksumInputStream resource = new ChecksumInputStream(Main.class.getResourceAsStream(resourcePath));
		SolvisDescription fromResource = reader.read(resource, rootId,
				new SolvisDescription.Creator(rootId), NAME_XML_CONTROLFILE);

		int newResourceHash = resource.getHash();
		int fileHash = 0;

		boolean xmlExits = xml.exists();
		if (xmlExits) {

			boolean mustVerify = true; // Wenn zu verifizieren

			try {

				fromFile = reader.read(inputStreamFromFile, rootId, new SolvisDescription.Creator(rootId),
						xml.getName());

			} catch (Throwable e1) {
				inputStreamFromFile.close();
				e = e1;
				mustWrite = true;
			}
			
			fileHash = inputStreamFromFile.getHash();
			
			if ( former.getFileHash() != null ) {
				mustWrite =  newResourceHash != former.getResourceHash() ;
				modifiedByUser = fileHash != former.getResourceHash();
				mustVerify = fileHash != former.getFileHash();
			} else {
				mustWrite = true;
				modifiedByUser = newResourceHash != inputStreamFromFile.getHash();
				mustVerify = true;
			}
			
			
			if (mustVerify) {
				String xsdPath = Constants.Files.RESOURCE + '/' + NAME_XSD_CONTROLFILE;
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
			modifiedByUser = false;
		}

		if (mustWrite && (learn || !OVERWWRITE_ONLY_ON_LEARN)) {

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
			return new Result(fromResource, newResourceHash, newResourceHash);
		} else if (mustWrite) {
			return new Result(fromResource, newResourceHash,newResourceHash);
		} else if (e != null) {
			logger.error(
					"Error on reading control.xml. Learning is necessary, start parameter \"--server-learn\" must be used.");
			LogManager.exit(Constants.ExitCodes.READING_CONFIGURATION_FAIL);
			return null;
		} else {
			return new Result(fromFile, newResourceHash, fileHash);
		}
	}

}
