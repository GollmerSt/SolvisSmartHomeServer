package de.sgollmer.solvismax.model;

import java.io.IOException;
import java.util.ArrayDeque;
import java.util.Iterator;
import java.util.Queue;

import de.sgollmer.solvismax.error.ErrorPowerOn;
import de.sgollmer.solvismax.model.objects.data.SolvisData;

public class SolvisWorkers {

	private final Solvis solvis;

	private ControlWorkerThread controlsThread = null;
	private MeasurementsWorkerThread measurementsThread = null;

	public SolvisWorkers(Solvis solvis) {
		this.solvis = solvis;
	}

	private class ControlWorkerThread extends Thread {

		private Queue<Command> queue = new ArrayDeque<>();
		private boolean terminate = false;
		int screenRestoreInhibitCnt = 0;

		public ControlWorkerThread() {
			super("ControlWorkerThread");
		}

		@Override
		public void run() {
			boolean queueWasEmpty = true;
			boolean saveScreen = false;
			boolean restoreScreen = false;
			boolean executeWatchDog = false;
			this.screenRestoreInhibitCnt = 0;
			long screenChangedByUser = -1;

			int releaseblockingAfterUserChange = solvis.getDuration("ReleaseblockingAfterUserChange").getTime_ms();
			int unsuccessfullWaitTime = solvis.getDuration("UnsuccessfullWaitTime").getTime_ms();

			while (!this.terminate) {
				Command command = null;
				synchronized (this) {
					boolean screenChanged = false;
					if (screenChangedByUser >= 0) {
						if (System.currentTimeMillis() > screenChangedByUser + releaseblockingAfterUserChange) {
							screenChanged = false;
							screenChangedByUser = -1;
							saveScreen = true;
						} else {
							screenChanged = true;
						}
					}

					if (this.queue.isEmpty() || screenChanged) {
						if (!queueWasEmpty && this.screenRestoreInhibitCnt == 0 && !screenChanged) {
							restoreScreen = true;
							queueWasEmpty = true;
						} else {
							try {
								this.wait(solvis.getDuration("WatchDogTime").getTime_ms());
							} catch (InterruptedException e) {
							}
							if (this.queue.isEmpty()) {
								executeWatchDog = true;
								this.screenRestoreInhibitCnt = 0;
							} else if (screenChanged) {
								executeWatchDog = true;
							}
						}

					} else {
						if (queueWasEmpty && screenRestoreInhibitCnt == 0) {
							saveScreen = true;
						}
						queueWasEmpty = false;
						command = this.queue.peek();
						if (!command.isInhibit()) {
							if (command.isScreenRestoreOff()) {
								++this.screenRestoreInhibitCnt;
							} else if (command.isScreenRestoreOn()) {
								--this.screenRestoreInhibitCnt;
							}
						}
					}
				}
				if (this.terminate) {
					return;
				}
				boolean success = true;
				try {
					if (saveScreen) {
						SolvisWorkers.this.solvis.saveScreen();
						saveScreen = false;
					} else if (restoreScreen) {
						SolvisWorkers.this.solvis.restoreScreen();
						restoreScreen = false;
					}
					if (executeWatchDog) {
						if (SolvisWorkers.this.solvis.getWatchDog().execute()) {
							screenChangedByUser = System.currentTimeMillis();
						}
						executeWatchDog = false;
					}
					if (command != null && !command.isInhibit() && command.getDescription() != null) {
						// SolvisData solvisData = solvis.ge;
						SolvisData data = solvis.getAllSolvisData().get(command.getDescription());
						if (command.getSetValue() == null) {
							success = command.getDescription().getValue(data, solvis);
						} else {
							SolvisData clone = data.clone();
							clone.setSingleData(command.getSetValue());
							success = command.getDescription().setValue(solvis, clone);
							if (success) {
								data.setSingleData(command.getSetValue());
							}
						}
					}
				} catch (IOException e) {
					success = false;
				} catch (ErrorPowerOn e2) {
					success = false;
					solvis.powerDetected(false);
				}
				if (success) {
					solvis.powerDetected(true);
					if (command != null) {
						queue.remove();
					}
				} else {
					synchronized (this) {
						try {
							this.wait(unsuccessfullWaitTime);
							if (SolvisWorkers.this.solvis.getWatchDog().execute()) {
								screenChangedByUser = System.currentTimeMillis();
							}
							executeWatchDog = false;
						} catch (InterruptedException e) {
						}
					}
				}
			}
		}

		public synchronized void terminate() {
			this.queue.clear();
			this.terminate = true;
			this.notifyAll();
		}

		public void push(Command command) {
			synchronized (this) {
				for (Iterator<Command> it = this.queue.iterator(); it.hasNext();) {
					Command cmp = it.next();
					if (command.getDescription() == cmp.getDescription()
							&& (cmp.getSetValue() == null) == (command.getSetValue() == null)) {
						cmp.setInhibit(true);
					}
				}
				this.queue.add(command);
				this.notifyAll();
			}
		}
	}

	public void push(Command command) {
		if (controlsThread == null) {
			return;
		}
		this.controlsThread.push(command);
	}

	public void terminate() {
		synchronized (this) {
			if (controlsThread != null) {
				controlsThread.terminate();
				this.controlsThread = null;
			}
			if (measurementsThread != null) {
				this.measurementsThread.terminate();
				this.measurementsThread = null;
			}
		}
	}

	public void start() {
		synchronized (this) {
			if (this.controlsThread == null) {
				this.controlsThread = new ControlWorkerThread();
				this.controlsThread.start();
			}
			if (this.measurementsThread == null) {
				this.measurementsThread = new MeasurementsWorkerThread();
				this.measurementsThread.start();
			}
		}
	}

	private class MeasurementsWorkerThread extends Thread {

		private boolean terminate = false;

		public MeasurementsWorkerThread() {
			super("MeasurementsWorkerThread");
		}

		@Override
		public void run() {
			int errorCount = 0;
			while (!terminate) {

				try {
					solvis.measure();
					errorCount = 0;
					solvis.powerDetected(true);
				} catch (IOException e) {
					++errorCount;
					if (errorCount == solvis.getSolvisDescription().getMiscellaneous()
							.getPowerOffDetectedAfterIoErrors()) {
						solvis.powerDetected(false);
					}
				}
				synchronized (this) {
					try {
						this.wait(
								solvis.getSolvisDescription().getMiscellaneous().getDefaultReadMeasurementsIntervall());
					} catch (InterruptedException e) {
					}
				}
			}
		}

		public synchronized void terminate() {
			this.terminate = true;
			this.notifyAll();
		}
	}
}
