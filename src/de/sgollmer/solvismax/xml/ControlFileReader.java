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
import de.sgollmer.solvismax.Constants.Debug;
import de.sgollmer.solvismax.Main;
import de.sgollmer.solvismax.error.AssignmentException;
import de.sgollmer.solvismax.error.FileException;
import de.sgollmer.solvismax.error.ReferenceException;
import de.sgollmer.solvismax.helper.FileHelper;
import de.sgollmer.solvismax.log.LogManager;
import de.sgollmer.solvismax.log.LogManager.ILogger;
import de.sgollmer.solvismax.log.LogManager.Level;
import de.sgollmer.solvismax.model.objects.SolvisDescription;
import de.sgollmer.solvismax.model.objects.control.Control;
import de.sgollmer.xmllibrary.XmlException;
import de.sgollmer.xmllibrary.XmlStreamReader;

public class ControlFileReader {

	private static final ILogger logger = LogManager.getInstance().getLogger(Control.class);
	private static final Level LEARN = Level.getLevel("LEARN");

	private static final String NAME_XML_CONTROLFILE = "control.xml";
	private static final String NAME_XSD_CONTROLFILE = "control.xsd";
	private static final String XML_ROOT_ID = "SolvisDescription";

	private final File parent;

	public ControlFileReader(final File path) {

		File writePath;

		if (path == null) {
			String pathName = System.getProperty("user.home");
			if (System.getProperty("os.name").startsWith("Windows")) {
				pathName = System.getenv("APPDATA");
			}
			writePath = new File(pathName);
		} else {
			writePath = path;
		}

		this.parent = new File(writePath, Constants.Files.RESOURCE_DESTINATION);
	}

	private void copyFiles(final boolean copyXml) throws IOException, FileException {

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
		private final boolean mustLearn;

		private Result(final SolvisDescription description, final long resourceHash, final long resultHash,
				final boolean mustLearn) {
			this.solvisDescription = description;
			this.hashes = new Hashes(resourceHash, resultHash);
			this.mustLearn = mustLearn;
		}

		public Hashes getHashes() {
			return this.hashes;
		}

		public SolvisDescription getSolvisDescription() {
			return this.solvisDescription;
		}

		public boolean mustLearn() {
			return this.mustLearn;
		}

	}

	public static class Hashes {
		private final Long resourceHash;
		private final Long fileHash;

		public Hashes(Long resourceHash, Long fileHash) {
			this.resourceHash = resourceHash;
			this.fileHash = fileHash;
		}

		public Long getResourceHash() {
			return this.resourceHash;
		}

		public Long getFileHash() {
			return this.fileHash;
		}

	}

	public Result read(final Hashes former, final boolean learn) throws IOException, XmlException, XMLStreamException,
			AssignmentException, FileException, ReferenceException {

		File xml = new File(this.parent, NAME_XML_CONTROLFILE);

		XmlStreamReader<SolvisDescription> reader = new XmlStreamReader<>();
		String rootId = XML_ROOT_ID;

		SolvisDescription fromFile = null;
		InputStream inputStreamFromFile;

		boolean mustWrite; // Wenn im Verzeichnis nicht vorhanden, nicht lesbar oder �lter
							// oder Checksumme unbekannt
		boolean modifiedByUser; // Wenn vom User modifiziert oder nicht lesbar

		boolean mustLearn;

		Throwable e = null;

		String resourcePath = Constants.Files.RESOURCE + '/' + NAME_XML_CONTROLFILE;

		InputStream resource = Main.class.getResourceAsStream(resourcePath);

		XmlStreamReader.ReadData<SolvisDescription> result = reader.read(resource, rootId,
				new SolvisDescription.Creator(rootId), NAME_XML_CONTROLFILE);

		SolvisDescription fromResource = result.getObject();
		long newResourceHash = result.getHash();

		long fileHash = 0;

		boolean xmlExits = xml.exists();
		if (xmlExits) {

			inputStreamFromFile = new FileInputStream(xml);

			boolean mustVerify = true; // Wenn zu verifizieren

			try {

				XmlStreamReader.ReadData<SolvisDescription> readData = reader.read(inputStreamFromFile, rootId,
						new SolvisDescription.Creator(rootId), xml.getName());

				fromFile = readData.getObject();
				fileHash = readData.getHash();

			} catch (Throwable e1) {
				inputStreamFromFile.close();
				e = e1;
				mustWrite = true;
			}

			if (former.getFileHash() != null) {
				mustWrite = newResourceHash != former.getResourceHash();
				modifiedByUser = fileHash != former.getResourceHash() && fileHash != newResourceHash;
				mustVerify = fileHash != former.getFileHash();
				mustLearn = fileHash != former.getFileHash() || mustWrite;
			} else {
				mustWrite = true;
				modifiedByUser = fileHash != newResourceHash;
				mustLearn = true;
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
			mustLearn = true;
		}

		boolean overwriteOnLearn = Debug.OVERWRITE_ONLY_ON_LEARN;

		if (mustWrite && (learn || !overwriteOnLearn)) {

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
			return new Result(fromResource, newResourceHash, newResourceHash, mustLearn);
		} else if (mustWrite) {
			return new Result(fromResource, newResourceHash, newResourceHash, mustLearn);
		} else if (e != null) {
			logger.error(
					"Error on reading control.xml. Learning is necessary, start parameter \"--server-learn\" must be used.");
			LogManager.exit(Constants.ExitCodes.READING_CONFIGURATION_FAIL);
			return null;
		} else {
			return new Result(fromFile, newResourceHash, fileHash, mustLearn);
		}
	}

}
