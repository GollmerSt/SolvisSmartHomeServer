/************************************************************************
 * 
 * $Id$
 *
 * 
 ************************************************************************/

package de.sgollmer.solvismax.model;

import java.io.IOException;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import de.sgollmer.solvismax.Constants;
import de.sgollmer.solvismax.error.ErrorPowerOn;
import de.sgollmer.solvismax.error.TerminationException;
import de.sgollmer.solvismax.model.CommandI.Handling;
import de.sgollmer.solvismax.model.objects.Miscellaneous;
import de.sgollmer.solvismax.model.objects.Observer.ObserverI;
import de.sgollmer.solvismax.model.objects.Screen;

public class SolvisWorkers {

	private static final Logger logger = LogManager.getLogger(SolvisWorkers.class);

	private final Solvis solvis;
	private final WatchDog watchDog;

	private ControlWorkerThread controlsThread = null;
	private MeasurementsWorkerThread measurementsThread = null;
	private Collection<CommandI> commandsOfScreen = new ArrayList<>();
	private Screen commandScreen = null;
	private long timeCommandScreen = System.currentTimeMillis();

	public SolvisWorkers(Solvis solvis) {
		this.solvis = solvis;
		this.watchDog = new WatchDog(solvis, solvis.getSolvisDescription().getSaver());
		this.solvis.registerAbortObserver(new ObserverI<Boolean>() {

			@Override
			public void update(Boolean data, Object source) {
				if (data) {
					abort();
				}
			}
		});
	}

	private class ControlWorkerThread extends Thread {

		private ArrayDeque<CommandI> queue = new ArrayDeque<>();
		private boolean abort = false;
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

			while (!this.abort) {
				boolean success;
				CommandI command = null;
				if (solvis.getSolvisState().getState() == SolvisState.State.SOLVIS_CONNECTED) {
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
								executeWatchDog = true;
							}

						} else {
							if (queueWasEmpty && screenRestoreInhibitCnt == 0) {
								saveScreen = true;
							}
							queueWasEmpty = false;
							command = this.queue.peek();
							if (!command.isInhibit()) {
								if (command.isScreenRestore() != null)
									if (!command.isScreenRestore()) {
										++this.screenRestoreInhibitCnt;
									} else {
										if (this.screenRestoreInhibitCnt > 0) {
											--this.screenRestoreInhibitCnt;
										}
									}
							}
						}
					}
					if (this.abort) {
						return;
					}
					success = true;

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
						if (command != null && !command.isInhibit()) {
							success = execute(command);
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
				} else {
					success = false;
				}
				synchronized (this) {
					if (success) {
						if (command != null) {
							boolean deleted = false;
							for (Iterator<CommandI> it = queue.iterator(); it.hasNext() && !deleted;) {
								CommandI cmp = it.next();
								if (command == cmp) {
									it.remove();
									deleted = true;
								}
							}
						}
					} else {
						try {
							this.wait(unsuccessfullWaitTime);
							if (solvis.getSolvisState().getState() == SolvisState.State.SOLVIS_CONNECTED) {
								watchDog.execute();
								executeWatchDog = false;
							}
						} catch (InterruptedException e) {
						}
					}
				}
			}
		}

		public synchronized void abort() {
			this.abort = true;
			this.notifyAll();
		}

		public void push(CommandI command) {
			synchronized (this) {
				boolean insert = true;

				for (Iterator<CommandI> it = this.queue.iterator(); it.hasNext();) {
					CommandI cmp = it.next();
					Handling handling = command.getHandling(cmp);
					if (handling.isInQueueInhibt()) {
						cmp.setInhibit(true);
					}
					if (handling.isAppendInhibit()) {
						insert = false;
					}
				}
				if (insert) {
					if (command.first()) {
						this.queue.addFirst(command);
					} else {
						this.queue.add(command);
					}
					this.notifyAll();
					watchDog.bufferNotEmpty();
				}
			}
		}

	}

	private boolean execute(CommandI command) throws IOException, ErrorPowerOn {
		Screen commandScreen = command.getScreen(this.solvis);
		if (commandScreen != null) {
			long now = System.currentTimeMillis();

			boolean clear ;

			if (this.solvis.getCurrentScreen() != commandScreen || this.commandScreen != commandScreen) {
				clear = true ;
			} else if (now > timeCommandScreen + Constants.TIME_COMMAND_SCREEN_VALID) {
				clear = true;
			} else {
				boolean isIn = this.commandsOfScreen.contains(command);
				if (!isIn) {
					clear = false ;
				} else {
					clear = true;
				}
			}
			if (clear) {
				this.solvis.clearCurrentImage();
				this.commandScreen = commandScreen;
				this.timeCommandScreen = now;
				this.commandsOfScreen.clear();
			}
			this.commandsOfScreen.add(command);
		}

		return command.execute(this.solvis);

	}

	public void push(CommandI command) {
		if (controlsThread == null) {
			return;
		}
		this.controlsThread.push(command);
	}

	public void abort() {
		synchronized (this) {
			if (controlsThread != null) {
				controlsThread.abort();
				this.controlsThread = null;
			}
			if (measurementsThread != null) {
				this.measurementsThread.abort();
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

		private boolean abort = false;
		private long nextTime;

		public MeasurementsWorkerThread() {
			super("MeasurementsWorkerThread");

		}

		@Override
		public void run() {
			int measurementIntervall = solvis.getDefaultReadMeasurementsIntervall_ms();
			int errorCount = 0;
			int powerOffDetectedAfterIoErrors = solvis.getSolvisDescription().getMiscellaneous()
					.getPowerOffDetectedAfterIoErrors();
			this.nextTime = System.currentTimeMillis() + measurementIntervall;
			while (!abort) {

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
					int waitTime = (int) (this.nextTime - System.currentTimeMillis());
					if (waitTime <= 0) {
						waitTime = Constants.WAITTIME_IF_LE_ZERO;
					}
					this.nextTime += measurementIntervall;
					try {
						this.wait(waitTime);
					} catch (InterruptedException e) {
					}
				}
			}
		}

		public synchronized void abort() {
			this.abort = true;
			this.notifyAll();
		}
	}
}
