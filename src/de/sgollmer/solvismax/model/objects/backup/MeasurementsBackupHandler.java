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
import de.sgollmer.solvismax.error.FileError;
import de.sgollmer.solvismax.error.XmlError;
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

	public MeasurementsBackupHandler(String pathName, int measurementsBackupTime_ms) {
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
		this.thread = new BackupThread(this, measurementsBackupTime_ms);
		try {
			this.read();
		} catch (IOException | XmlError | XMLStreamException e) {
			logger.warn("Error on reading the BackupFile detected", e);
		}
	}

	private void copyFiles() throws IOException {

		boolean success = true;

		if (!this.parent.exists()) {
			success = FileHelper.mkdir(this.parent);
		}

		if (!success) {
			throw new FileError("Error on creating directory <" + this.parent.getAbsolutePath() + ">");
		}

		if (!this.xsdWritten) {

			File xsd = new File(this.parent, NAME_XSD_MEASUREMENTS_FILE);

			FileHelper.copyFromResource(Constants.RESOURCE_PATH + '/' + NAME_XSD_MEASUREMENTS_FILE, xsd);

			this.xsdWritten = true;
		}
	}

	public void read() throws IOException, XmlError, XMLStreamException {

		this.copyFiles();

		File xml = new File(this.parent, NAME_XML_MEASUREMENTS_FILE);

		if (!xml.exists()) {
			return;
		}

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

	public void write() throws IOException, XMLStreamException {

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
					AbortHelper.getInstance().sleep(Constants.WAIT_TIME_AFTER_THROWABLE);
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
			} catch (IOException | XMLStreamException e) {
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

}
