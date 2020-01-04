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

import de.sgollmer.solvismax.connection.SolvisConnection;
import de.sgollmer.solvismax.connection.transfer.ConnectPackage;
import de.sgollmer.solvismax.error.LearningError;
import de.sgollmer.solvismax.error.XmlError;
import de.sgollmer.solvismax.model.objects.AllSolvisGrafics;
import de.sgollmer.solvismax.model.objects.Miscellaneous;
import de.sgollmer.solvismax.model.objects.SolvisDescription;
import de.sgollmer.solvismax.model.objects.Units.Unit;
import de.sgollmer.solvismax.model.objects.backup.MeasurementsBackupHandler;
import de.sgollmer.solvismax.xml.BaseControlFileReader;
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

	public Instances(String writeablePath) throws IOException, XmlError, XMLStreamException, LearningError {
		this.writeablePath = writeablePath;
		this.baseData = new BaseControlFileReader(writeablePath).read().getTree();
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

	public synchronized Solvis getInstance(ConnectPackage connectPackage)
			throws IOException, XmlError, XMLStreamException, LearningError {
		for (Solvis solvis : units) {
			if (solvis.getId().equals(connectPackage.getId())) {
				return solvis;
			}
		}
		return null;
	}

	private Solvis createSolvisInstance(Unit unit) throws IOException, XmlError, XMLStreamException, LearningError {
		Miscellaneous misc = solvisDescription.getMiscellaneous();
		SolvisConnection connection = new SolvisConnection(unit.getUrl(), unit, misc.getSolvisConnectionTimeout_ms(),
				misc.getSolvisReadTimeout_ms());
		String timeZone = this.baseData.getTimeZone();
		Solvis solvis = new Solvis(unit, this.solvisDescription, this.graficDatas.get(unit.getId()), connection,
				this.backupHandler, timeZone);
		if (solvis.getGrafics().isEmpty()) {
			solvis.learning();

			new GraficFileHandler(this.writeablePath, this.xmlHash).write(this.graficDatas);
		}
		solvis.init();
		return solvis;
	}

	public SolvisDescription getSolvisDescription() {
		return solvisDescription;
	}

	public void abort() {
		for (Solvis unit : units) {
			unit.abort();
		}
		this.backupHandler.abort();
	}

	public void backupMeasurements() throws IOException, XMLStreamException {
		this.backupHandler.write();
	}

}
