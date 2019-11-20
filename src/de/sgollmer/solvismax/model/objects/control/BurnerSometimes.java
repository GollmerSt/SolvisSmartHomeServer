package de.sgollmer.solvismax.model.objects.control;

import de.sgollmer.solvismax.model.Command;
import de.sgollmer.solvismax.model.Solvis;
import de.sgollmer.solvismax.model.objects.AllDataDescriptions;
import de.sgollmer.solvismax.model.objects.AllSolvisData;
import de.sgollmer.solvismax.model.objects.Observer.ObserverI;
import de.sgollmer.solvismax.model.objects.control.UpdateStrategies.Strategy;
import de.sgollmer.solvismax.model.objects.data.SolvisData;

public class BurnerSometimes extends Strategy<BurnerSometimes> {

	private final String burnerId;
	private final String runParameterId;
	private final int factor;
	private final String checkIntervallId;
	private final String readIntervalId;

	public BurnerSometimes(Control control, String burnerId, String runParameterId, int factor, String checkIntervallId,
			String readIntervalId) {
		super(control);
		this.burnerId = burnerId;
		this.runParameterId = runParameterId;
		this.factor = factor;
		this.checkIntervallId = checkIntervallId;
		this.readIntervalId = readIntervalId;
	}

	public BurnerSometimes() {
		this(null, null, null, -1, null, null);
	}

	@Override
	public BurnerSometimes create(Control control, String irgendwasWieXML) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public void instantiate(Solvis solvis) {
		AllSolvisData allData = solvis.getAllSolvisData();
		SolvisData burner = allData.checkAndGet(this.burnerId);

		SolvisData burnerRunTime = allData.checkAndGet(this.runParameterId);

		SolvisData burnerSolvisRunTime = allData.checkAndGet(this.control.getDescription().getId());

		int checkIntervall = solvis.getDuration(this.checkIntervallId).getDuration_ms();
		int readIntervall = solvis.getDuration(this.readIntervalId).getDuration_ms();

		new Executable(solvis, burner, burnerRunTime, burnerSolvisRunTime, factor, checkIntervall, readIntervall);

	}

	@Override
	public void assign(AllDataDescriptions descriptions) {

	}

	private class Executable implements ObserverI<SolvisData> {

		private final Solvis solvis;
		private final SolvisData burner;
		private final SolvisData burnerRunTime;
		private final SolvisData burnerSolvisRunTime;
		private final int factor;
		private final int checkIntervall;
		private final int readIntervall;

		private int lastValue = -1;
		private long lastCheckTime = -1;
		private boolean syncActive = false;
		private boolean lastBurnerState = false;
		private ObserverI<SolvisData> controlValueObserver = new ObserverI<SolvisData>() {

			@Override
			public void update(SolvisData data) {
				updateByControl(data);

			}
		};

		public Executable(Solvis solvis, SolvisData burner, SolvisData burnerRunTime, SolvisData burnerSolvisRunTime,
				int factor, int checkIntervall, int readIntervall) {
			this.solvis = solvis;
			this.burner = burner;
			this.burnerRunTime = burnerRunTime;
			this.burnerSolvisRunTime = burnerSolvisRunTime;
			this.factor = factor;
			this.checkIntervall = checkIntervall;
			this.readIntervall = readIntervall;

			this.burner.registerContinuousObserver(this);
			this.burnerSolvisRunTime.register(this.controlValueObserver);
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
			int solvisRunTime = this.burnerSolvisRunTime.getInt() * this.factor;
			if (this.syncActive) {
				if (this.lastValue < 0) {
					this.lastValue = solvisRunTime;
				} else if (this.lastValue != solvisRunTime) {
					this.burnerRunTime.setInteger(solvisRunTime);
					this.solvis.execute(new Command(null, false, true));
					this.syncActive = false;
				}
			} else {
				int runTime = this.burnerRunTime.getInt();

				if (runTime < solvisRunTime || runTime >= solvisRunTime + this.factor) {
					this.burnerRunTime.setInteger(solvisRunTime);
					if (this.factor > 1) {
						this.solvis.execute(new Command(null, true, false));
						this.syncActive = true;
					}
				}
			}
		}

		// private class SyncThread extends Thread {
		//
		// private boolean terminate = false;
		//
		// @Override
		// public void run() {
		// solvis.execute(new Command(null, true, false));
		//
		// DataDescription description =
		// BurnerSometimes.this.control.getDescription();
		//
		// int readPendingCnt = 10;
		//
		// while (!terminate) {
		// if (!readPending) {
		// Command command = new Command(description);
		// readPending = true;
		// solvis.execute(command);
		// } else {
		// --readPendingCnt;
		// if (readPendingCnt == 0) {
		// readPending = false;
		// }
		// }
		// synchronized (BurnerSometimes.this) {
		// try {
		// this.wait(readIntervall);
		// } catch (InterruptedException e) {
		// }
		// }
		// }
		// readPending = false;
		// solvis.execute(new Command(null, false, true));
		//
		// }
		//
		// public void terminate() {
		// synchronized (BurnerSometimes.this) {
		// this.terminate = true;
		// this.notifyAll();
		// }
		// }
		//
		// }
		//
		// private synchronized void stopSynchronisation() {
		// this.syncActive = false;
		// if (this.syncThread != null) {
		// this.syncThread.terminate();
		// this.syncThread = null;
		// }
		// }
		//
		// private synchronized void startSynchronisation() {
		// this.syncActive = true;
		// if (this.syncThread != null) {
		// this.syncThread.terminate();
		// }
		// this.syncThread = new SyncThread();
		// }
		//
		// }
	}
}
