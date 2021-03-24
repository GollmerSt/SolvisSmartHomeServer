/************************************************************************
 * 
 * $Id$
 *
 * 
 ************************************************************************/

package de.sgollmer.solvismax.model.update;

import java.util.ArrayList;
import java.util.Collection;

import javax.xml.namespace.QName;

import de.sgollmer.solvismax.Constants;
import de.sgollmer.solvismax.error.TypeException;
import de.sgollmer.solvismax.log.LogManager;
import de.sgollmer.solvismax.log.LogManager.ILogger;
import de.sgollmer.solvismax.model.CommandControl;
import de.sgollmer.solvismax.model.CommandScreenRestore;
import de.sgollmer.solvismax.model.Solvis;
import de.sgollmer.solvismax.model.objects.AllSolvisData;
import de.sgollmer.solvismax.model.objects.Observer.IObserver;
import de.sgollmer.solvismax.model.objects.SolvisDescription;
import de.sgollmer.solvismax.model.objects.control.Control;
import de.sgollmer.solvismax.model.objects.data.SolvisData;
import de.sgollmer.solvismax.model.update.UpdateStrategies.IExecutable;
import de.sgollmer.solvismax.model.update.UpdateStrategies.Strategy;
import de.sgollmer.solvismax.model.update.UpdateStrategies.UpdateCreator;
import de.sgollmer.xmllibrary.BaseCreator;
import de.sgollmer.xmllibrary.CreatorByXML;
import de.sgollmer.xmllibrary.XmlException;

public class EquipmentOnOff extends Strategy<EquipmentOnOff> {

	private final boolean DEBUG = false;

	private static final String XML_TRIGGER = "Trigger";

	private static final ILogger logger = LogManager.getInstance().getLogger(EquipmentOnOff.class);

	private final String equipmentId;
	private final String calculatedId;
	private final int factor;
	private final String checkIntervalId;
	private final String readIntervalId;
	private final boolean hourly;
	private final Collection<String> triggerIds;

	private EquipmentOnOff(String equipmentId, String calculatedId, int factor, String checkIntervalId,
			String readIntervalId, boolean hourly, Collection<String> triggerIds) {
		this.equipmentId = equipmentId;
		this.calculatedId = calculatedId;
		this.factor = factor;
		this.checkIntervalId = checkIntervalId;
		this.readIntervalId = readIntervalId;
		this.hourly = hourly;
		this.triggerIds = triggerIds;
	}

	@Override
	public void instantiate(Solvis solvis) {
		AllSolvisData allData = solvis.getAllSolvisData();
		SolvisData equipment = allData.checkAndGet(this.equipmentId);

		SolvisData calculatedValue = allData.checkAndGet(this.calculatedId);

		SolvisData toUpdate = allData.checkAndGet(this.source.getDescription().getId());

		int checkInterval = -1;
		int readInterval = -1;

		if (this.factor >= 0) {
			checkInterval = solvis.getDuration(this.checkIntervalId).getTime_ms();
			readInterval = solvis.getDuration(this.readIntervalId).getTime_ms();
		}

		IExecutable executable = new Executable(solvis, toUpdate, equipment, calculatedValue, this.factor,
				checkInterval, readInterval, this.hourly);
		solvis.add(executable);
	}

	@Override
	public void assign(SolvisDescription description) {

	}

	private class Executable implements IObserver<SolvisData>, IExecutable {

		private final Solvis solvis;
		private final SolvisData toUpdate;
		private final SolvisData equipment;
		private final SolvisData calculatedValue;
		private final int factor;
		private final int checkInterval;
		private final int readInterval;
		private final boolean hourly;

		private long lastCheckTime = -1;
		private boolean syncActive = false;
		private boolean lastEquipmentState = false;
		private boolean screenRestore = true;

		private Executable(Solvis solvis, SolvisData toUpdate, SolvisData equipment, SolvisData calculatedValue,
				int factor, int checkInterval, int readInterval, boolean hourly) {
			this.solvis = solvis;
			this.toUpdate = toUpdate;
			this.equipment = equipment;
			this.calculatedValue = calculatedValue;
			this.factor = factor;
			this.checkInterval = checkInterval;
			this.readInterval = readInterval;
			this.hourly = hourly;

			this.equipment.registerContinuousObserver(this);
			this.toUpdate.register(new IObserver<SolvisData>() {

				@Override
				public void update(SolvisData data, Object source) {
					updateByControl(data, source);

				}
			});

		}


		private void updateByControl(SolvisData data, Object source) {
			if (!this.solvis.isInitialized())
				return;
			int controlData;
			int calcData;
			try {
				controlData = data.getInt();
				calcData = this.calculatedValue.getInt();
			} catch (TypeException e) {
				logger.error("Type exception, update ignored", e);
				return;
			}

			boolean updateBySync = false;
			boolean updateByReadout = false;

			if (this.factor > 0) {
				controlData *= this.factor;
				if (this.syncActive) {
					this.syncActive = false;
					updateBySync = true;
					logger.info("Synchronisation  of <" + EquipmentOnOff.this.calculatedId + "> finished.");
				} else if (controlData > calcData || controlData + this.factor < calcData) {
					updateByReadout = true;
					if (controlData > calcData + 0.1 * this.factor || controlData + this.factor < calcData) {
						this.syncActive = true;
						logger.info("Synchronisation  of <" + EquipmentOnOff.this.calculatedId + "> activated.");
					}
					notifyTriggerIds();
				}
			} else if (calcData != controlData) {
				updateByReadout = true;
				notifyTriggerIds();
			}
			if (updateByReadout|| updateBySync) {
				logger.info("Update of <" + EquipmentOnOff.this.calculatedId
						+ "> by SolvisConrol data take place, former: " + calcData + ", new: " + controlData);
				UpdateType type = new UpdateType(updateBySync);
				this.calculatedValue.setInteger(controlData, data.getTimeStamp(), type);
			}
		}

		private void notifyTriggerIds() {
			for (String triggerId : EquipmentOnOff.this.triggerIds) {
				Collection<IExecutable> executables = this.solvis.getUpdateStrategies(triggerId);
				if (executables != null) {
					for (IExecutable executable : executables) {
						executable.trigger();
					}
				}
			}
		}

		@SuppressWarnings("unused")
		private void updateByMeasurement(SolvisData data) {

			if (!(EquipmentOnOff.this.source instanceof Control)) {
				return;
			}

			if (!this.solvis.getFeatures().isEquipmentTimeSynchronisation()) {
				return;
			}
			long time = System.currentTimeMillis();

			boolean equipmentOn;
			int currentCalcValue;
			try {
				equipmentOn = data.getBool();

				if (EquipmentOnOff.this.DEBUG && data.getDescription().getId().equals("A12.Brenner")) {
					equipmentOn = true;
				}

				if (EquipmentOnOff.this.DEBUG && data.getDescription().getId().equals("A13.Brenner_S2")) {
					equipmentOn = true;
				}

				currentCalcValue = this.calculatedValue.getInt();
			} catch (TypeException e) {
				logger.error("Type exception, update ignored", e);
				return;
			}

			boolean screenRestore = true;

			boolean checkP = this.checkInterval > 0 && time > this.lastCheckTime + this.checkInterval; // periodic check
			boolean checkH = false;
			boolean checkC = false;

			if (equipmentOn || this.lastEquipmentState) {

				int checkIntervalHourly_s = Constants.HOURLY_EQUIPMENT_SYNCHRONISATION_READ_INTERVAL_FACTOR
						* this.readInterval / 1000;
				int nextHour = ((currentCalcValue - checkIntervalHourly_s / 2) / this.factor + 1) * this.factor;
				checkH = this.hourly && currentCalcValue > nextHour - checkIntervalHourly_s / 2;

				checkC = this.syncActive && this.readInterval > 0 && time > this.lastCheckTime + this.readInterval;
				checkC |= this.syncActive && !equipmentOn && this.lastEquipmentState;

			}
			boolean check = checkC || checkP || checkH;
			if (check) {

				this.lastCheckTime = time;
				this.solvis.execute(
						new CommandControl(((Control) EquipmentOnOff.this.source).getDescription(), this.solvis));
				logger.debug("Update of <" + EquipmentOnOff.this.calculatedId + "> requested.");
			}

			if (checkH || this.syncActive) {
				screenRestore = !equipmentOn;
			}

			if (this.screenRestore != screenRestore) {
				this.screenRestore = screenRestore;
				this.solvis.execute(new CommandScreenRestore(this.screenRestore, this));
			}
			this.lastEquipmentState = equipmentOn;
		}

		@Override
		public void update(SolvisData data, Object Source) { // by burner
			updateByMeasurement(data);
		}

		@Override
		public void trigger() {
			if (!this.syncActive) {
				this.syncActive = true;
				logger.info("Synchronisation  of <" + EquipmentOnOff.this.calculatedId + "> activated.");
			}
		}

		@Override
		public String getTriggerId() {
			return this.calculatedValue.getId();
		}

	}

	static class Creator extends UpdateCreator<EquipmentOnOff> {

		private String equipmentId;
		private String calculatedId;
		private int factor = -1;
		private String checkIntervalId;
		private String readIntervalId;
		private boolean hourly = false;
		private final Collection<String> triggerIds = new ArrayList<>();

		Creator() {
			super(null, null);
		}

		private Creator(String id, BaseCreator<?> creator) {
			super(id, creator);
		}

		@Override
		public void setAttribute(QName name, String value) {
			switch (name.getLocalPart()) {
				case "equipmentId":
					this.equipmentId = value;
					break;
				case "calculatedId":
					this.calculatedId = value;
					break;
				case "factor":
					this.factor = Integer.parseInt(value);
					break;
				case "checkIntervalId":
					this.checkIntervalId = value;
					break;
				case "readIntervalId":
					this.readIntervalId = value;
					break;
				case "hourly":
					this.hourly = Boolean.parseBoolean(value);
			}
		}

		@Override
		public EquipmentOnOff create() throws XmlException {
			return new EquipmentOnOff(this.equipmentId, this.calculatedId, this.factor, this.checkIntervalId,
					this.readIntervalId, this.hourly, this.triggerIds);
		}

		@Override
		public CreatorByXML<?> getCreator(QName name) {
			String id = name.getLocalPart();
			switch (id) {
				case XML_TRIGGER:
					return new Trigger.Creator(id, this.getBaseCreator());
			}

			return null;
		}

		@Override
		public void created(CreatorByXML<?> creator, Object created) {
			switch (creator.getId()) {
				case XML_TRIGGER:
					this.triggerIds.add(((Trigger) created).getId());
					break;
			}
		}

		@Override
		public UpdateCreator<EquipmentOnOff> createCreator(String id, BaseCreator<?> creator) {
			return new Creator(id, creator);
		}
	}

	public static class UpdateType {
		private final boolean syncType;

		UpdateType(boolean syncType) {
			this.syncType = syncType;
		}

		public boolean isSyncType() {
			return this.syncType;
		}
	}
}
