package de.sgollmer.solvismax.model.objects.control;

import javax.xml.namespace.QName;

import de.sgollmer.solvismax.error.XmlError;
import de.sgollmer.solvismax.model.Command;
import de.sgollmer.solvismax.model.Solvis;
import de.sgollmer.solvismax.model.objects.AllSolvisData;
import de.sgollmer.solvismax.model.objects.Observer.ObserverI;
import de.sgollmer.solvismax.model.objects.SolvisDescription;
import de.sgollmer.solvismax.model.objects.control.UpdateStrategies.Strategy;
import de.sgollmer.solvismax.model.objects.data.SolvisData;
import de.sgollmer.solvismax.xml.BaseCreator;
import de.sgollmer.solvismax.xml.CreatorByXML;

public class BurnerSometimes extends Strategy<BurnerSometimes> {

	private final String burnerId;
	private final String burnerCalcId;
	private final int factor;
	private final String checkIntervallId;
	private final String readIntervalId;
	private final boolean hourly;

	public BurnerSometimes(String burnerId, String burnerCalcId, int factor, String checkIntervallId,
			String readIntervalId, boolean hourly) {
		this.burnerId = burnerId;
		this.burnerCalcId = burnerCalcId;
		this.factor = factor;
		this.checkIntervallId = checkIntervallId;
		this.readIntervalId = readIntervalId;
		this.hourly = hourly;
	}

	public BurnerSometimes() {
		this(null, null, -1, null, null, false);
	}

	@Override
	public BurnerSometimes create(String irgendwasWieXML) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public void instantiate(Solvis solvis) {
		AllSolvisData allData = solvis.getAllSolvisData();
		SolvisData burner = allData.checkAndGet(this.burnerId);

		SolvisData burnerRunTime = allData.checkAndGet(this.burnerCalcId);

		SolvisData burnerSolvisRunTime = allData.checkAndGet(this.control.getDescription().getId());

		int checkIntervall = solvis.getDuration(this.checkIntervallId).getTime_ms();
		int readIntervall = solvis.getDuration(this.readIntervalId).getTime_ms();

		new Executable(solvis, burner, burnerRunTime, burnerSolvisRunTime, factor, checkIntervall, readIntervall);

	}

	@Override
	public void assign(SolvisDescription description) {

	}

	private class Executable implements ObserverI<SolvisData> {

		private final Solvis solvis;
		private final SolvisData burner;
		private final SolvisData burnerCalcValue;
		private final SolvisData burnerSolvisValue;
		private final int factor;
		private final int checkIntervall;
		private final int readIntervall;

		private int lastSolvisValue = -1;
		private long lastCheckTime = -1;
		private boolean syncActive = false;
		private boolean lastBurnerState = false;
		private ObserverI<SolvisData> controlValueObserver = new ObserverI<SolvisData>() {

			@Override
			public void update(SolvisData data) {
				updateByControl(data);

			}
		};

		public Executable(Solvis solvis, SolvisData burner, SolvisData burnerRunTime, SolvisData burnerSolvisValue,
				int factor, int checkIntervall, int readIntervall) {
			this.solvis = solvis;
			this.burner = burner;
			this.burnerCalcValue = burnerRunTime;
			this.burnerSolvisValue = burnerSolvisValue;
			this.factor = factor;
			this.checkIntervall = checkIntervall;
			this.readIntervall = readIntervall;

			this.burner.registerContinuousObserver(this);
			this.burnerSolvisValue.register(this.controlValueObserver);
		}

		@Override
		public void update(SolvisData data) {
			this.updateByBurner(data);

		}

		private void updateByBurner(SolvisData data) {
			long time = System.currentTimeMillis();

			boolean burnerOn = data.getBool();

			if (burnerOn || this.lastBurnerState) {

				boolean check = time > lastCheckTime + this.checkIntervall;
				check |= syncActive && time > lastCheckTime + this.readIntervall;
				check |= syncActive && !burnerOn && this.lastBurnerState;

				if (check) {
					this.lastCheckTime = time;
					this.solvis.execute(new Command(control.getDescription()));
				}
			}
			this.lastBurnerState = burnerOn;
		}

		private void updateByControl(SolvisData data) {
			int solvisValue = this.burnerSolvisValue.getInt() * this.factor;
			if (this.syncActive) {
				if (this.lastSolvisValue < 0) {
					this.lastSolvisValue = solvisValue;
				} else if (this.lastSolvisValue != solvisValue) {
					this.burnerCalcValue.setInteger(solvisValue);
					this.solvis.execute(new Command(null, false, true));
					this.syncActive = false;
				}
			} else {
				int runTime = this.burnerCalcValue.getInt();

				if (runTime < solvisValue || runTime >= solvisValue + this.factor) {
					this.burnerCalcValue.setInteger(solvisValue);
					if (this.factor > 1) {
						this.solvis.execute(new Command(null, true, false));
						this.syncActive = true;
					}
				}
			}
		}

	}

	public static class Creator extends CreatorByXML<BurnerSometimes> {

		private String burnerId;
		private String burnerCalcId;
		private int factor = 1;
		private String checkIntervallId;
		private String readIntervalId;
		private boolean hourly = false;

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
		public BurnerSometimes create() throws XmlError {
			return new BurnerSometimes(burnerId, burnerCalcId, factor, checkIntervallId, readIntervalId,hourly);
		}

		@Override
		public CreatorByXML<?> getCreator(QName name) {
			// TODO Auto-generated method stub
			return null;
		}

		@Override
		public void created(CreatorByXML<?> creator, Object created) {
			// TODO Auto-generated method stub

		}
	}
}
