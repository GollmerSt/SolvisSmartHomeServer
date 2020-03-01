/************************************************************************
 * 
 * $Id$
 *
 * 
 ************************************************************************/

package de.sgollmer.solvismax.model;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.ListIterator;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import de.sgollmer.solvismax.Constants;
import de.sgollmer.solvismax.error.ErrorPowerOn;
import de.sgollmer.solvismax.error.TerminationException;
import de.sgollmer.solvismax.helper.AbortHelper;
import de.sgollmer.solvismax.model.Command.Handling;
import de.sgollmer.solvismax.model.objects.Miscellaneous;
import de.sgollmer.solvismax.model.objects.Observer.ObserverI;
import de.sgollmer.solvismax.model.objects.screen.Screen;

public class SolvisWorkers {

	private static final Logger logger = LogManager.getLogger(SolvisWorkers.class);

	private final Solvis solvis;
	private final WatchDog watchDog;

	private ControlWorkerThread controlsThread = null;
	private MeasurementsWorkerThread measurementsThread = null;
	private Collection<Command> commandsOfScreen = new ArrayList<>();
	private Screen commandScreen = null;
	private long timeCommandScreen = System.currentTimeMillis();
	private final boolean controlEnable;

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
		this.controlEnable = !this.solvis.getUnit().getFeatures().isOnlyMeasurements();

	}

	private class ControlWorkerThread extends Thread {

		private LinkedList<Command> queue = new LinkedList<>();
		private boolean abort = false;
		private int screenRestoreInhibitCnt = 0;
		private int optimizationInhibitCnt = 0;
		private int commandDisableCount = 0;

		public ControlWorkerThread() {
			super("ControlWorkerThread");
		}

		@Override
		public void run() {
			boolean queueWasEmpty = true;
			boolean saveScreen = false;
			boolean restoreScreen = false;
			boolean executeWatchDog = false;
			boolean stateChanged = false;

			Miscellaneous misc = solvis.getSolvisDescription().getMiscellaneous();
			int unsuccessfullWaitTime = misc.getUnsuccessfullWaitTime_ms();
			int watchDogTime = solvis.getUnit().getWatchDogTime_ms();

			synchronized (this) {
				this.notifyAll();
			}

			while (!this.abort) {
				boolean success;
				Command command = null;
				if (solvis.getSolvisState().getState() == SolvisState.State.SOLVIS_CONNECTED) {
					synchronized (this) {
						if (this.queue.isEmpty() || this.commandDisableCount > 0 || !controlEnable) {
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
						}
					}
					if (this.abort) {
						return;
					}
					success = true;

					try {
						if (command != null && command.getScreen(solvis) == solvis.getCurrentScreen().get()
								&& !solvis.isScreenSaverActive()
								&& solvis.getSolvisState().getState() != SolvisState.State.ERROR | stateChanged) {
							executeWatchDog = true;
						}
						stateChanged = false;
						if (saveScreen) {
							SolvisWorkers.this.solvis.saveScreen();
							saveScreen = false;
						} else if (restoreScreen) {
							SolvisWorkers.this.solvis.restoreScreen();
							restoreScreen = false;
						}
						if (executeWatchDog) {
							watchDog.execute();
							executeWatchDog = false;
						}
						if (command != null && !command.isInhibit()) {
							logger.debug("Command <" + command.toString() + "> will be executed");
							success = execute(command);
							logger.debug("Command <" + command.toString() + "> executed "
									+ (success ? "" : "not " + "successfull"));
						}
					} catch (IOException | ErrorPowerOn e) {
						success = false;
					} catch (TerminationException e3) {
						return;
					} catch (Throwable e4) {
						logger.error("Unknown error detected", e4);
						success = true;
					}
				} else {
					success = false;
					stateChanged = true;
				}
				synchronized (this) {
					boolean remove = success;
					boolean insertAtTheEnd = false;
					if (command != null) {
						if (!success) {
							command.incrementFailCount();
							if (command.getFailCount() >= Constants.COMMAND_TO_QUEUE_END_AFTER_N_FAILURES) {
								remove = true;
								if (command.getFailCount() < Constants.COMMAND_IGNORED_AFTER_N_FAILURES) {
									insertAtTheEnd = true;
								} else {
									logger.error("Command <" + command.toString() + "> couldn't executed. Is ignored.");
								}
							}
						}
						if (remove) {
							boolean deleted = false;
							for (Iterator<Command> it = queue.iterator(); it.hasNext() && !deleted;) {
								Command cmp = it.next();
								if (command == cmp) {
									it.remove();
									deleted = true;
								}
							}
						}
						if (insertAtTheEnd) {
							queue.add(command); // Shift command at the end of the queue on too many errors
						}
					}
					if (!success) {
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

		public void push(Command command) {
			if (!controlEnable) {
				return;
			}
			synchronized (this) {

				boolean add = true;

				ListIterator<Command> itInsert = null;

				if (this.optimizationInhibitCnt == 0) {

					for (ListIterator<Command> it = this.queue.listIterator(this.queue.size()); it.hasPrevious();) {
						Command cmp = it.previous();
						Handling handling = command.getHandling(cmp, solvis);
						if (handling.mustInhibitInQueue()) {
							cmp.setInhibit(true);
							logger.debug(
									"Command <" + cmp.toString() + "> was inhibited.");
						}
						if (handling.isInhibitAppend()) {
							add = false;
						}
						if (handling.mustInsert()) {
							if (itInsert == null) {
								itInsert = this.queue.listIterator(it.nextIndex());
							}
						}
					}
				} else {
					add = true;
				}
				if (add) {
					if (command.first()) {
						this.queue.addFirst(command);
						logger.debug(
								"Command <" + command.toString() + "> was added to the beginning of the command queue.");
					} else if (itInsert != null) {
						itInsert.next();
						itInsert.add(command);
						logger.debug("Command <" + command.toString() + "> was inserted in the command queue.");
					} else {
						this.queue.add(command);
						logger.debug("Command <" + command.toString() + "> was added to the end of the command queue.");
					}
					this.notifyAll();
					watchDog.bufferNotEmpty();
				}
			}
		}

		public synchronized void commandOptimization(boolean enable) {
			if (enable) {
				if (this.optimizationInhibitCnt > 0) {
					--this.optimizationInhibitCnt;
				}
			} else {
				++this.optimizationInhibitCnt;
			}
		}

		public synchronized void screenRestore(boolean enable) {
			if (enable) {
				if (this.screenRestoreInhibitCnt > 0) {
					--this.screenRestoreInhibitCnt;
				}
			} else {
				++this.screenRestoreInhibitCnt;
			}
		}

		public synchronized void commandEnable(boolean enable) {
			if (enable) {
				if (this.commandDisableCount > 0) {
					--this.commandDisableCount;
				}
			} else {
				++this.commandDisableCount;
			}
		}

	}

	private boolean execute(Command command) throws IOException, TerminationException, ErrorPowerOn {
		Screen commandScreen = command.getScreen(this.solvis);
		if (commandScreen != null) {
			long now = System.currentTimeMillis();

			boolean clear;

			if (this.solvis.getCurrentScreen().get() != commandScreen || this.commandScreen != commandScreen) {
				clear = true;
			} else if (now > timeCommandScreen + Constants.TIME_COMMAND_SCREEN_VALID) {
				clear = true;
			} else {
				boolean isIn = this.commandsOfScreen.contains(command);
				if (!isIn) {
					clear = false;
				} else {
					clear = true;
				}
			}
			if (clear) {
				this.solvis.clearCurrentScreen();
				this.commandScreen = commandScreen;
				this.timeCommandScreen = now;
				this.commandsOfScreen.clear();
			}
			this.commandsOfScreen.add(command);
		}

		return command.execute(this.solvis);

	}

	public void commandOptimization(boolean enable) {
		if (enable) {
			this.controlsThread.push(new Command() {

				@Override
				public boolean execute(Solvis solvis) throws IOException, TerminationException, ErrorPowerOn {
					controlsThread.commandOptimization(true);
					return true;
				}

				@Override
				public String toString() {
					return "Optimization enable";
				}

			});
		} else {
			this.controlsThread.commandOptimization(false);
		}
	}

	public void screenRestore(boolean enable) {
		this.controlsThread.screenRestore(enable);

	}

	public void commandEnable(boolean enable) {
		this.controlsThread.commandEnable(enable);

	}

	public void push(Command command) {
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
			int measurementInterval = solvis.getDefaultReadMeasurementsInterval_ms();
			this.nextTime = System.currentTimeMillis();
			while (!abort) {

				try {

					try {
						solvis.measure();
						solvis.getSolvisState().connected();
					} catch (IOException e1) {
					} catch (ErrorPowerOn e2) {
						solvis.getSolvisState().remoteConnected();
					}

					synchronized (this) {
						long now = System.currentTimeMillis();
						this.nextTime = now - (now - this.nextTime) % measurementInterval + measurementInterval;
						int waitTime = (int) (this.nextTime - now);
						try {
							this.wait(waitTime);
						} catch (InterruptedException e) {
						}
					}
				} catch (Throwable e) {
					logger.error("Error was thrown in measurements worker thread. Cause: ", e);
					AbortHelper.getInstance().sleep(Constants.WAIT_TIME_AFTER_THROWABLE);
				}

			}
		}

		public synchronized void abort() {
			this.abort = true;
			this.notifyAll();
		}
	}

	public void serviceReset() {
		this.watchDog.serviceReset();

	}

}
