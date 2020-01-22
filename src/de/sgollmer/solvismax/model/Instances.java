/************************************************************************
 * 
 * $Id$
 *
 * 
 ************************************************************************/

package de.sgollmer.solvismax.model;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;

import javax.xml.stream.XMLStreamException;

import de.sgollmer.solvismax.BaseData;
import de.sgollmer.solvismax.connection.SolvisConnection;
import de.sgollmer.solvismax.connection.transfer.ConnectPackage;
import de.sgollmer.solvismax.error.LearningError;
import de.sgollmer.solvismax.error.XmlError;
import de.sgollmer.solvismax.model.objects.AllSolvisGrafics;
import de.sgollmer.solvismax.model.objects.Miscellaneous;
import de.sgollmer.solvismax.model.objects.SolvisDescription;
import de.sgollmer.solvismax.model.objects.Units.Unit;
import de.sgollmer.solvismax.model.objects.backup.MeasurementsBackupHandler;
import de.sgollmer.solvismax.xml.ControlFileReader;
import de.sgollmer.solvismax.xml.GraficFileHandler;
import de.sgollmer.solvismax.xml.XmlStreamReader.Result;

public class Instances {
	private Collection<Solvis> units = new ArrayList<>();
	private final SolvisDescription solvisDescription;
	private final BaseData baseData;
	private final MeasurementsBackupHandler backupHandler;
	private final AllSolvisGrafics graficDatas;
	private final int xmlHash;
	private final String writeablePath;

	public Instances(BaseData baseData) throws IOException, XmlError, XMLStreamException {
		this.baseData = baseData;
		this.writeablePath = baseData.getWritablePath();
		ControlFileReader reader = new ControlFileReader(this.writeablePath);
		Result<SolvisDescription> result = reader.read();
		this.solvisDescription = result.getTree();
		this.xmlHash = result.getHash();
		this.backupHandler = new MeasurementsBackupHandler(this.writeablePath,
				solvisDescription.getMiscellaneous().getMeasurementsBackupTime_ms());
		this.graficDatas = new GraficFileHandler(this.writeablePath, this.xmlHash).read();

		for (Unit xmlUnit : baseData.getUnits().getUnits()) {
			Solvis solvis = this.createSolvisInstance(xmlUnit);
			this.units.add(solvis);
		}
		this.backupHandler.start();
	}

	public boolean learn() throws IOException, LearningError, XMLStreamException {
		boolean nothingToLearn = true;
		for (Solvis solvis : this.units) {
			if (solvis.getGrafics().isEmpty()) {
				solvis.learning();
				nothingToLearn = false;
				new GraficFileHandler(this.writeablePath, this.xmlHash).write(this.graficDatas);
			}
		}
		return !nothingToLearn;
	}

	public boolean init() throws IOException, XmlError, XMLStreamException, LearningError {
		for (Solvis solvis : this.units) {
			if (!solvis.getGrafics().isEmpty()) {
				solvis.init();
			} else {
				throw new LearningError("Learning is necessary");
			}
		}
		return true;
	}

	public synchronized Solvis getInstance(ConnectPackage connectPackage)
			throws IOException, XmlError, XMLStreamException, LearningError {
		for (Solvis solvis : units) {
			if (solvis.getUnit().getId().equals(connectPackage.getId())) {
				return solvis;
			}
		}
		return null;
	}

	private Solvis createSolvisInstance(Unit unit) throws IOException, XmlError, XMLStreamException, LearningError {
		Miscellaneous misc = solvisDescription.getMiscellaneous();
		SolvisConnection connection = new SolvisConnection(unit.getUrl(), unit, misc.getSolvisConnectionTimeout_ms(),
				misc.getSolvisReadTimeout_ms(), misc.getPowerOffDetectedAfterIoErrors(),
				misc.getPowerOffDetectedAfterTimeout_ms());
		String timeZone = this.baseData.getTimeZone();
		Solvis solvis = new Solvis(unit, this.solvisDescription, this.graficDatas.get(unit.getId()), connection,
				this.backupHandler, timeZone);
		return solvis;
	}

	public SolvisDescription getSolvisDescription() {
		return solvisDescription;
	}

	public void abort() {
		for (Solvis unit : units) {
			unit.abort();
		}
		this.backupHandler.writeAndAbort();
	}

	public void backupMeasurements() throws IOException, XMLStreamException {
		this.backupHandler.write();
	}

}
