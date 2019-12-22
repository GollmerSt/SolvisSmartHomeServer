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

import org.slf4j.LoggerFactory;

import de.sgollmer.solvismax.Constants;
import de.sgollmer.solvismax.error.FileError;
import de.sgollmer.solvismax.error.XmlError;
import de.sgollmer.solvismax.helper.FileHelper;
import de.sgollmer.solvismax.model.Solvis;
import de.sgollmer.solvismax.xml.XmlStreamReader;

public class MeasurementsBackupHandler {

	private static final org.slf4j.Logger logger = LoggerFactory.getLogger(MeasurementsBackupHandler.class);
	
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
			String writeDirectory = System.getProperty("user.home");
			if (System.getProperty("os.name").startsWith("Windows")) {
				writeDirectory = System.getenv("APPDATA");
			}

			writeDirectory += File.separator + "SolvisMaxJava";

			parent = new File(writeDirectory);
		} else {
			parent = new File(pathName);
		}
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

		if (!parent.exists()) {
			success = parent.mkdir();
		}

		if (!success) {
			throw new FileError("Error on creating directory <" + parent.getAbsolutePath() + ">");
		}

		if (!xsdWritten) {

			File xsd = new File(this.parent, NAME_XSD_MEASUREMENTS_FILE);

			FileHelper.copyFromResource(Constants.RESOURCE_PATH + File.separator + NAME_XSD_MEASUREMENTS_FILE, xsd);

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

		reader.read(source, rootId, new Measurements.Creator(measurements, rootId), xml.getName());
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

		File output = new File(parent, NAME_XML_MEASUREMENTS_FILE);

		XMLOutputFactory factory = XMLOutputFactory.newInstance();
		OutputStream outputStream = new FileOutputStream(output) ;
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
		private boolean terminate = false;

		public BackupThread(MeasurementsBackupHandler handler, int measurementsBackupTime_ms) {
			super("BackupThread");

			this.measurementsBackupTime_ms = measurementsBackupTime_ms;
			this.handler = handler;
		}

		@Override
		public void run() {
			while (!terminate) {
				synchronized (this) {
					try {
						this.wait(this.measurementsBackupTime_ms);
					} catch (InterruptedException e) {
					}
					try {
						handler.write();
					} catch (IOException | XMLStreamException e) {
					}
				}
			}
		}

		public synchronized void terminate() {
			this.terminate = true;
			this.notifyAll();
		}
	}

	public void start() {
		this.thread.start();
	}

	public void terminate() {
		this.thread.terminate();
	}

	public SystemMeasurements getSystemMeasurements(String id) {
		return this.measurements.get(id);
	}

}