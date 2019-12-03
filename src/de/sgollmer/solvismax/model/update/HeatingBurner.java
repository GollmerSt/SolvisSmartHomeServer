package de.sgollmer.solvismax.model.update;

import javax.xml.namespace.QName;

import de.sgollmer.solvismax.error.XmlError;
import de.sgollmer.solvismax.model.Command;
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

		private int lastCalcValue = -1;
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
				public void update(SolvisData data) {
					updateByControl(data);

				}
			});

		}

		private void updateByControl(SolvisData data) {
			int controlData = data.getInt();
			int calcData = this.burnerCalcValue.getInt();

			if (factor > 0) {
				controlData *= this.factor;
				if (syncActive) {
					this.syncActive = false;
					this.burnerCalcValue.setInteger(controlData);
				}
				if (controlData > calcData || controlData + this.factor < calcData) {
					this.syncActive = true;
					this.burnerCalcValue.setInteger(controlData);
				}
			} else {
				this.burnerCalcValue.setInteger(controlData);
			}
		}

		@Override
		public void update(SolvisData data) { // by burner

			if (!(source instanceof Control)) {
				return;
			}

			long time = System.currentTimeMillis();

			boolean burnerOn = data.getBool();

			int currentCalcValue = this.burnerCalcValue.getInt();

			if (burnerOn || this.lastBurnerState) {

				boolean checkP = time > this.lastCheckTime + this.checkIntervall;
				boolean checkC = this.syncActive && time > lastCheckTime + this.readIntervall;
				checkC |= this.syncActive && !burnerOn && this.lastBurnerState;
				checkC |= this.lastCalcValue >= 0 && this.hourly
						&& currentCalcValue > this.lastCalcValue + this.factor - 2 * this.readIntervall;

				if (checkC && burnerOn && !this.lastBurnerState && this.screenRestore) {
					this.screenRestore = false;
					this.solvis.execute(new Command(this.screenRestore));
				}

				if (checkC || checkP) {
					this.lastCheckTime = time;
					this.solvis.execute(new Command(((Control) source).getDescription()));
				}
			} else if (!this.screenRestore) {
				this.screenRestore = true;
				this.solvis.execute(new Command(this.screenRestore));
			}
			this.lastBurnerState = burnerOn;
			this.lastCalcValue = currentCalcValue;
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
