/************************************************************************
 * 
 * $Id$
 *
 * 
 ************************************************************************/

package de.sgollmer.solvismax.model.update;

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
import de.sgollmer.solvismax.model.update.UpdateStrategies.Strategy;
import de.sgollmer.solvismax.model.update.UpdateStrategies.UpdateCreator;
import de.sgollmer.xmllibrary.BaseCreator;
import de.sgollmer.xmllibrary.CreatorByXML;
import de.sgollmer.xmllibrary.XmlException;

public class EquipmentOnOff extends Strategy<EquipmentOnOff> {

	private static final ILogger logger = LogManager.getInstance().getLogger(EquipmentOnOff.class);

	private final String equipmentId;
	private final String calculatedId;
	private final int factor;
	private final String checkIntervalId;
	private final String readIntervalId;
	private final boolean hourly;

	private EquipmentOnOff(String equipmentId, String calculatedId, int factor, String checkIntervalId,
			String readIntervalId, boolean hourly) {
		this.equipmentId = equipmentId;
		this.calculatedId = calculatedId;
		this.factor = factor;
		this.checkIntervalId = checkIntervalId;
		this.readIntervalId = readIntervalId;
		this.hourly = hourly;
	}

	private EquipmentOnOff() {
		this(null, null, -1, null, null, false);
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

		new Executable(solvis, toUpdate, equipment, calculatedValue, this.factor, checkInterval, readInterval,
				this.hourly);

	}

	@Override
	public void assign(SolvisDescription description) {

	}

	private class Executable implements IObserver<SolvisData> {

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
			int controlData;
			int calcData;
			try {
				controlData = data.getInt();
				calcData = this.calculatedValue.getInt();
			} catch (TypeException e) {
				logger.error("Type exception, update ignored", e);
				return;
			}

			boolean update = false;

			if (this.factor > 0) {
				controlData *= this.factor;
				if (this.syncActive) {
					this.syncActive = false;
					update = true;
					logger.info("Synchronisation  of <" + EquipmentOnOff.this.calculatedId + "> finished");
				} else if (controlData > calcData || controlData + this.factor < calcData) {
					update = true;
					if (controlData > calcData + 0.1 * this.factor || controlData + this.factor < calcData) {
						this.syncActive = true;
						logger.info("Synchronisation  of <" + EquipmentOnOff.this.calculatedId + "> activated");
					}
				}
			} else if (calcData != controlData) {
				update = true;
			}
			if (update) {
				logger.info("Update of <" + EquipmentOnOff.this.calculatedId
						+ "> by SolvisConrol data take place, former: " + calcData + ", new: " + controlData);
				this.calculatedValue.setInteger(controlData, data.getTimeStamp());
			}
		}

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
				this.solvis.execute(new CommandScreenRestore(this.screenRestore));
			}
			this.lastEquipmentState = equipmentOn;
		}

		@Override
		public void update(SolvisData data, Object Source) { // by burner
			updateByMeasurement(data);
		}

	}

	static class Creator extends UpdateCreator<EquipmentOnOff> {

		private String equipmentId;
		private String calculatedId;
		private int factor = -1;
		private String checkIntervalId;
		private String readIntervalId;
		private boolean hourly = false;

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
					this.readIntervalId, this.hourly);
		}

		@Override
		public CreatorByXML<?> getCreator(QName name) {
			return null;
		}

		@Override
		public void created(CreatorByXML<?> creator, Object created) {

		}

		@Override
		public UpdateCreator<EquipmentOnOff> createCreator(String id, BaseCreator<?> creator) {
			return new Creator(id, creator);
		}
	}
}
