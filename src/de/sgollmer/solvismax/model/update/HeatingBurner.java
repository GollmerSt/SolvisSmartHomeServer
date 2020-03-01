/************************************************************************
 * 
 * $Id$
 *
 * 
 ************************************************************************/

package de.sgollmer.solvismax.model.update;

import javax.xml.namespace.QName;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import de.sgollmer.solvismax.Constants;
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
	private final String checkIntervalId;
	private final String readIntervalId;
	private final boolean hourly;

	public HeatingBurner(String burnerId, String burnerCalcId, int factor, String checkIntervalId,
			String readIntervalId, boolean hourly) {
		this.burnerId = burnerId;
		this.burnerCalcId = burnerCalcId;
		this.factor = factor;
		this.checkIntervalId = checkIntervalId;
		this.readIntervalId = readIntervalId;
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

		int checkInterval = -1;
		int readInterval = -1;

		if (this.factor >= 0) {
			checkInterval = solvis.getDuration(this.checkIntervalId).getTime_ms();
			readInterval = solvis.getDuration(this.readIntervalId).getTime_ms();
		}

		new Executable(solvis, toUpdate, burner, burnerRunTime, factor, checkInterval, readInterval, hourly);

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
		private final int checkInterval;
		private final int readInterval;
		private final boolean hourly;

		private long lastCheckTime = -1;
		private boolean syncActive = false;
		private boolean lastBurnerState = false;
		private boolean screenRestore = true;

		public Executable(Solvis solvis, SolvisData toUpdate, SolvisData burner, SolvisData burnerCalcValue, int factor,
				int checkInterval, int readInterval, boolean hourly) {
			this.solvis = solvis;
			this.toUpdate = toUpdate;
			this.burner = burner;
			this.burnerCalcValue = burnerCalcValue;
			this.factor = factor;
			this.checkInterval = checkInterval;
			this.readInterval = readInterval;
			this.hourly = hourly;

			this.burner.registerContinuousObserver(this);
			this.toUpdate.register(new ObserverI<SolvisData>() {

				@Override
				public void update(SolvisData data, Object source) {
					updateByControl(data, source);

				}
			});

		}

		private void updateByControl(SolvisData data, Object source) {
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
			} else if (calcData != controlData) {
				update = true;
			}
			if (update) {
				logger.info("Update of <" + burnerCalcId + "> by SolvisConrol data take place, former: " + calcData
						+ ", new: " + controlData);
				this.burnerCalcValue.setInteger(controlData, data.getTimeStamp());
			}
		}

		private void updateByMeasurement(SolvisData data) {

			if (!(source instanceof Control)) {
				return;
			}

			if (!this.solvis.getUnit().getFeatures().isHeatingBurnerTimeSynchronisation()) {
				return;
			}
			long time = System.currentTimeMillis();

			boolean burnerOn = data.getBool();

			int currentCalcValue = this.burnerCalcValue.getInt();

			boolean screenRestore = true;

			boolean checkP = this.checkInterval > 0 && time > this.lastCheckTime + this.checkInterval; // periodic check
			boolean checkH = false;
			boolean checkC = false;

			if (burnerOn || this.lastBurnerState) {

				int checkIntervalHourly_s = Constants.HOURLY_BEARNER_SYNCHRONISATION_READ_INTERVAL_FACTOR
						* this.readInterval / 1000;
				int nextHour = ((currentCalcValue - checkIntervalHourly_s / 2) / this.factor + 1) * this.factor;
				checkH = this.hourly && currentCalcValue > nextHour - checkIntervalHourly_s / 2;

				checkC = this.syncActive && this.readInterval > 0 && time > this.lastCheckTime + this.readInterval;
				checkC |= this.syncActive && !burnerOn && this.lastBurnerState;

			}
			boolean check = checkC || checkP || checkH;
			if (check) {

				this.lastCheckTime = time;
				this.solvis.execute(new CommandControl(((Control) source).getDescription(), this.solvis));
				logger.debug("Update of <" + burnerCalcId + "> requested.");
			}

			if (checkH || this.syncActive) {
				screenRestore = !burnerOn;
			}

			if (this.screenRestore != screenRestore) {
				this.screenRestore = screenRestore;
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
		private String checkIntervalId;
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
		public HeatingBurner create() throws XmlError {
			return new HeatingBurner(burnerId, burnerCalcId, factor, checkIntervalId, readIntervalId, hourly);
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
