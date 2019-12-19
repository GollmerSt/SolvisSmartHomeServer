package de.sgollmer.solvismax.model;

import java.io.IOException;
import java.util.ArrayDeque;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Queue;
import java.util.Set;

import org.slf4j.LoggerFactory;

import de.sgollmer.solvismax.error.ErrorPowerOn;
import de.sgollmer.solvismax.model.objects.ChannelDescription;
import de.sgollmer.solvismax.model.objects.Miscellaneous;
import de.sgollmer.solvismax.model.objects.Screen;
import de.sgollmer.solvismax.model.objects.data.SolvisData;

public class SolvisWorkers {

	private static final org.slf4j.Logger logger = LoggerFactory.getLogger(SolvisWorkers.class);

	private final Solvis solvis;
	private final WatchDog watchDog;

	private ControlWorkerThread controlsThread = null;
	private MeasurementsWorkerThread measurementsThread = null;
	private Set<ChannelDescription> commandsOfScreen = new HashSet<>();
	private Screen commandScreen = null;

	public SolvisWorkers(Solvis solvis) {
		this.solvis = solvis;
		this.watchDog = new WatchDog(solvis, solvis.getSolvisDescription().getSaver());
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

			Miscellaneous misc = solvis.getSolvisDescription().getMiscellaneous();
			int unsuccessfullWaitTime = misc.getUnsuccessfullWaitTime_ms();
			int watchDogTime = misc.getWatchDogTime_ms();

			while (!this.terminate) {
				Command command = null;
				synchronized (this) {
					if (this.queue.isEmpty()) {
						if (!queueWasEmpty && this.screenRestoreInhibitCnt == 0) {
							restoreScreen = true;
							queueWasEmpty = true;
						} else {
							try {
								this.wait(watchDogTime);
							} catch (InterruptedException e) {
							}
							if (this.queue.isEmpty()) {
								executeWatchDog = true;
								// this.screenRestoreInhibitCnt = 0;
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
								if (this.screenRestoreInhibitCnt > 0) {
									--this.screenRestoreInhibitCnt;
								}
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
						watchDog.execute();
					}
					if (command != null && !command.isInhibit() && command.getDescription() != null) {
						success = executeCommand(command);
					}
				} catch (IOException e) {
					success = false;
				} catch (ErrorPowerOn e2) {
					success = false;
					solvis.getSolvisState().powerOff();
				} catch (Throwable e3) {
					logger.error("Unknown error detected", e3);
					success = true;
				}
				if (success) {
					if (command != null) {
						queue.remove();
					}
				} else {
					synchronized (this) {
						try {
							this.wait(unsuccessfullWaitTime);
							watchDog.execute();
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
				boolean insert = true;

				if (command.getDescription() != null) {
					for (Iterator<Command> it = this.queue.iterator(); it.hasNext();) {
						Command cmp = it.next();
						if (command.getDescription() == cmp.getDescription()) {
							if (command.getSetValue() != null) {
								cmp.setInhibit(true);
							} else {
								insert = false;
							}
						}
					}
				}
				if (insert) {
					this.queue.add(command);
					this.notifyAll();
					watchDog.bufferNotEmpty();
				}
			}
		}
	}

	private boolean executeCommand(Command command) throws IOException, ErrorPowerOn {
		boolean success = false;
		ChannelDescription description = command.getDescription();
		Screen commandScreen = description.getScreen();
		if (this.solvis.getCurrentScreen() != commandScreen || this.commandScreen != commandScreen) {
			this.commandsOfScreen.clear();
		}
		this.commandScreen = commandScreen;
		boolean isIn = !this.commandsOfScreen.add(description);

		if (isIn) {
			this.solvis.clearCurrentImage();
			this.commandsOfScreen.clear();
			this.commandsOfScreen.add(description);
		}

		SolvisData data = solvis.getAllSolvisData().get(description);
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
		return success;
	}

	public void push(Command command) {
		if (controlsThread == null) {
			return;
		}
		this.controlsThread.push(command);
	}

	public void terminate() {
		this.watchDog.terminate();
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
					solvis.getSolvisState().connected();
				} catch (IOException e1) {
					++errorCount;
					if (errorCount == solvis.getSolvisDescription().getMiscellaneous()
							.getPowerOffDetectedAfterIoErrors()) {
						solvis.getSolvisState().powerOff();
					}
				} catch (ErrorPowerOn e2) {
					solvis.getSolvisState().remoteConnected();
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
