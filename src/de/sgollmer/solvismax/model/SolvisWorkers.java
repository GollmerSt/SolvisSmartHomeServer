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

import de.sgollmer.solvismax.Constants;
import de.sgollmer.solvismax.error.ModbusException;
import de.sgollmer.solvismax.error.PowerOnException;
import de.sgollmer.solvismax.error.TerminationException;
import de.sgollmer.solvismax.helper.AbortHelper;
import de.sgollmer.solvismax.log.LogManager;
import de.sgollmer.solvismax.log.LogManager.ILogger;
import de.sgollmer.solvismax.model.Command.Handling;
import de.sgollmer.solvismax.model.objects.Miscellaneous;
import de.sgollmer.solvismax.model.objects.Observer.IObserver;
import de.sgollmer.solvismax.model.objects.data.SolvisData;
import de.sgollmer.solvismax.model.objects.screen.AbstractScreen;
import de.sgollmer.solvismax.model.objects.screen.SolvisScreen;

public class SolvisWorkers {

	private static final ILogger logger = LogManager.getInstance().getLogger(SolvisWorkers.class);

	private final Solvis solvis;
	private final WatchDog watchDog;

	private ControlWorkerThread controlsThread = null;
	private MeasurementsWorkerThread measurementsThread = null;
	private Collection<Command> commandsOfScreen = new ArrayList<>();
	private AbstractScreen commandScreen = null;
	private long timeCommandScreen = System.currentTimeMillis();
	private final boolean controlEnable;

	SolvisWorkers(Solvis solvis) {
		this.solvis = solvis;
		this.watchDog = new WatchDog(solvis, solvis.getSolvisDescription().getSaver());
		this.solvis.registerAbortObserver(new IObserver<Boolean>() {

			@Override
			public void update(Boolean data, Object source) {
				if (data) {
					abort();
				}
			}
		});
		this.controlEnable = !this.solvis.getFeatures().isOnlyMeasurements();

	}

	private class ControlWorkerThread extends Thread {

		private LinkedList<Command> queue = new LinkedList<>();
		private boolean abort = false;
		private int screenRestoreInhibitCnt = 0;
		private int optimizationInhibitCnt = 0;
		private int commandDisableCount = 0;

		private ControlWorkerThread() {
			super("ControlWorkerThread");
		}

		@Override
		public void run() {
			boolean queueWasEmpty = true;
			boolean restoreScreen = false;
			boolean executeWatchDog = true;
			boolean stateChanged = false;

			Miscellaneous misc = SolvisWorkers.this.solvis.getSolvisDescription().getMiscellaneous();
			int unsuccessfullWaitTime = misc.getUnsuccessfullWaitTime_ms();
			int watchDogTime = SolvisWorkers.this.solvis.getUnit().getWatchDogTime_ms();

			synchronized (this) {
				this.notifyAll();
			}

			while (!this.abort) {
				boolean success;
				Command command = null;
				SolvisState.State state = SolvisWorkers.this.solvis.getSolvisState().getState();
				if (state == SolvisState.State.SOLVIS_CONNECTED || state == SolvisState.State.ERROR) {
					synchronized (this) {
						if (this.queue.isEmpty() || this.commandDisableCount > 0 || !SolvisWorkers.this.controlEnable) {
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
							queueWasEmpty = false;
							command = this.queue.peek();
						}
					}
					if (this.abort) {
						return;
					}
					success = true;

					try {
						if (command != null
								&& command.getScreen(SolvisWorkers.this.solvis) == SolvisScreen
										.get(SolvisWorkers.this.solvis.getCurrentScreen())
								&& !SolvisWorkers.this.solvis.getSolvisState().isError() | stateChanged) {
							executeWatchDog = true;
						}
						stateChanged = false;
						if (restoreScreen) {
							SolvisWorkers.this.solvis.restoreScreen();
							restoreScreen = false;
						}
						if (executeWatchDog) {
							SolvisWorkers.this.watchDog.execute();
							executeWatchDog = false;
						}
						if (command == null && SolvisWorkers.this.solvis.getSolvisState().isError()) {
							SolvisWorkers.this.solvis.getHomeScreen().goTo(SolvisWorkers.this.solvis);
						}
						if (command != null && !command.isInhibit()) {
							String commandString = command.toString();
							logger.debug("Command <" + commandString + "> will be executed");
							success = execute(command);
							logger.debug("Command <" + commandString + "> executed "
									+ (success ? "" : "not " + "successfull"));
						}
					} catch (IOException | PowerOnException | ModbusException e) {
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
					if (command != null) {
						if (success) {
							this.removeCommand(command);
						} else {
							if (command.toEndOfQueue()) {
								this.moveToTheEnd(command);
							}
						}
					}
					if (!success) {
						try {
							this.wait(unsuccessfullWaitTime);
							if (SolvisWorkers.this.solvis.getSolvisState().isConnected()) {
								SolvisWorkers.this.watchDog.execute();
								executeWatchDog = false;
							}
						} catch (InterruptedException e) {
						}
					}
				}
			}
		}

		private synchronized void removeCommand(Command command) {
			boolean deleted = false;
			for (Iterator<Command> it = this.queue.iterator(); it.hasNext() && !deleted;) {
				Command cmp = it.next();
				if (command == cmp) {
					it.remove();
					deleted = true;
				}
			}
		}

		private synchronized void moveToTheEnd(Command command) {
			boolean deleted = false;
			Iterator<Command> it = this.queue.iterator();
			while (it.hasNext() && !deleted) {
				Command cmp = it.next();
				if (command == cmp) {
					it.remove();
					deleted = true;
				}
			}
			if (command.isInhibit()) {
				return;
			}
			while (it.hasNext()) {
				Command cmp = it.next();
				if (command.canBeIgnored(cmp)) {
					return;
				}
			}
			this.queue.add(command);
		}

		private synchronized void abort() {
			this.abort = true;
			this.notifyAll();
		}

		private void push(Command command) {
			if (!SolvisWorkers.this.controlEnable) {
				return;
			}
			synchronized (this) {

				boolean add = true;

				ListIterator<Command> itInsert = null;

				if (this.optimizationInhibitCnt == 0) {

					for (ListIterator<Command> it = this.queue.listIterator(this.queue.size()); it.hasPrevious();) {
						Command cmp = it.previous();
						Handling handling = command.getHandling(cmp, SolvisWorkers.this.solvis);
						if (handling.mustInhibitInQueue()) {
							cmp.setInhibit(true);
							logger.debug("Command <" + cmp.toString() + "> was inhibited.");
						}
						if (handling.isInhibitAdd()) {
							add = false;
						}
						if (handling.mustInsert()) {
							if (itInsert == null) {
								itInsert = this.queue.listIterator(it.nextIndex());
							}
						}
						if (handling.mustFinished()) {
							break;
						}
					}
				} else {
					add = true;
				}
				if (add) {
					if (command.first()) {
						this.queue.addFirst(command);
						logger.debug("Command <" + command.toString()
								+ "> was added to the beginning of the command queue.");
					} else if (itInsert != null) {
						itInsert.next();
						itInsert.add(command);
						logger.debug("Command <" + command.toString() + "> was inserted in the command queue.");
					} else {
						this.queue.add(command);
						logger.debug("Command <" + command.toString() + "> was added to the end of the command queue.");
					}
					this.notifyAll();
					SolvisWorkers.this.watchDog.bufferNotEmpty();
				}
			}
		}

		private synchronized void commandOptimization(boolean enable) {
			if (enable) {
				if (this.optimizationInhibitCnt > 0) {
					--this.optimizationInhibitCnt;
				}
			} else {
				++this.optimizationInhibitCnt;
			}
		}

		private synchronized void screenRestore(boolean enable) {
			if (enable) {
				if (this.screenRestoreInhibitCnt > 0) {
					--this.screenRestoreInhibitCnt;
				}
			} else {
				++this.screenRestoreInhibitCnt;
			}
		}

		private synchronized void commandEnable(boolean enable) {
			if (enable) {
				if (this.commandDisableCount > 0) {
					--this.commandDisableCount;
				}
			} else {
				++this.commandDisableCount;
			}
		}

		public synchronized boolean willBeModified(SolvisData data) {
			for (ListIterator<Command> it = this.queue.listIterator(this.queue.size()); it.hasPrevious();) {
				Command command = it.previous();
				if (command instanceof CommandControl) {
					CommandControl control = (CommandControl) command;
					if (control.isWriting() && control.getDescription() == data.getDescription()
							&& !control.getSetValue().equals(data.getSingleData())) {
						return true;
					}
				}
			}
			return false;
		}

	}

	private boolean execute(Command command)
			throws IOException, TerminationException, PowerOnException, ModbusException {
		if (!command.isModbus() || command.isWriting()) {
			AbstractScreen commandScreen = command.getScreen(this.solvis);
			if (commandScreen != null) {
				long now = System.currentTimeMillis();

				boolean clear;

				if (SolvisScreen.get(this.solvis.getCurrentScreen()) != commandScreen
						|| this.commandScreen != commandScreen) {
					clear = true;
				} else if (now > this.timeCommandScreen + Constants.TIME_COMMAND_SCREEN_VALID) {
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
		} else {
			return true;
		}
	}

	void commandOptimization(boolean enable) {
		if (enable) {
			this.controlsThread.push(new Command() {

				@Override
				public boolean execute(Solvis solvis) throws IOException, TerminationException, PowerOnException {
					SolvisWorkers.this.controlsThread.commandOptimization(true);
					return true;
				}

				@Override
				public String toString() {
					return "Optimization enable";
				}

				@Override
				public boolean isModbus() {
					return false;
				}

				@Override
				public boolean isWriting() {
					return false;
				}

				@Override
				public void notExecuted() {
				}

			});
		} else {
			this.controlsThread.commandOptimization(false);
		}
	}

	void screenRestore(boolean enable) {
		this.controlsThread.screenRestore(enable);

	}

	void commandEnable(boolean enable) {
		this.controlsThread.commandEnable(enable);

	}

	void push(Command command) {
		if (this.controlsThread == null) {
			return;
		}
		this.controlsThread.push(command);
	}

	private void abort() {
		synchronized (this) {
			if (this.controlsThread != null) {
				this.controlsThread.abort();
				this.controlsThread = null;
			}
			if (this.measurementsThread != null) {
				this.measurementsThread.abort();
				this.measurementsThread = null;
			}
		}
	}

	void init() {
		synchronized (this) {
			if (this.controlsThread == null) {
				this.controlsThread = new ControlWorkerThread();
			}
			if (this.measurementsThread == null) {
				this.measurementsThread = new MeasurementsWorkerThread();
			}
		}
	}

	void start() {
		synchronized (this) {
			if (this.controlsThread != null && !this.controlsThread.isAlive())
				synchronized (this.controlsThread) {
					try {
						this.controlsThread.start();
						this.controlsThread.wait(); // in case of
					} catch (InterruptedException e) {
					}
				}

			if (this.measurementsThread != null && !this.measurementsThread.isAlive()) {
				this.measurementsThread.start();
			}
		}
	}

	private class MeasurementsWorkerThread extends Thread {

		private boolean abort = false;
		private long nextTime;

		private MeasurementsWorkerThread() {
			super("MeasurementsWorkerThread");

		}

		@Override
		public void run() {
			int measurementInterval = SolvisWorkers.this.solvis.getDefaultReadMeasurementsInterval_ms();
			this.nextTime = System.currentTimeMillis();
			while (!this.abort) {

				try {

					try {
						SolvisWorkers.this.solvis.measure();
						SolvisWorkers.this.solvis.getSolvisState().connected();
					} catch (IOException e1) {
					} catch (PowerOnException e2) {
						SolvisWorkers.this.solvis.getSolvisState().remoteConnected();
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
					try {
						AbortHelper.getInstance().sleep(Constants.WAIT_TIME_AFTER_THROWABLE);
					} catch (TerminationException e1) {
						return;
					}
				}

			}
		}

		private synchronized void abort() {
			this.abort = true;
			this.notifyAll();
		}
	}

	void serviceReset() {
		this.watchDog.serviceReset();

	}

	public boolean willBeModified(SolvisData data) {
		return this.controlsThread.willBeModified(data);
	}

}
