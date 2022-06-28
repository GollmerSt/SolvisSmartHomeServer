/************************************************************************
 * 
 * $Id$
 *
 * 
 ************************************************************************/

package de.sgollmer.solvismax.model;

import java.io.IOException;

import de.sgollmer.solvismax.Constants;
import de.sgollmer.solvismax.connection.transfer.SolvisStatePackage;
import de.sgollmer.solvismax.error.TerminationException;
import de.sgollmer.solvismax.log.LogManager;
import de.sgollmer.solvismax.log.LogManager.ILogger;
import de.sgollmer.solvismax.model.HumanAccess.Status;
import de.sgollmer.solvismax.model.Solvis.SynchronizedScreenResult;
import de.sgollmer.solvismax.model.objects.Observer.IObserver;
import de.sgollmer.solvismax.model.objects.screen.ScreenSaver;
import de.sgollmer.solvismax.model.objects.screen.ScreenSaver.SaverEvent;
import de.sgollmer.solvismax.model.objects.screen.SolvisScreen;

public class WatchDog {

	@SuppressWarnings("unused")
	private static final ILogger logger = LogManager.getInstance().getLogger(WatchDog.class);

	private final Solvis solvis;
	private final ScreenSaver.Exec saver;

	private final int watchDogTime;
	private boolean abort = false;
	private boolean waiting = false;
	private final HumanAccess humanAccess;
	private boolean initialized = false;
	private SolvisStateObserver solvisStateObserver = new SolvisStateObserver();

	private class SolvisStateObserver implements IObserver<SolvisStatePackage> {

		private SolvisStatus lastState = SolvisStatus.UNDEFINED;

		@Override
		public void update(final SolvisStatePackage data, final Object source) {

			SolvisStatus state = data.getState();

			synchronized (WatchDog.this) {
				try {
					switch (state) {
						case REMOTE_CONNECTED:
							if (this.lastState == SolvisStatus.SOLVIS_CONNECTED
									|| this.lastState == SolvisStatus.ERROR) {
								WatchDog.this.humanAccess.processEvent(Event.POWER_OFF, null);
							}
							break;
						case POWER_OFF:
							WatchDog.this.humanAccess.processEvent(Event.POWER_OFF, null);
							break;
						case SOLVIS_CONNECTED:
							WatchDog.this.humanAccess.processEvent(Event.POWER_ON, null);
							break;
					}
				} catch (IOException | TerminationException e) {
				}
			}
			this.lastState = state;
		}

	}

	WatchDog(final Solvis solvis, final ScreenSaver saver) {
		this.solvis = solvis;
		this.humanAccess = solvis.getHumanAccess();
		solvis.getFeatures().isClearErrorMessageAfterMail();
		this.saver = saver.createExecutable(solvis);
		this.watchDogTime = this.solvis.getUnit().getWatchDogTime_ms();
		this.solvis.registerAbortObserver(new IObserver<Boolean>() {

			@Override
			public void update(Boolean data, Object source) {
				if (data) {
					abort();
				}

			}
		});
		if (this.solvis.getFeatures().isPowerOffIsServiceAccess()
				&& this.solvis.getFeatures().isDetectServiceAccess()) {
			this.solvis.getSolvisState().register(this.solvisStateObserver);
		}
	}

	public enum Event {
		SCREENSAVER, //
		SET_ERROR_BY_BUTTON(true), //
		SET_ERROR_BY_MESSAGE(true), //
		RESET_ERROR(false), //
		NONE, //
		HUMAN_ACCESS_USER(HumanAccess.Status.USER), //
		HUMAN_ACCESS_SERVICE(HumanAccess.Status.SERVICE), //
		INIT, //
		POWER_OFF, //
		POWER_ON, //
		TRIGGER_SERVICE_BY_COMMAND, //
		RESET_SERVICE_BY_COMMAND;

		private final boolean error;
		private final boolean errorEvent;
		private final HumanAccess.Status humanAccess;

		private Event(final boolean error) {
			this.error = error;
			this.errorEvent = true;
			this.humanAccess = Status.NONE;
		}

		private Event(final HumanAccess.Status status) {
			this.error = false;
			this.errorEvent = false;
			this.humanAccess = status;
		}

		private Event() {
			this.error = false;
			this.errorEvent = false;
			this.humanAccess = Status.NONE;
		}

		public boolean isError() {
			return this.error;
		}

		public boolean isErrorEvent() {
			return this.errorEvent;
		}

		public boolean isHumanAccess() {
			return this.humanAccess != Status.NONE;
		}

	}

	void execute() {

		this.abort = false;

		if (!this.initialized) {
			this.initialized = true;
			try {
				this.humanAccess.processEvent(Event.INIT, null);
			} catch (Throwable e) {
			}
		}

		while (!this.abort) { // loop in case off user access or error detected

			boolean commandExecutionEnabled = true;

			try {

				SynchronizedScreenResult synchronizedScreenResult = this.solvis.getSyncronizedRealScreen();

				SolvisScreen realScreen = SynchronizedScreenResult.getScreen(synchronizedScreenResult);

				Event event = Event.NONE;

				if (realScreen != null && synchronizedScreenResult.isChanged()) {

					SaverEvent saverState = this.saver.getSaverState(realScreen);
					if (saverState == SaverEvent.SCREENSAVER) {

						event = Event.SCREENSAVER;

					} else {

						Boolean enabled = this.solvis.getSolvisState().handleError(realScreen);

						commandExecutionEnabled &= enabled == null ? true : enabled;

						if (enabled == null && saverState != SaverEvent.POSSIBLE) {

							event = this.humanAccess.getEvent(realScreen);
						}
					}
				}

				if (event.isHumanAccess()) {

					this.solvis.setCurrentScreen(realScreen);
				}

				this.humanAccess.processEvent(event, realScreen);

				this.abort = this.humanAccess.isServerAccessEnabled() && commandExecutionEnabled;

				synchronized (this) {
					if (!this.abort) {
						try {
							this.waiting = true;
							this.wait(this.watchDogTime);
							this.waiting = false;
						} catch (InterruptedException e) {
						}
					}
				}

			} catch (IOException e) {
				synchronized (this) {
					if (!this.abort) {
						try {
							this.waiting = true;
							this.wait(Constants.WAIT_TIME_AFTER_IO_ERROR);
							this.waiting = false;
						} catch (InterruptedException e1) {
						}
					}
				}
			} catch (TerminationException e) {
				return;
			}
		}

	}

	private synchronized void abort() {
		this.abort = true;
		this.notifyAll();
	}

	synchronized void bufferNotEmpty() {
		if (this.waiting) {
			this.notifyAll();
		}
	}

}
