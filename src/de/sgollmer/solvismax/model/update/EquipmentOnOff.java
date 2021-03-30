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
import de.sgollmer.solvismax.model.objects.data.SingleData;
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

	private enum Range {
		UPDATE_ONLY, OUT_OF_RANGE, SYNC_PREFFERED, NO_ACTION
	};

	private class Executable implements IObserver<SolvisData>, IExecutable {

		private final Solvis solvis;
		private final SolvisData updateSource;
		private final SolvisData equipment;
		private final SolvisData calculatedValue;
		private int lastSyncValue = -1;
		private final int factor;
		private final int checkInterval;
		private final int readInterval;
		private final boolean hourly;

		private long lastCheckTime = -1;
		private boolean syncActive = true; // Synchronisation forced after restart
		private boolean syncPossible = false;

		private boolean lastEquipmentState = false;
		private SingleData<?> lastUpdateValue = null;
		private boolean screenRestore = true;

		private Executable(Solvis solvis, SolvisData updateSource, SolvisData equipment, SolvisData calculatedValue,
				int factor, int checkInterval, int readInterval, boolean hourly) {
			this.solvis = solvis;
			this.updateSource = updateSource;
			this.equipment = equipment;
			this.calculatedValue = calculatedValue;
			this.factor = factor;
			this.checkInterval = checkInterval;
			this.readInterval = readInterval;
			this.hourly = hourly;

			this.equipment.registerContinuousObserver(this);
			this.updateSource.registerContinuousObserver(new IObserver<SolvisData>() {

				@Override
				public void update(SolvisData data, Object source) {
					updateByControl(data, source);

				}
			});
		}

		private class Result {
			private final Range range;
			private final int newValue;
			private final int currentValue;

			public Result(Range range, int newValue, int currentValue) {
				this.range = range;
				this.newValue = newValue;
				this.currentValue = currentValue;
			}
		}

		private Result checkRange(SolvisData controlData) throws TypeException {
			int factor = this.factor < 0 ? 1 : this.factor;
			int data = controlData.getInt() * factor;
			int currentData = this.calculatedValue.getInt();
			if (this.factor < 0) {
				return currentData == data ? //
						new Result(Range.NO_ACTION, currentData, currentData) //
						: new Result(Range.UPDATE_ONLY, data, currentData);
			}
			if (currentData < data || data <= currentData - this.factor) {
				return new Result(Range.OUT_OF_RANGE, data, currentData);
//			} else if (data > currentData + 0.1 * this.factor) {
//				return new Result(Range.SYNC_PREFFERED, data, currentData);
			} else {
				return new Result(Range.NO_ACTION, currentData, currentData);
			}

		}

		private void updateByControl(SolvisData data, Object source) {
			if (!this.solvis.isInitialized()) {
				return;
			}

			boolean update = false;
			Result result = null;

			synchronized (this) {

				boolean equipmentOn;

				try {
					equipmentOn = this.equipment.getBool();
				} catch (TypeException e1) {
					equipmentOn = false;
				}

				boolean equal = data.getSingleData().equals(this.lastUpdateValue);

				if (equal) {
					if (!this.syncPossible && this.factor > 0 && equipmentOn) {
						this.syncPossible = true;
						logger.debug("Synchronisation of <" + EquipmentOnOff.this.calculatedId + "> is possible.");
					}
					return;
				}

				this.lastUpdateValue = data.getSingleData();

				try {
					result = this.checkRange(data);
				} catch (TypeException e) {
					logger.error("Type exception, update ignored", e);
					return;
				}

				switch (result.range) {
					case UPDATE_ONLY:
						update = true;
						this.syncActive = false;
						this.syncPossible = false;
						break;
					case OUT_OF_RANGE:
						update = true;
						String resultString;
						if (this.syncPossible && this.syncActive) {
							this.syncActive = false;
							resultString = "finished";
						} else {
							this.syncActive = true;
							resultString = "activated";
						}
						logger.info(
								"Synchronisation  of <" + EquipmentOnOff.this.calculatedId + "> " + resultString + ".");
						break;
					case SYNC_PREFFERED:
						if (!this.syncActive) {
							this.syncActive = true;
							logger.info("Synchronisation  of <" + EquipmentOnOff.this.calculatedId + "> activated.");
						}
						break;
				}
			}

			if (update) {
				logger.info(
						"Update of <" + EquipmentOnOff.this.calculatedId + "> by SolvisConrol data take place, former: "
								+ result.currentValue + ", new: " + result.newValue);
				UpdateType type = new UpdateType(this.syncPossible);
				this.calculatedValue.setInteger(result.newValue, data.getTimeStamp(), type);
				notifyTriggerIds();
				this.lastSyncValue = result.newValue;
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

			boolean check = false;
			boolean screenRestore = true;

			long time = data.getTimeStamp();

			synchronized (this) {

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

				if (!equipmentOn) {
					if (this.syncPossible) {
						this.syncPossible = false;
						logger.debug("Synchronisation of <" + EquipmentOnOff.this.calculatedId
								+ "> isn't possible in case of equipment switched off.");
					}
				}

				boolean checkOneShot = this.checkInterval > 0 //
						&& time > this.lastCheckTime + this.checkInterval; // periodic check, only one check, no
																			// synchronisation

				int lowerHourly_s = -Constants.HOURLY_EQUIPMENT_SYNCHRONISATION_READ_INTERVAL_FACTOR * this.readInterval
						/ 1000;
				int nextHour = ((this.lastSyncValue - lowerHourly_s) / this.factor + 1) * this.factor;
				this.syncActive = this.syncActive || this.hourly && nextHour + lowerHourly_s < currentCalcValue;

				boolean checkC = this.syncActive;

				if (!equipmentOn && !this.lastEquipmentState) {
					checkC = false;
				}

				check = checkC || checkOneShot;

				screenRestore = !checkC || !equipmentOn;

				this.lastEquipmentState = equipmentOn;
			}
			if (check) {

				this.lastCheckTime = time;
				this.solvis.execute(
						new CommandControl(((Control) EquipmentOnOff.this.source).getDescription(), this.solvis));
				logger.debug("Update of <" + EquipmentOnOff.this.calculatedId + "> requested.");
			}

			if (this.screenRestore != screenRestore) {
				this.screenRestore = screenRestore;
				this.solvis.execute(new CommandScreenRestore(this.screenRestore, this));
			}
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
