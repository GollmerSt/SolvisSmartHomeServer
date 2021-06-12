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
import de.sgollmer.solvismax.model.Solvis;
import de.sgollmer.solvismax.model.command.CommandControl;
import de.sgollmer.solvismax.model.command.CommandObserver;
import de.sgollmer.solvismax.model.command.CommandScreenRestore;
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

	private EquipmentOnOff(final String equipmentId, final String calculatedId, final int factor,
			final String checkIntervalId, final String readIntervalId, final boolean hourly,
			final Collection<String> triggerIds) {
		this.equipmentId = equipmentId;
		this.calculatedId = calculatedId;
		this.factor = factor;
		this.checkIntervalId = checkIntervalId;
		this.readIntervalId = readIntervalId;
		this.hourly = hourly;
		this.triggerIds = triggerIds;
	}

	@Override
	public void instantiate(final Solvis solvis) {
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
	public void assign(final SolvisDescription description) {

	}

	private enum Synchronisation {
		UPDATE_ONLY, SYNC_FINISHED, SYNC_MISSED, ENABLE_SYNC_BY_VALUE, SYNC_PREFFERED, NO_ACTION
	};

	private static class SynchronisationResult {
		private final Synchronisation synchronisation;
		private final int setValue;
		private final int currentValue;

		public SynchronisationResult(final Synchronisation synchronisation, final int newValue,
				final int currentValue) {
			this.synchronisation = synchronisation;
			this.setValue = newValue;
			this.currentValue = currentValue;
		}
	}

	private class Executable implements IObserver<SolvisData>, IExecutable {

		private final Solvis solvis;
		private final SolvisData updateSource;
		private final SolvisData equipment;
		private final SolvisData calculatedValue;
		private final int factor;
		private final int checkInterval;
		private final int readInterval;
		private final boolean hourly;
		private final boolean equipmentTimeSyncEnabled;

		private long lastCheckTime = -1;
		private boolean syncActiveForced; // Synchronisation forced by restart or big difference
		private boolean syncEnableHourly = false; // Synchronisation forced by hourly request
		private boolean syncPossible = false;

		private boolean lastEquipmentState = false;
		private SingleData<?> lastUpdateValue = null;
		private boolean monitor = false;
		private boolean otherExecuting = false;

		private int executionTime = 0;
		private int hourlyWindow_s = 0;

		private Executable(final Solvis solvis, final SolvisData updateSource, final SolvisData equipment,
				final SolvisData calculatedValue, final int factor, final int checkInterval, final int readInterval,
				final boolean hourly) {
			this.solvis = solvis;
			this.updateSource = updateSource;
			this.equipment = equipment;
			this.calculatedValue = calculatedValue;
			this.factor = factor;
			this.checkInterval = checkInterval;
			this.readInterval = readInterval;
			this.hourly = hourly;
			this.equipmentTimeSyncEnabled = this.solvis.getUnit().getFeatures().isEquipmentTimeSynchronisation();
			this.syncActiveForced = this.equipmentTimeSyncEnabled;
			this.equipment.registerContinuousObserver(this);
			this.setHourlyWindow(false);
			this.updateSource.registerContinuousObserver(new IObserver<SolvisData>() {

				@Override
				public void update(final SolvisData data, final Object source) {
					updateByControl(data, source);

				}
			});
			this.solvis.registerControlExecutingObserver(new IObserver<Boolean>() {

				@Override
				public void update(final Boolean data, final Object source) {
					if (data) {
						Executable.this.otherExecuting = true;
					} else if (Executable.this.otherExecuting) {
						Executable.this.otherExecuting = false;
						Executable.this.syncPossible = false;
					}

				}
			});
		}

		private void setHourlyWindow(final boolean enlarge) {

			int min = Constants.HOURLY_EQUIPMENT_WINDOW_READ_INTERVAL_FACTOR * this.readInterval / 1000;
			int max = 3600;

			if (enlarge) {
				this.hourlyWindow_s *= 2;

			} else {
				this.hourlyWindow_s /= 2;
			}

			if (this.hourlyWindow_s > max) {
				this.hourlyWindow_s = max;
			} else if (this.hourlyWindow_s < min) {
				this.hourlyWindow_s = min;
			}

		}

		private SynchronisationResult checkSynchronisation(final SolvisData controlData, final boolean equipmentOn,
				final boolean changed) throws TypeException {

			int factor = this.factor < 0 ? 1 : this.factor;
			int data = controlData.getInt() * factor;
			int currentData = this.calculatedValue.getInt();

			if (this.factor < 0) {
				if (!equipmentOn && currentData != data) {
					return new SynchronisationResult(Synchronisation.UPDATE_ONLY, data, currentData);
				} else if (data > currentData || data < currentData - 1) {
					return new SynchronisationResult(Synchronisation.UPDATE_ONLY, data, currentData);
				} else {
					return new SynchronisationResult(Synchronisation.NO_ACTION, currentData, currentData);
				}
			}

			int tolerance = Constants.SYNC_TOLERANCE_PERCENT * this.factor / 100;

			Synchronisation range = null;
			int setValue = data;

			if (currentData - tolerance > data + this.factor) {
				range = Synchronisation.ENABLE_SYNC_BY_VALUE;
				setValue = data + this.factor;
			} else if (currentData + tolerance < data) {
				range = Synchronisation.ENABLE_SYNC_BY_VALUE;
			} else if (currentData < data) {
				range = Synchronisation.UPDATE_ONLY;
			}

			if (changed && (this.syncActiveForced || this.syncEnableHourly)) {
				if (equipmentOn && this.syncPossible) {
					range = Synchronisation.SYNC_FINISHED;
				} else if (range == null) {
					range = Synchronisation.SYNC_MISSED;
				}
			}

			if (range != null) {
				return new SynchronisationResult(this.equipmentTimeSyncEnabled ? range : Synchronisation.UPDATE_ONLY, //
						setValue, currentData);

			} else {
				return new SynchronisationResult(Synchronisation.NO_ACTION, currentData, currentData);
			}

		}

		private void updateByControl(final SolvisData data, final Object source) {
			if (!this.solvis.isInitialized()) {
				return;
			}

			this.otherExecuting = false;

			if (data.getExecutionTime() > this.executionTime) {
				this.executionTime = data.getExecutionTime();
			}

			boolean update = false;
			SynchronisationResult result = null;
			UpdateType type = new UpdateType(false);

			synchronized (this) {

				boolean equipmentOn;

				try {
					equipmentOn = this.equipment.getBool();
				} catch (TypeException e1) {
					equipmentOn = false;
				}

				boolean changed = !data.getSingleData().equals(this.lastUpdateValue);

				if (!changed) {
					if (!this.syncPossible && this.factor > 0 && equipmentOn) {
						this.syncPossible = true;
						logger.debug("Synchronisation of <" + EquipmentOnOff.this.calculatedId + "> is possible.");
					}
				}

				this.lastUpdateValue = data.getSingleData();

				try {
					result = this.checkSynchronisation(data, equipmentOn, changed);
				} catch (TypeException e) {
					logger.error("TopicType exception, update ignored", e);
					return;
				}

				switch (result.synchronisation) {
					case UPDATE_ONLY:
						update = true;
						this.syncPossible = false;
						break;

					case SYNC_FINISHED:
						update = true;
						type = new UpdateType(true);
						this.syncActiveForced = false;
						this.syncPossible = false;
						logger.info("Synchronisation  of <" + EquipmentOnOff.this.calculatedId + "> finished.");
						this.setHourlyWindow(false);
						break;

					case ENABLE_SYNC_BY_VALUE:
						update = true;
						type = new UpdateType(false);
						this.syncActiveForced = true;
						this.syncPossible = equipmentOn;
						logger.info("Synchronisation  of <" + EquipmentOnOff.this.calculatedId + "> activated.");
						break;

					case SYNC_MISSED:
						this.syncPossible = false;
						this.setHourlyWindow(true);
						logger.info("Synchronisation  of <" + EquipmentOnOff.this.calculatedId + "> missed.");
						break;

					case SYNC_PREFFERED:
						if (!this.syncActiveForced) {
							this.syncActiveForced = true;
							this.syncPossible = equipmentOn;
							logger.info("Synchronisation  of <" + EquipmentOnOff.this.calculatedId + "> activated.");
						}
						break;
				}
			}

			if (update) {
				logger.info(
						"Update of <" + EquipmentOnOff.this.calculatedId + "> by SolvisConrol data take place, former: "
								+ result.currentValue + ", new: " + result.setValue);
				this.calculatedValue.setInteger(result.setValue, data.getTimeStamp(), type);
				notifyTriggerIds();
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
		private void updateByMeasurement(final SolvisData data) {

			if (!(EquipmentOnOff.this.source instanceof Control)) {
				return;
			}

			if (!this.equipmentTimeSyncEnabled) {
				return;
			}

			boolean check = false;
			boolean monitor = false;

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
					logger.error("TopicType exception, update ignored", e);
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

				int currentControlValue = -1;
				try {
					currentControlValue = this.updateSource.getInt();
				} catch (TypeException e) {
					logger.error("TopicType exception, update ignored", e);
					return;
				}

				int delta_s = this.hourlyWindow_s + this.executionTime;
				int nextHour = (currentControlValue + 1) * this.factor;

				boolean formerSyncActiveHourly = this.syncEnableHourly;
				this.syncEnableHourly = this.hourly && nextHour - delta_s < currentCalcValue;

				if (formerSyncActiveHourly != this.syncEnableHourly) {
					if (this.syncEnableHourly) {
						logger.info("Hourly synchronisation of <" + EquipmentOnOff.this.calculatedId + "> activated.");
					} else {
						logger.info(
								"Hourly synchronisation interval of <" + EquipmentOnOff.this.calculatedId + "> over.");

					}
				}

				boolean checkC = this.syncActiveForced || this.syncEnableHourly;

				if (!equipmentOn && !this.lastEquipmentState) {
					checkC = false;
				}

				check = checkC || checkOneShot;

				monitor = checkC && equipmentOn;

				this.lastEquipmentState = equipmentOn;
			}
			if (check) {

				this.lastCheckTime = time;
				this.solvis.execute(new CommandControl(((Control) EquipmentOnOff.this.source).getDescription(),
						this.solvis, Constants.Commands.EQUIPMENT_ON_OFF_PRIORITY));
				logger.debug("Update of <" + EquipmentOnOff.this.calculatedId + "> requested.");
			}

			if (this.monitor != monitor) {
				this.monitor = monitor;
				this.solvis.updateByMonitoringTask(monitor ? CommandObserver.Status.MONITORING_STARTED
						: CommandObserver.Status.MONITORING_FINISHED, data);
				this.solvis.execute(new CommandScreenRestore(!monitor, this));
			}
		}

		@Override
		public void update(final SolvisData data, final Object Source) { // by burner
			updateByMeasurement(data);
		}

		@Override
		public void trigger() {
			if (!this.syncActiveForced) {
				this.syncActiveForced = true;
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

		private Creator(final String id, final BaseCreator<?> creator) {
			super(id, creator);
		}

		@Override
		public void setAttribute(final QName name, final String value) {
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
		public CreatorByXML<?> getCreator(final QName name) {
			String id = name.getLocalPart();
			switch (id) {
				case XML_TRIGGER:
					return new Trigger.Creator(id, this.getBaseCreator());
			}

			return null;
		}

		@Override
		public void created(final CreatorByXML<?> creator, final Object created) {
			switch (creator.getId()) {
				case XML_TRIGGER:
					this.triggerIds.add(((Trigger) created).getId());
					break;
			}
		}

		@Override
		public UpdateCreator<EquipmentOnOff> createCreator(final String id, final BaseCreator<?> creator) {
			return new Creator(id, creator);
		}
	}

	public static class UpdateType {
		private final boolean syncType;

		UpdateType(final boolean syncType) {
			this.syncType = syncType;
		}

		public boolean isSyncType() {
			return this.syncType;
		}
	}
}
