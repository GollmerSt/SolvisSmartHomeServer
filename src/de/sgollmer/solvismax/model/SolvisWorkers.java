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
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.ListIterator;
import java.util.Set;

import de.sgollmer.solvismax.Constants;
import de.sgollmer.solvismax.error.PowerOnException;
import de.sgollmer.solvismax.error.TerminationException;
import de.sgollmer.solvismax.error.TypeException;
import de.sgollmer.solvismax.helper.AbortHelper;
import de.sgollmer.solvismax.helper.Helper;
import de.sgollmer.solvismax.helper.Helper.Times;
import de.sgollmer.solvismax.log.LogManager;
import de.sgollmer.solvismax.log.LogManager.ILogger;
import de.sgollmer.solvismax.model.Command.Handling;
import de.sgollmer.solvismax.model.objects.AllChannelDescriptions.MeasureMode;
import de.sgollmer.solvismax.model.objects.ChannelDescription;
import de.sgollmer.solvismax.model.objects.Miscellaneous;
import de.sgollmer.solvismax.model.objects.Observer.IObserver;
import de.sgollmer.solvismax.model.objects.ResultStatus;
import de.sgollmer.solvismax.model.objects.data.SolvisData;
import de.sgollmer.solvismax.model.objects.screen.AbstractScreen;
import de.sgollmer.solvismax.model.objects.screen.SolvisScreen;
import de.sgollmer.xmllibrary.XmlException;

public class SolvisWorkers {

	private static final ILogger logger = LogManager.getInstance().getLogger(SolvisWorkers.class);

	private final Solvis solvis;
	private final WatchDog watchDog;

	private ControlWorkerThread controlsThread = null;
	private MeasurementsWorkerThread measurementsThread = null;
	private Collection<Command> commandsOfScreen = new ArrayList<>();
	private AbstractScreen commandScreen = null;
	private long timeCommandScreen = System.currentTimeMillis();

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

	}

	public void controlEnable(boolean enable) {
		SolvisWorkers.this.controlsThread.controlEnable(enable);
	}

	private class ControlWorkerThread extends Thread {

		private final LinkedList<Command> queue = new LinkedList<>();
		private final Collection<ChannelDescription> channelsOfQueueRead = new ArrayList<>();
		private boolean abort = false;
		private Set<Object> inhibitScreenResoreServices = new HashSet<>();
		private int optimizationInhibitCnt = 0;
		private boolean controlEnabled = true;
		private boolean running = false;

		private ControlWorkerThread() {
			super("ControlWorkerThread");
		}

		@Override
		public void run() {
			boolean queueWasEmpty = true;
			int restoreScreenCnt = 0;
			boolean executeWatchDog = true;
			boolean stateChanged = false;

			Miscellaneous misc = SolvisWorkers.this.solvis.getSolvisDescription().getMiscellaneous();
			int unsuccessfullWaitTime = misc.getUnsuccessfullWaitTime_ms();
			int watchDogTime = SolvisWorkers.this.solvis.getUnit().getWatchDogTime_ms();

			synchronized (this) {
				this.running = true;
				this.notifyAll();
			}

			while (!this.abort) {
				ResultStatus status;
				Command command = null;
				SolvisStatus state = SolvisWorkers.this.solvis.getSolvisState().getState();
				if (state == SolvisStatus.SOLVIS_CONNECTED || state == SolvisStatus.ERROR) {
					synchronized (this) {
						if (this.queue.isEmpty() || !this.controlEnabled
								|| !SolvisWorkers.this.solvis.getFeatures().isInteractiveGUIAccess()) {
							this.channelsOfQueueRead.clear();
							if (!queueWasEmpty && isScreenRestoreEnabled()) {
								restoreScreenCnt = 2;
							} else if (restoreScreenCnt == 0) {
								try {
									this.wait(watchDogTime);
								} catch (InterruptedException e) {
								}
								executeWatchDog = true;
							}
							queueWasEmpty = true;

						} else {
							queueWasEmpty = false;
							restoreScreenCnt = 0;
							command = this.queue.peek();
						}
					}
					if (this.abort) {
						return;
					}
					status = ResultStatus.SUCCESS;

					try {
						if (command != null
								&& command.getScreen(SolvisWorkers.this.solvis) == SolvisScreen
										.get(SolvisWorkers.this.solvis.getCurrentScreen())
								&& !SolvisWorkers.this.solvis.getSolvisState().isError() | stateChanged) {
							executeWatchDog = true;
						}
						stateChanged = false;
						if (restoreScreenCnt > 0) {
							if (restoreScreenCnt == 2) {
								synchronized (this) {
									try {
										this.wait(Constants.WAIT_TIME_AFTER_QUEUE_EMPTY);
									} catch (InterruptedException e) {
									}
								}
							} else {
								SolvisWorkers.this.solvis.restoreScreen();
							}
							--restoreScreenCnt;
						}
						if (executeWatchDog) {
							SolvisWorkers.this.watchDog.execute();
							executeWatchDog = false;
						}
						if (command == null && SolvisWorkers.this.solvis.getSolvisState().isError()) {
							SolvisWorkers.this.solvis.getHomeScreen().goTo(SolvisWorkers.this.solvis);
						}
						if (command != null && !command.isInhibit()) {
							ExecutedCommand executedCommand = this.processCommand(command);
							command = executedCommand.command;
							status = executedCommand.status;
						}
					} catch (IOException | PowerOnException e) {
						status = ResultStatus.NO_SUCCESS;
					} catch (TerminationException e3) {
						return;
					} catch (Throwable e4) {
						logger.error("Unknown error detected", e4);
						status = ResultStatus.SUCCESS;
					}
				} else {
					status = ResultStatus.NO_SUCCESS;
					stateChanged = true;
				}
				if (command != null) {
					synchronized (this) {
						if (status == ResultStatus.SUCCESS) {
							this.removeCommand(command);
						} else {
							if (command.toEndOfQueue()) {
								this.moveToTheEnd(command);
							}
						}
					}
				}
				if (status == ResultStatus.NO_SUCCESS) {
					try {
						synchronized (this) {
							this.wait(unsuccessfullWaitTime);
						}
						if (SolvisWorkers.this.solvis.getSolvisState().isConnected()) {
							SolvisWorkers.this.watchDog.execute();
							executeWatchDog = false;
						}
					} catch (InterruptedException e) {
					}
				}
			}
		}

		private class ExecutedCommand {
			private final Command command;
			private final ResultStatus status;

			public ExecutedCommand(Command command, ResultStatus status) {
				this.command = command;
				this.status = status;
			}
		}

		private ExecutedCommand processCommand(Command command) throws IOException, TerminationException,
				PowerOnException, NumberFormatException, TypeException, XmlException {
			Command executeCommand = command;

			String commandString = executeCommand.toString();
			logger.debug("Command <" + commandString + "> will be executed");
			ResultStatus status = execute(executeCommand);

			if (status == ResultStatus.INTERRUPTED) {
				logger.debug("Command <" + commandString + "> was interrupted. will be continued.");
			} else {

				logger.debug("Command <" + commandString + "> executed "
						+ (status == ResultStatus.NO_SUCCESS ? "not " : "") + "successfull");
			}
			return new ExecutedCommand(executeCommand, status);
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
			if (!SolvisWorkers.this.solvis.getFeatures().isInteractiveGUIAccess()) {
				return;
			}
			synchronized (this) {

				boolean add = true;

				ListIterator<Command> itInsert = null;

				if (this.optimizationInhibitCnt == 0) {

					for (ListIterator<Command> it = this.queue.listIterator(this.queue.size()); it.hasPrevious();) {
						Command cmp = it.previous();
						Handling handling = command.handle(cmp, SolvisWorkers.this.solvis);
						if (handling.isInhibitedInQueue()) {
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

		private synchronized void screenRestore(boolean enable, Object service) {
			if (!enable) {
				this.inhibitScreenResoreServices.add(service);
			} else {
				this.inhibitScreenResoreServices.remove(service);
			}
		}

		private boolean isScreenRestoreEnabled() {
			return this.inhibitScreenResoreServices.size() == 0;
		}

		private synchronized void controlEnable(boolean enable) {
			this.controlEnabled = enable;
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

	private ResultStatus execute(Command command) throws IOException, TerminationException, PowerOnException,
			NumberFormatException, TypeException, XmlException {
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
	}

	void commandOptimization(boolean enable) {
		if (enable) {
			this.controlsThread.push(new Command() {

				@Override
				public ResultStatus execute(Solvis solvis) throws IOException, TerminationException, PowerOnException {
					SolvisWorkers.this.controlsThread.commandOptimization(true);
					return ResultStatus.SUCCESS;
				}

				@Override
				public String toString() {
					return "Optimization enable";
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

	void screenRestore(boolean enable, Object service) {
		this.controlsThread.screenRestore(enable, service);

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
						while (!this.controlsThread.running) {
							this.controlsThread.wait();
						}
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
		private long nextFast = -1;
		private long nextStd = -1;
		private final int measurementInterval;
		private final int measurementIntervalFast;

		private MeasurementsWorkerThread() {
			super("MeasurementsWorkerThread");
			this.measurementInterval = SolvisWorkers.this.solvis.getUnit().getMeasurementsInterval_ms();
			this.measurementIntervalFast = SolvisWorkers.this.solvis.getUnit().getMeasurementsIntervalFast_ms();
		}

		private int calculateNextMeasurementTimes() {
			Times times = Helper.getStartOfDay();
			long now = times.getNow();
			long firstOfDay = times.getStartOfDay() + 500;
			this.nextStd = firstOfDay + ((now - firstOfDay) / this.measurementInterval + 1) * this.measurementInterval;
			this.nextFast = firstOfDay
					+ ((now - firstOfDay) / this.measurementIntervalFast + 1) * this.measurementIntervalFast;
			long next = Math.min(this.nextStd, this.nextFast);
			return (int) (next - now);
		}

		@Override
		public void run() {

			calculateNextMeasurementTimes();

			while (!this.abort) {

				try {
					try {
						MeasureMode mode;
						
						long now = System.currentTimeMillis() ;
						
						boolean std = now >= this.nextStd;
						boolean fast = now >= this.nextFast;
						
						if ( std&&fast ) {
							mode = MeasureMode.ALL;
						} else if ( fast ) {
							mode = MeasureMode.FAST;
						} else if (std) {
							mode = MeasureMode.STANDARD;
						} else {
							mode = null ;
						}
						
						if (mode != null) {
							SolvisWorkers.this.solvis.measure(mode);
						}

					} catch (IOException e1) {
					} catch (PowerOnException e2) {
					}

					int waitTime = this.calculateNextMeasurementTimes();

					synchronized (this) {

						if (this.abort) {
							break;
						}

						try {
							this.wait(waitTime);
						} catch (InterruptedException e) {
						}
					}
				} catch (Throwable e) {
					if (e instanceof TerminationException) {
						return;
					}
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
