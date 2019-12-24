package de.sgollmer.solvismax.model;

import java.io.IOException;
import java.util.ArrayDeque;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Queue;
import java.util.Set;

import org.apache.logging.log4j.core.util.SystemClock;
import org.slf4j.LoggerFactory;

import de.sgollmer.solvismax.Constants;
import de.sgollmer.solvismax.error.ErrorPowerOn;
import de.sgollmer.solvismax.error.TerminationException;
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
			// this.screenRestoreInhibitCnt = 0;

			Miscellaneous misc = solvis.getSolvisDescription().getMiscellaneous();
			int unsuccessfullWaitTime = misc.getUnsuccessfullWaitTime_ms();
			int watchDogTime = misc.getWatchDogTime_ms();

			synchronized (this) {
				this.notifyAll();
			}

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
				} catch (TerminationException e3) {
					return;
				} catch (Throwable e4) {
					logger.error("Unknown error detected", e4);
					success = true;
				}
				synchronized (this) {
					if (success) {
						if (command != null) {
							queue.remove();
						}
					} else {
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
		Screen commandScreen = description.getScreen(solvis.getConfigurationMask());
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
				synchronized (this.controlsThread) {
					try {
						this.controlsThread.start();
						this.controlsThread.wait(); // in case of
					} catch (InterruptedException e) {
					}
				}
			}
			if (this.measurementsThread == null) {
				this.measurementsThread = new MeasurementsWorkerThread();
				this.measurementsThread.start();
			}
		}
	}

	private class MeasurementsWorkerThread extends Thread {

		private boolean terminate = false;
		private long nextTime;

		public MeasurementsWorkerThread() {
			super("MeasurementsWorkerThread");

		}

		@Override
		public void run() {
			int measurementIntervall = solvis.getSolvisDescription().getMiscellaneous().getDefaultReadMeasurementsIntervall();
			int errorCount = 0;
			int powerOffDetectedAfterIoErrors = solvis.getSolvisDescription().getMiscellaneous()
					.getPowerOffDetectedAfterIoErrors() ;
			this.nextTime = System.currentTimeMillis() + measurementIntervall ;
			while (!terminate) {

				try {
					solvis.measure();
					errorCount = 0;
					solvis.getSolvisState().connected();
				} catch (IOException e1) {
					++errorCount;
					if (errorCount == powerOffDetectedAfterIoErrors) {
						solvis.getSolvisState().powerOff();
					}
				} catch (ErrorPowerOn e2) {
					solvis.getSolvisState().remoteConnected();
				}
				
				synchronized (this) {
					int waitTime = (int) (this.nextTime - System.currentTimeMillis()) ;
					if ( waitTime <= 0) {
						waitTime = Constants.WAITTIME_IF_LE_ZERO ;
					}
					this.nextTime += measurementIntervall ;
					try {
						this.wait(
								waitTime);
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
