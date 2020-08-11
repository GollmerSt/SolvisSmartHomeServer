/************************************************************************
 * 
 * $Id$
 *
 * 
 ************************************************************************/

package de.sgollmer.solvismax.model.objects.backup;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import javax.xml.stream.XMLOutputFactory;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamWriter;

import de.sgollmer.solvismax.Constants;
import de.sgollmer.solvismax.error.AssignmentException;
import de.sgollmer.solvismax.error.FileException;
import de.sgollmer.solvismax.error.ReferenceException;
import de.sgollmer.solvismax.error.TerminationException;
import de.sgollmer.solvismax.error.XmlException;
import de.sgollmer.solvismax.helper.AbortHelper;
import de.sgollmer.solvismax.helper.FileHelper;
import de.sgollmer.solvismax.log.LogManager;
import de.sgollmer.solvismax.log.LogManager.ILogger;
import de.sgollmer.solvismax.model.Solvis;
import de.sgollmer.solvismax.xml.XmlStreamReader;

public class MeasurementsBackupHandler {

	private static final ILogger logger = LogManager.getInstance().getLogger(MeasurementsBackupHandler.class);

	private static final String NAME_XSD_MEASUREMENTS_FILE = "measurements.xsd";
	private static final String NAME_XML_MEASUREMENTS_FILE = "measurements.xml";

	private static final String XML_MEASUREMENTS = "SolvisMeasurements";

	private final File parent;
	private final Measurements measurements = new Measurements();
	private final BackupThread thread;
	private boolean xsdWritten = false;
	private long timeOfLastBackup = -1 ;

	public MeasurementsBackupHandler(File path, int measurementsBackupTime_ms)
			throws FileException, ReferenceException {

		if (path == null) {
			String pathName = System.getProperty("user.home");
			if (System.getProperty("os.name").startsWith("Windows")) {
				pathName = System.getenv("APPDATA");
			}
			path = new File(pathName);
		}

		this.parent = new File(path, Constants.Files.RESOURCE_DESTINATION);
		this.thread = new BackupThread(this, measurementsBackupTime_ms);

		try {
			this.read();
		} catch (IOException | XmlException | XMLStreamException | AssignmentException e) {
			logger.warn("Error on reading the BackupFile detected", e);
		}
	}

	private void copyFiles() throws IOException, FileException {

		boolean success = true;

		if (!this.parent.exists()) {
			success = FileHelper.mkdir(this.parent);
		}

		if (!success) {
			throw new FileException("Error on creating directory <" + this.parent.getAbsolutePath() + ">");
		}

		if (!this.xsdWritten) {

			File xsd = new File(this.parent, NAME_XSD_MEASUREMENTS_FILE);

			FileHelper.copyFromResource(Constants.Files.RESOURCE + '/' + NAME_XSD_MEASUREMENTS_FILE, xsd);

			this.xsdWritten = true;
		}
	}

	public void read() throws IOException, XmlException, XMLStreamException, AssignmentException, FileException,
			ReferenceException {

		this.copyFiles();

		File xml = new File(this.parent, NAME_XML_MEASUREMENTS_FILE);

		if (!xml.exists()) {
			return;
		}
		
		this.timeOfLastBackup =xml.lastModified() ; 

		InputStream source = new FileInputStream(xml);

		XmlStreamReader<Measurements> reader = new XmlStreamReader<>();

		String rootId = XML_MEASUREMENTS;

		reader.read(source, rootId, new Measurements.Creator(this.measurements, rootId), xml.getName());
	}

	private void update() throws IOException, XMLStreamException {
		for (SystemMeasurements system : this.measurements.getSystemMeasurements()) {
			Solvis owner = system.getOwner();
			if (owner != null) {
				owner.backupMeasurements(system);
			}
		}

	}

	public void write() throws IOException, XMLStreamException, FileException {

		this.update();

		this.copyFiles();

		File output = new File(this.parent, NAME_XML_MEASUREMENTS_FILE);

		XMLOutputFactory factory = XMLOutputFactory.newInstance();
		OutputStream outputStream = new FileOutputStream(output);
		XMLStreamWriter writer = factory.createXMLStreamWriter(outputStream);
		writer.writeStartDocument();
		writer.writeStartElement(XML_MEASUREMENTS);
		this.measurements.writeXml(writer);
		writer.writeEndElement();
		writer.writeEndDocument();
		writer.flush();
		writer.close();
		outputStream.close();

		logger.info("Backup of measurements written.");
	}

	public void register(Solvis owner, String id) {
		SystemMeasurements system = this.measurements.get(id);
		system.setOwner(owner);
	}

	private static class BackupThread extends Thread {

		private final int measurementsBackupTime_ms;
		private final MeasurementsBackupHandler handler;
		private boolean abort = false;

		public BackupThread(MeasurementsBackupHandler handler, int measurementsBackupTime_ms) {
			super("BackupThread");

			this.measurementsBackupTime_ms = measurementsBackupTime_ms;
			this.handler = handler;
		}

		@Override
		public void run() {
			while (!this.abort) {
				try {
					synchronized (this) {
						try {
							this.wait(this.measurementsBackupTime_ms);
						} catch (InterruptedException e) {
						}
						if (!this.abort) {
							try {
								this.handler.write();
							} catch (IOException | XMLStreamException e) {
							}
						}
					}
				} catch (Throwable e) {
					logger.error("Error was thrown in backup thread. Cause: ", e);
					try {
						AbortHelper.getInstance().sleep(Constants.WAIT_TIME_AFTER_THROWABLE);
					} catch (TerminationException e1) {
						return;
					}
				}

			}
		}

		public void writeAndAbort() {
			synchronized (this) {
				this.abort = true;
				this.notifyAll();
			}
			try {
				this.handler.write();
			} catch (IOException | XMLStreamException | FileException e) {
			}
		}
	}

	public void start() {
		this.thread.start();
	}

	public void writeAndAbort() {
		this.thread.writeAndAbort();

	}

	public SystemMeasurements getSystemMeasurements(String id) {
		return this.measurements.get(id);
	}
	
	public long getTimeOfLastBackup() {
		return this.timeOfLastBackup;
	}

}
