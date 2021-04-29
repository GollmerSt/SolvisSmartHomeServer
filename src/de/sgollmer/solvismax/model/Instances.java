/************************************************************************
 * 
 * $Id$
 *
 * 
 ************************************************************************/

package de.sgollmer.solvismax.model;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;

import javax.xml.stream.XMLStreamException;

import de.sgollmer.solvismax.BaseData;
import de.sgollmer.solvismax.Constants;
import de.sgollmer.solvismax.connection.SolvisConnection;
import de.sgollmer.solvismax.error.AliasException;
import de.sgollmer.solvismax.error.AssignmentException;
import de.sgollmer.solvismax.error.FileException;
import de.sgollmer.solvismax.error.LearningException;
import de.sgollmer.solvismax.error.ReferenceException;
import de.sgollmer.solvismax.error.TerminationException;
import de.sgollmer.solvismax.error.TypeException;
import de.sgollmer.solvismax.helper.FileHelper;
import de.sgollmer.solvismax.model.objects.AllSolvisGrafics;
import de.sgollmer.solvismax.model.objects.ErrorDetection.WriteErrorScreens;
import de.sgollmer.solvismax.model.objects.Miscellaneous;
import de.sgollmer.solvismax.model.objects.SolvisDescription;
import de.sgollmer.solvismax.model.objects.SystemGrafics;
import de.sgollmer.solvismax.model.objects.backup.BackupHandler;
import de.sgollmer.solvismax.model.objects.unit.Unit;
import de.sgollmer.solvismax.xml.ControlFileReader;
import de.sgollmer.solvismax.xml.ControlFileReader.Hashes;
import de.sgollmer.solvismax.xml.GraficFileHandler;
import de.sgollmer.xmllibrary.XmlException;

public class Instances {
	private Collection<Solvis> units = new ArrayList<>();
	private final SolvisDescription solvisDescription;
	private final BaseData baseData;
	private final BackupHandler backupHandler;
	private final AllSolvisGrafics graficDatas;
	private final Hashes xmlHash;
	private final File writeablePath;
	private final WriteErrorScreens writeErrorScreens;
	private boolean mustLearn;

	public Instances(BaseData baseData, boolean learn) throws IOException, XmlException, XMLStreamException,
			AssignmentException, FileException, ReferenceException {
		this.baseData = baseData;
		this.writeablePath = new File(baseData.getWritablePath());
		this.graficDatas = new GraficFileHandler(this.writeablePath).read();
		ControlFileReader reader = new ControlFileReader(this.writeablePath);
		ControlFileReader.Result result = reader.read(this.graficDatas.getControlHashCodes(), learn);
		this.solvisDescription = result.getSolvisDescription();
		this.solvisDescription.assign();
		this.mustLearn = result.mustLearn();
		this.xmlHash = result.getHashes();
		this.backupHandler = new BackupHandler(this.writeablePath,
				this.solvisDescription.getMiscellaneous().getMeasurementsBackupTime_ms());
		this.writeErrorScreens = new WriteErrorScreens(this);

		for (Unit xmlUnit : baseData.getUnits().getUnits()) {

			SystemGrafics systemGrafics = this.graficDatas.get(xmlUnit.getId(), this.xmlHash);

			this.mustLearn |= !systemGrafics.areRelevantFeaturesEqual(xmlUnit.getFeatures().getMap());
			this.mustLearn |= xmlUnit.getConfigOrMask() != systemGrafics.getBaseConfigurationMask();

			Solvis solvis = this.createSolvisInstance(xmlUnit, this.mustLearn);
			this.units.add(solvis);
		}
	}

	public void initialized() {
		this.backupHandler.start();
	}

	public void learn(boolean force)
			throws IOException, LearningException, XMLStreamException, FileException, TerminationException {
		boolean learned = true;
		File learnDesination = new File(this.writeablePath, Constants.Files.RESOURCE_DESTINATION);
		learnDesination = new File(learnDesination, Constants.Files.LEARN_DESTINATION);
		FileHelper.rmDir(learnDesination);
		FileHelper.mkdir(learnDesination);

		for (Solvis solvis : this.units) {
			solvis.learning(force);
			learned = true;
		}
		if (learned) {
			// this.graficDatas.
			new GraficFileHandler(this.writeablePath).write(this.graficDatas);
		}
	}

	public boolean init() throws IOException, XMLStreamException, LearningException, AssignmentException,
			AliasException, TypeException {
		for (Solvis solvis : this.units) {
			if (!solvis.getGrafics().isEmpty()) {
				solvis.init();
			} else {
				throw new LearningException("Learning is necessary, start parameter \"--server-learn\" must be used.");
			}
		}
		return true;
	}

	public boolean start() throws IOException, XMLStreamException, AssignmentException,
			AliasException, TypeException {
		for (Solvis solvis : this.units) {
			solvis.start();
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

	private Solvis createSolvisInstance(Unit unit, boolean mustLearn)
			throws IOException, XmlException, XMLStreamException {
		Miscellaneous misc = this.solvisDescription.getMiscellaneous();
		SolvisConnection connection = new SolvisConnection(unit.getUrl(), unit, misc.getSolvisConnectionTimeout_ms(),
				misc.getSolvisReadTimeout_ms(), misc.getPowerOffDetectedAfterIoErrors(),
				misc.getPowerOffDetectedAfterTimeout_ms(), unit.isFwLth2_21_02A());
		String timeZone = this.baseData.getTimeZone();
		Solvis solvis = new Solvis(unit, this.solvisDescription, this.graficDatas.get(unit.getId(), this.xmlHash),
				connection, this.backupHandler, timeZone, this.baseData.getEchoInhibitTime_ms(), this.writeablePath,
				mustLearn);
		if (this.baseData.getExceptionMail() != null && solvis.getFeatures().isSendMailOnError()) {
			solvis.registerSolvisErrorObserver(this.baseData.getExceptionMail());
		}
		solvis.registerSolvisErrorObserver(this.writeErrorScreens);
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

	public void backupMeasurements() throws IOException, XMLStreamException, FileException {
		this.backupHandler.write();
	}

	public Collection<Solvis> getUnits() {
		return this.units;
	}

	public Solvis getUnit(String unitId) {
		for (Solvis solvis : this.units) {
			if (unitId.equals(solvis.getUnit().getId())) {
				return solvis;
			}
		}
		return null;
	}

	public BackupHandler getBackupHandler() {
		return this.backupHandler;
	}

	public File getWritePath() {
		return this.writeablePath;
	}

	public boolean mustLearn() {
		return this.mustLearn;
	}
}
