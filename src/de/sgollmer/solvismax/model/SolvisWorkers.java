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
import de.sgollmer.solvismax.error.PowerOnException;
import de.sgollmer.solvismax.error.SolvisErrorException;
import de.sgollmer.solvismax.error.TerminationException;
import de.sgollmer.solvismax.error.TypeException;
import de.sgollmer.solvismax.helper.AbortHelper;
import de.sgollmer.solvismax.helper.Helper;
import de.sgollmer.solvismax.helper.Helper.Times;
import de.sgollmer.solvismax.log.LogManager;
import de.sgollmer.solvismax.log.LogManager.ILogger;
import de.sgollmer.solvismax.model.command.Command;
import de.sgollmer.solvismax.model.command.CommandControl;
import de.sgollmer.solvismax.model.command.CommandObserver;
import de.sgollmer.solvismax.model.command.Handling;
import de.sgollmer.solvismax.model.objects.AllChannelDescriptions.MeasureMode;
import de.sgollmer.solvismax.model.objects.ChannelDescription;
import de.sgollmer.solvismax.model.objects.ErrorState;
import de.sgollmer.solvismax.model.objects.Miscellaneous;
import de.sgollmer.solvismax.model.objects.Observer.IObserver;
import de.sgollmer.solvismax.model.objects.Observer.Observable;
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
	private Observable<Boolean> executingControlObserver = new Observable<>();
	private CommandObserver commandObserver = new CommandObserver();

	SolvisWorkers(final Solvis solvis) {
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

	public void registerControlExecutingObserver(final IObserver<Boolean> executingObserver) {
		this.executingControlObserver.register(executingObserver);
	}

	private class ControlWorkerThread extends Thread {

		private final LinkedList<Command> queue = new LinkedList<>();
		private final Collection<ChannelDescription> channelsOfQueueRead = new ArrayList<>();
		private boolean abort = false;
		private int optimizationInhibitCnt = 0;
		private boolean running = false;
		private Handling.QueueStatus queueStatus = new Handling.QueueStatus();
		private int writeCnt = 0;
		private int readCnt = 0;
		private long nextWatchDogTime = 0L;
		private boolean forceRestoreScreen = false;
		private boolean messageErrorVisible = false;

		private ControlWorkerThread() {
			super("ControlWorkerThread");

			SolvisWorkers.this.solvis.registerSolvisErrorObserver(new IObserver<ErrorState.Info>() {

				private boolean messageError = false;

				@Override
				public void update(ErrorState.Info data, Object source) {

					synchronized (ControlWorkerThread.this) {

						SolvisState solvisState = SolvisWorkers.this.solvis.getSolvisState();

						boolean error = solvisState.isMessageError();
						boolean messageErrorVisible = solvisState.isMessageErrorVisible();

						ControlWorkerThread.this.forceRestoreScreen |= //
								this.messageError && !error
										|| ControlWorkerThread.this.messageErrorVisible && !messageErrorVisible;
						this.messageError = error;

						ControlWorkerThread.this.messageErrorVisible = messageErrorVisible;
					}
				}
			});

		}

		@Override
		public void run() {
			boolean queueWasEmpty = true;

			boolean firstDelayedCircle = false;
			boolean stateChanged = false;

			boolean executeRestoreScreen = false;
			this.nextWatchDogTime = 0L;

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
				int waitTime = 0;

				SolvisStatus state = SolvisWorkers.this.solvis.getSolvisState().getState();

				if (state == SolvisStatus.SOLVIS_CONNECTED || state == SolvisStatus.ERROR) {
					synchronized (this) {

						boolean queueInhibit = !SolvisWorkers.this.solvis.isControlEnabled()
								|| this.messageErrorVisible;

						if (this.queue.isEmpty()) {
							this.handleCommandAddedRemoved(null, false);
						}

						if (this.queue.isEmpty()) {
							this.channelsOfQueueRead.clear();
							if (this.forceRestoreScreen) {
								executeRestoreScreen = true;
								this.forceRestoreScreen = false;
							}
							if (!queueWasEmpty) {
								firstDelayedCircle = true;
								waitTime = Constants.CYCLE_TIME_WHERE_QUEUE_EMPTY;
								queueWasEmpty = true;
							} else if (firstDelayedCircle) {
								firstDelayedCircle = false;
								executeRestoreScreen = true;
								waitTime = 0;
							} else {
								waitTime = Constants.CYCLE_TIME_WHERE_QUEUE_EMPTY;
							}

						} else {
							firstDelayedCircle = false;
							this.forceRestoreScreen = false;
							queueWasEmpty = false;

							if (queueInhibit) {

								waitTime = Constants.CYCLE_TIME_WHERE_QUEUE_EMPTY;

							} else {

								command = this.queue.peek();
							}
						}
					}

					if (this.abort) {
						return;
					}
					status = ResultStatus.SUCCESS;

					try {
						if (command != null
								&& command.getScreen(SolvisWorkers.this.solvis) == SolvisScreen
										.get(SolvisWorkers.this.solvis.getCurrentScreen(false))
								&& !SolvisWorkers.this.solvis.getSolvisState().isMessageError() | stateChanged) {
							setExecuteWatchDog();
						}
						stateChanged = false;
						if (executeRestoreScreen) {
							executeRestoreScreen &= !SolvisWorkers.this.solvis.restoreScreen();
						}

						long time = System.currentTimeMillis();
						if (time > this.nextWatchDogTime) {
							SolvisWorkers.this.watchDog.execute();
							this.nextWatchDogTime = time + watchDogTime;
						}

						if (command != null && !command.isInhibit()) {
							SolvisWorkers.this.executingControlObserver.notify(true);
							try {
								status = this.processCommand(command);
							} catch (IOException | PowerOnException | SolvisErrorException e) {
								throw e;
							} finally {
								SolvisWorkers.this.executingControlObserver.notify(false);
							}
						}
					} catch (IOException | PowerOnException | SolvisErrorException e) {
						status = ResultStatus.NO_SUCCESS;
					} catch (TerminationException e3) {
						return;
					} catch (Throwable e4) {
						logger.error("Unknown error detected", e4);
						waitTime = unsuccessfullWaitTime;
						this.setExecuteWatchDog();
						status = ResultStatus.SUCCESS;
					}
				} else {
					status = ResultStatus.NO_SUCCESS;
					stateChanged = true;
				}
				if (command != null) {
					synchronized (this) {
						if (status.removeFromQueue()) {
							this.handleCommandAddedRemoved(command, false);

							this.removeCommand(command);
						} else {
							if (command.toEndOfQueue()) {
								this.moveToTheEnd(command);
							}
						}
					}
				}
				if (status == ResultStatus.NO_SUCCESS) {
					waitTime = unsuccessfullWaitTime;
					this.setExecuteWatchDog();
				}
				if (waitTime > 0) {
					try {
						synchronized (this) {
							this.wait(waitTime);
						}
					} catch (InterruptedException e) {
					}
				}
			}
		}

		private void setExecuteWatchDog() {
			this.nextWatchDogTime = 0L;
		}

		private final ResultStatus processCommand(final Command command) throws IOException, TerminationException,
				PowerOnException, NumberFormatException, TypeException, XmlException, SolvisErrorException {

			String commandString = command.toString();
			logger.debug("Command <" + commandString + "> will be executed");
			ResultStatus status = execute(command);

			if (status == ResultStatus.INTERRUPTED) {
				logger.debug("Command <" + commandString + "> was interrupted. will be continued.");
			} else {

				logger.debug("Command <" + commandString + "> executed "
						+ (status == ResultStatus.NO_SUCCESS ? "not " : "") + "successfull");
			}
			return status;
		}

		private synchronized void removeCommand(final Command command) {
			boolean deleted = false;
			for (Iterator<Command> it = this.queue.iterator(); it.hasNext() && !deleted;) {
				Command cmp = it.next();
				if (command == cmp) {
					it.remove();
					deleted = true;
				}
			}
		}

		private synchronized void moveToTheEnd(final Command command) {
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

		private void push(final Command command) {
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
					this.handleCommandAddedRemoved(command, true);

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

		private void handleCommandAddedRemoved(final Command command, final boolean addedToQueue) {
			if (command == null) {
				if (this.readCnt != 0 || this.writeCnt != 0) {
					logger.error("Programing error: Queue is empty, but readCnt/writeCnt !=0.");
					this.writeCnt = 0;
					this.readCnt = 0;
					SolvisWorkers.this.commandObserver.notify(SolvisStatus.CONTROL_FINISHED);
				}
				return;
			}

			boolean write = false;

			switch (command.getType()) {
				case OTHER:
				case CONTROL_UPDATE:
					return;
				case CONTROL_READ:
					if (addedToQueue) {
						++this.readCnt;
					} else {
						--this.readCnt;
					}
					break;
				case CONTROL_WRITE:
					if (addedToQueue) {
						++this.writeCnt;
					} else {
						--this.writeCnt;
					}
					write = true;
					break;
				default:
					return;
			}
			if (addedToQueue) {
				if (write) {
					if (this.writeCnt == 1) {
						updateByMonitoringTask(CommandObserver.Status.QUEUE_WRITE, this);
					}
				} else {
					if (this.writeCnt == 0 && this.readCnt == 1) {
						updateByMonitoringTask(CommandObserver.Status.QUEUE_READ, this);
					}
				}

			} else {
				if (this.writeCnt == 0 && this.readCnt == 0) {
					updateByMonitoringTask(CommandObserver.Status.QUEUE_FINISHED, this);
				} else if (write && this.writeCnt == 0) {
					updateByMonitoringTask(CommandObserver.Status.QUEUE_READ, this);
				}
			}
		}

		private synchronized void commandOptimization(final boolean enable) {
			if (enable) {
				if (this.optimizationInhibitCnt > 0) {
					--this.optimizationInhibitCnt;
				}
			} else {
				++this.optimizationInhibitCnt;
			}
		}

		public synchronized boolean willBeModified(final SolvisData data) {
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

	private ResultStatus execute(final Command command) throws IOException, TerminationException, PowerOnException,
			NumberFormatException, TypeException, XmlException, SolvisErrorException {

		ResultStatus resultStatus = command.preExecute(this.solvis, this.controlsThread.queueStatus);

		if (resultStatus != null) {
			return resultStatus;
		}

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

		return command.execute(this.solvis, this.controlsThread.queueStatus);
	}

	void commandOptimization(final boolean enable) {
		if (enable) {
			this.controlsThread.push(new Command() {

				@Override
				public ResultStatus execute(Solvis solvis, Handling.QueueStatus queueStatus)
						throws IOException, TerminationException, PowerOnException {
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

	void push(final Command command) {
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

						long now = System.currentTimeMillis();

						boolean std = now >= this.nextStd;
						boolean fast = now >= this.nextFast;

						if (std && fast) {
							mode = MeasureMode.ALL;
						} else if (fast) {
							mode = MeasureMode.FAST;
						} else if (std) {
							mode = MeasureMode.STANDARD;
						} else {
							mode = null;
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

	public boolean willBeModified(final SolvisData data) {
		return this.controlsThread.willBeModified(data);
	}

	public void registerAllSettingsDoneObserver(final IObserver<SolvisStatus> observer) {
		this.commandObserver.register(observer);

	}

	public SolvisStatus getSettingStatus() {
		if (this.controlsThread == null) {
			return SolvisStatus.CONTROL_FINISHED;
		} else if (this.controlsThread.writeCnt != 0) {
			return SolvisStatus.CONTROL_WRITE_ONGOING;
		} else if (this.controlsThread.readCnt != 0) {
			return SolvisStatus.CONTROL_READ_ONGOING;
		} else {
			return SolvisStatus.CONTROL_FINISHED;
		}
	}

	public void updateByMonitoringTask(final CommandObserver.Status status, final Object source) {
		this.commandObserver.update(status, source);
	}

}
