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
import de.sgollmer.solvismax.error.LearningError;
import de.sgollmer.solvismax.error.XmlError;
import de.sgollmer.solvismax.model.objects.AllSolvisGrafics;
import de.sgollmer.solvismax.model.objects.Miscellaneous;
import de.sgollmer.solvismax.model.objects.SolvisDescription;
import de.sgollmer.solvismax.model.objects.Units.Unit;
import de.sgollmer.solvismax.model.objects.backup.MeasurementsBackupHandler;
import de.sgollmer.solvismax.xml.ControlFileReader;
import de.sgollmer.solvismax.xml.ControlFileReader.Hashes;
import de.sgollmer.solvismax.xml.GraficFileHandler;

public class Instances {
	private Collection<Solvis> units = new ArrayList<>();
	private final SolvisDescription solvisDescription;
	private final BaseData baseData;
	private final MeasurementsBackupHandler backupHandler;
	private final AllSolvisGrafics graficDatas;
	private final Hashes xmlHash;
	private final String writeablePath;

	public Instances(BaseData baseData, boolean learn) throws IOException, XmlError, XMLStreamException {
		this.baseData = baseData;
		this.writeablePath = baseData.getWritablePath();
		this.graficDatas = new GraficFileHandler(this.writeablePath).read();
		ControlFileReader reader = new ControlFileReader(this.writeablePath);
		ControlFileReader.Result result = reader.read(this.graficDatas.getControlHashCodes(), learn);
		this.solvisDescription = result.getSolvisDescription();
		this.xmlHash = result.getHashes();
		this.backupHandler = new MeasurementsBackupHandler(this.writeablePath,
				this.solvisDescription.getMiscellaneous().getMeasurementsBackupTime_ms());

		for (Unit xmlUnit : baseData.getUnits().getUnits()) {
			Solvis solvis = this.createSolvisInstance(xmlUnit);
			this.units.add(solvis);
		}
	}

	public void initialized() {
		this.backupHandler.start();
	}

	public boolean learn() throws IOException, LearningError, XMLStreamException {
		boolean nothingToLearn = true;
		for (Solvis solvis : this.units) {
			solvis.learning();
			nothingToLearn = false;
			new GraficFileHandler(this.writeablePath).write(this.graficDatas);
		}
		return !nothingToLearn;
	}

	public boolean init() throws IOException, XmlError, XMLStreamException, LearningError {
		for (Solvis solvis : this.units) {
			if (!solvis.getGrafics().isEmpty()) {
				solvis.init();
			} else {
				throw new LearningError("Learning is necessary, start parameter \"--server-learn\" must be used.");
			}
		}
		return true;
	}

	public synchronized Solvis getInstance(String solvisId) {
		for (Solvis solvis : this.units) {
			if (solvis.getUnit().getId().equals(solvisId)) {
				return solvis;
			}
		}
		return null;
	}

	private Solvis createSolvisInstance(Unit unit) throws IOException, XmlError, XMLStreamException, LearningError {
		Miscellaneous misc = this.solvisDescription.getMiscellaneous();
		SolvisConnection connection = new SolvisConnection(unit.getUrl(), unit, misc.getSolvisConnectionTimeout_ms(),
				misc.getSolvisReadTimeout_ms(), misc.getPowerOffDetectedAfterIoErrors(),
				misc.getPowerOffDetectedAfterTimeout_ms(), unit.isFwLth2_21_02A());
		String timeZone = this.baseData.getTimeZone();
		Solvis solvis = new Solvis(unit, this.solvisDescription, this.graficDatas.get(unit.getId(), this.xmlHash),
				connection, this.backupHandler, timeZone, this.baseData.getExceptionMail(),
				this.baseData.getEchoInhibitTime_ms());
		return solvis;
	}

	public SolvisDescription getSolvisDescription() {
		return this.solvisDescription;
	}

	public void abort() {
		for (Solvis unit : this.units) {
			unit.abort();
		}
		this.backupHandler.writeAndAbort();
	}

	public void backupMeasurements() throws IOException, XMLStreamException {
		this.backupHandler.write();
	}

	public Collection<Solvis> getUnits() {
		return this.units ;
	}

	public Solvis getUnit(String unitId) {
		for ( Solvis solvis : this.units ) {
			if ( unitId.equals(solvis.getUnit().getId())) {
				return solvis ;
			}
		}
		return null;
	}
}
