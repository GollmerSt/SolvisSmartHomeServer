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
import de.sgollmer.solvismax.model.objects.SystemGrafics;
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
		ControlFileReader.Result result = reader.read(graficDatas.getControlResourceHashCode(), learn);
		this.solvisDescription = result.getSolvisDescription();
		this.xmlHash = result.getHashes();
		this.backupHandler = new MeasurementsBackupHandler(this.writeablePath,
				solvisDescription.getMiscellaneous().getMeasurementsBackupTime_ms());

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
				SystemGrafics systemGrafics = this.graficDatas.get(solvis.getUnit().getId(), this.xmlHash) ;
				systemGrafics.setControlFileHashCode(this.xmlHash.getFileHash()) ;
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
				misc.getPowerOffDetectedAfterTimeout_ms(), unit.isFwLth2_21_02A());
		String timeZone = this.baseData.getTimeZone();
		Solvis solvis = new Solvis(unit, this.solvisDescription, this.graficDatas.get(unit.getId(), this.xmlHash), connection,
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
