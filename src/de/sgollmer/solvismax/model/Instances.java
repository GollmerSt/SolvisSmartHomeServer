package de.sgollmer.solvismax.model;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;

import javax.xml.stream.XMLStreamException;

import de.sgollmer.solvismax.connection.AccountInfo;
import de.sgollmer.solvismax.connection.SolvisConnection;
import de.sgollmer.solvismax.connection.transfer.ConnectPackage;
import de.sgollmer.solvismax.error.XmlError;
import de.sgollmer.solvismax.model.objects.AllSolvisGrafics;
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
	private final int xmlHash ;
	private final String writeablePath ;

	public Instances(String writeablePath, Unit cliUnit) throws IOException, XmlError, XMLStreamException {
		this.writeablePath = writeablePath ;
		this.baseData = new BaseControlFileReader(writeablePath).read().getTree();
		ControlFileReader reader = new ControlFileReader(this.writeablePath);
		Result<SolvisDescription> result = reader.read();
		this.solvisDescription = result.getTree();
		this.xmlHash = result.getHash() ;
		this.backupHandler = new MeasurementsBackupHandler(this.writeablePath,
				solvisDescription.getMiscellaneous().getMeasurementsBackupTime_ms());
		this.graficDatas = new GraficFileHandler(this.writeablePath, this.xmlHash).read();

		if (cliUnit != null) {
			Solvis solvis = this.createSolvisInstance(cliUnit.getId(), cliUnit.getUrl(), cliUnit);
			this.units.add(solvis);
		} else {
			for (Unit xmlUnit : baseData.getUnits().getUnits()) {
				Solvis solvis = this.createSolvisInstance(xmlUnit.getId(), xmlUnit.getUrl(), xmlUnit);
				this.units.add(solvis);
			}
		}
		this.backupHandler.start();
	}

	public synchronized Solvis getInstance(ConnectPackage connectPackage)
			throws IOException, XmlError, XMLStreamException {
		for (Solvis solvis : units) {
			if (solvis.getId().equals(connectPackage.getId())) {
				return solvis;
			}
		}
		Solvis solvis = null;
		if (connectPackage.containsSolvisLogin()) {
			solvis = createSolvisInstance(connectPackage.getId(), connectPackage.getUrl(), connectPackage);
			this.units.add(solvis);
		}
		return solvis;
	}

	private Solvis createSolvisInstance(String id, String url, AccountInfo accountInfo)
			throws IOException, XmlError, XMLStreamException {
		SolvisConnection connection = new SolvisConnection(url, accountInfo);
		Solvis solvis = new Solvis(id, this.solvisDescription, this.graficDatas.get(id), connection,
				this.backupHandler);
		if ( solvis.getGrafics().isEmpty()) {
			solvis.learning();
			
			new GraficFileHandler(this.writeablePath, this.xmlHash).write(this.graficDatas);
		}
		solvis.init();
		return solvis;
	}

	public SolvisDescription getSolvisDescription() {
		return solvisDescription;
	}
	
	public void terminate() {
		for ( Solvis unit : units ) {
			unit.terminate();
		}
		this.backupHandler.terminate(); 
	}
	
	public void backupMeasurements() throws IOException, XMLStreamException {
		this.backupHandler.write();
	}

}
