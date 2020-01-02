package de.sgollmer.solvismax.model.update;

import javax.xml.namespace.QName;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import de.sgollmer.solvismax.error.XmlError;
import de.sgollmer.solvismax.model.CommandControl;
import de.sgollmer.solvismax.model.CommandScreenRestore;
import de.sgollmer.solvismax.model.Solvis;
import de.sgollmer.solvismax.model.objects.AllSolvisData;
import de.sgollmer.solvismax.model.objects.Observer.ObserverI;
import de.sgollmer.solvismax.model.objects.SolvisDescription;
import de.sgollmer.solvismax.model.objects.control.Control;
import de.sgollmer.solvismax.model.objects.data.SolvisData;
import de.sgollmer.solvismax.model.update.UpdateStrategies.Strategy;
import de.sgollmer.solvismax.model.update.UpdateStrategies.UpdateCreator;
import de.sgollmer.solvismax.xml.BaseCreator;
import de.sgollmer.solvismax.xml.CreatorByXML;

public class HeatingBurner extends Strategy<HeatingBurner> {

	private static final Logger logger = LogManager.getLogger(HeatingBurner.class);

	private final String burnerId;
	private final String burnerCalcId;
	private final int factor;
	private final String checkIntervallId;
	private final String readIntervallId;
	private final boolean hourly;

	public HeatingBurner(String burnerId, String burnerCalcId, int factor, String checkIntervallId,
			String readIntervallId, boolean hourly) {
		this.burnerId = burnerId;
		this.burnerCalcId = burnerCalcId;
		this.factor = factor;
		this.checkIntervallId = checkIntervallId;
		this.readIntervallId = readIntervallId;
		this.hourly = hourly;
	}

	public HeatingBurner() {
		this(null, null, -1, null, null, false);
	}

	@Override
	public void instantiate(Solvis solvis) {
		AllSolvisData allData = solvis.getAllSolvisData();
		SolvisData burner = allData.checkAndGet(this.burnerId);

		SolvisData burnerRunTime = allData.checkAndGet(this.burnerCalcId);

		SolvisData toUpdate = allData.checkAndGet(this.source.getDescription().getId());

		int checkIntervall = -1;
		int readIntervall = -1;

		if (this.factor >= 0) {
			checkIntervall = solvis.getDuration(this.checkIntervallId).getTime_ms();
			readIntervall = solvis.getDuration(this.readIntervallId).getTime_ms();
		}

		new Executable(solvis, toUpdate, burner, burnerRunTime, factor, checkIntervall, readIntervall, hourly);

	}

	@Override
	public void assign(SolvisDescription description) {

	}

	private class Executable implements ObserverI<SolvisData> {

		private final Solvis solvis;
		private final SolvisData toUpdate;
		private final SolvisData burner;
		private final SolvisData burnerCalcValue;
		private final int factor;
		private final int checkIntervall;
		private final int readIntervall;
		private final boolean hourly;

		private long lastCheckTime = -1;
		private boolean syncActive = false;
		private boolean lastBurnerState = false;
		private boolean screenRestore = true;

		public Executable(Solvis solvis, SolvisData toUpdate, SolvisData burner, SolvisData burnerCalcValue, int factor,
				int checkIntervall, int readIntervall, boolean hourly) {
			this.solvis = solvis;
			this.toUpdate = toUpdate;
			this.burner = burner;
			this.burnerCalcValue = burnerCalcValue;
			this.factor = factor;
			this.checkIntervall = checkIntervall;
			this.readIntervall = readIntervall;
			this.hourly = hourly;

			this.burner.registerContinuousObserver(this);
			this.toUpdate.register(new ObserverI<SolvisData>() {

				@Override
				public void update(SolvisData data, Object source) {
					updateByControl(data, source);

				}
			});

		}

		private void updateByControl(SolvisData data, Object source ) {
			int controlData = data.getInt();
			int calcData = this.burnerCalcValue.getInt();

			boolean update = false;

			if (factor > 0) {
				controlData *= this.factor;
				if (syncActive) {
					this.syncActive = false;
					update = true;
					logger.info("Synchronisation  of <" + burnerCalcId + "> finished");
				} else if (controlData > calcData || controlData + this.factor < calcData) {
					update = true;
					if (controlData > calcData + 0.1 * this.factor || controlData + this.factor < calcData) {
						this.syncActive = true;
						logger.info("Synchronisation  of <" + burnerCalcId + "> activated");
					}
				}
				if (update) {
				}
			} else if (calcData != controlData) {
				update = true;
			}
			if (update) {
				logger.info("Update of <" + burnerCalcId + "> by SolvisConrol data take place, former: " + calcData
						+ ", new: " + controlData);
				this.burnerCalcValue.setInteger(controlData);
			}
		}

		private void updateByMeasurement(SolvisData data) {

			if (!(source instanceof Control)) {
				return;
			}

			long time = System.currentTimeMillis();

			boolean burnerOn = data.getBool();

			int currentCalcValue = this.burnerCalcValue.getInt();

			if (burnerOn || this.lastBurnerState) {

				boolean checkP = this.checkIntervall > 0 && time > this.lastCheckTime + this.checkIntervall; // periodic
																												// check
				boolean checkC = this.readIntervall > 0 && this.syncActive && time > this.lastCheckTime + this.readIntervall;
				checkC |= this.syncActive && !burnerOn && this.lastBurnerState;
				int nextHour = (currentCalcValue / this.factor + 1) * this.factor ; 
				checkC |= this.hourly
						&& currentCalcValue > nextHour - 4 * this.readIntervall/1000;

				if (checkC && burnerOn && this.screenRestore) {
					this.screenRestore = false;
					this.solvis.execute(new CommandScreenRestore(this.screenRestore));
				}

				if (checkC || checkP) {
					this.lastCheckTime = time;
					this.solvis.execute(new CommandControl(((Control) source).getDescription()));
					logger.debug("Update of <" + burnerCalcId + "> requested.");
				}
			} else if (!this.screenRestore) {
				this.screenRestore = true;
				this.solvis.execute(new CommandScreenRestore(this.screenRestore));
			}
			this.lastBurnerState = burnerOn;
		}

		@Override
		public void update(SolvisData data, Object Source) { // by burner
			updateByMeasurement(data);
		}

	}

	public static class Creator extends UpdateCreator<HeatingBurner> {

		private String burnerId;
		private String burnerCalcId;
		private int factor = -1;
		private String checkIntervallId;
		private String readIntervalId;
		private boolean hourly = false;

		public Creator() {
			super(null, null);
		}

		public Creator(String id, BaseCreator<?> creator) {
			super(id, creator);
		}

		@Override
		public void setAttribute(QName name, String value) {
			switch (name.getLocalPart()) {
				case "burnerId":
					this.burnerId = value;
					break;
				case "calculatedId":
					this.burnerCalcId = value;
					break;
				case "factor":
					this.factor = Integer.parseInt(value);
					break;
				case "checkIntervallId":
					this.checkIntervallId = value;
					break;
				case "readIntervalId":
					this.readIntervalId = value;
					break;
				case "hourly":
					this.hourly = Boolean.parseBoolean(value);
			}
		}

		@Override
		public HeatingBurner create() throws XmlError {
			return new HeatingBurner(burnerId, burnerCalcId, factor, checkIntervallId, readIntervalId, hourly);
		}

		@Override
		public CreatorByXML<?> getCreator(QName name) {
			return null;
		}

		@Override
		public void created(CreatorByXML<?> creator, Object created) {

		}

		@Override
		public UpdateCreator<HeatingBurner> createCreator(String id, BaseCreator<?> creator) {
			return new Creator(id, creator);
		}
	}
}
