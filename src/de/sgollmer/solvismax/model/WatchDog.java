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

	private final boolean clearErrorMessageAfterMail;

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
		this.clearErrorMessageAfterMail = solvis.getFeatures().isClearErrorMessageAfterMail();
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
		HUMAN_ACCESS_USER, //
		HUMAN_ACCESS_SERVICE, //
		INIT, //
		POWER_OFF, //
		POWER_ON, //
		TRIGGER_SERVICE_BY_COMMAND, //
		RESET_SERVICE_BY_COMMAND;

		private final boolean error;
		private final boolean errorEvent;

		private Event(final boolean error) {
			this.error = error;
			this.errorEvent = true;
		}

		private Event() {
			this.error = false;
			this.errorEvent = false;
		}

		public boolean isError() {
			return this.error;
		}

		public boolean isErrorEvent() {
			return this.errorEvent;
		}

	}

	void execute() {

		this.abort = false;
		boolean handleErrorSuccessfull = true;

		if (!this.initialized) {
			this.initialized = true;
			try {
				this.humanAccess.processEvent(Event.INIT, null);
			} catch (Throwable e) {
			}
		}

		while (!this.abort) { // loop in case off user access or error detected

			try {

				SynchronizedScreenResult synchronizedScreenResult = this.solvis.getSyncronizedRealScreen();

				SolvisScreen realScreen = SynchronizedScreenResult.getScreen(synchronizedScreenResult);

				SolvisScreen possibleErrorScreen = realScreen;
				Event event = Event.NONE;

				if (realScreen != null && synchronizedScreenResult.isChanged()) {

					SaverEvent saverState = this.saver.getSaverState(realScreen);
					if (saverState == SaverEvent.SCREENSAVER) {
						event = Event.SCREENSAVER;
						possibleErrorScreen = null;
					} else if (saverState != SaverEvent.POSSIBLE) {
						event = this.checkError(realScreen); // RESET_ERROR or SET_ERROR

						if (event == null) {
							event = this.isHumanAccess(realScreen) ? Event.HUMAN_ACCESS_USER : Event.NONE;
						}
					}
				}

				if (event == Event.HUMAN_ACCESS_USER) {
					this.solvis.setCurrentScreen(realScreen);
					if (realScreen.isService() && this.solvis.getFeatures().isDetectServiceAccess()) {
						event = Event.HUMAN_ACCESS_SERVICE;
					}
				}

				if (event.isErrorEvent()) {
					handleErrorSuccessfull = this.solvis.getSolvisState().setError(event, possibleErrorScreen);
				}

				this.humanAccess.processEvent(event, realScreen);

				boolean clearErrorMessage = this.clearErrorMessageAfterMail && handleErrorSuccessfull
						&& event == Event.SET_ERROR_BY_MESSAGE;

				if (clearErrorMessage) {
					try {
						this.solvis.sendBack();
					} catch (TerminationException e) {
						return;
					}
				}

				this.abort = this.humanAccess.isServerAccessEnabled()
						&& (clearErrorMessage || event != Event.SET_ERROR_BY_MESSAGE);

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

	private Event checkError(final SolvisScreen realScreen) {
		Event event = null;
		switch (this.solvis.getSolvisDescription().getErrorDetection().getType(realScreen)) {
			case MESSAGE_BOX:
				event = Event.SET_ERROR_BY_MESSAGE;
				break;
			case ERROR_BUTTON:
				event = Event.SET_ERROR_BY_BUTTON;
				break;
			case HOME_NONE:
				if (this.solvis.getSolvisState().isErrorMessage()) {
					event = Event.RESET_ERROR;
				} else {
					event = null;
				}
				break;
			case NONE:
				event = null;
				break;
		}
		return event;
	}

	private boolean isHumanAccess(final SolvisScreen solvisScreen) throws IOException, TerminationException {
		return !solvisScreen.equalsWoIgnore(WatchDog.this.solvis.getCurrentScreen(false));
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
