/************************************************************************
 * 
 * $Id$
 *
 * 
 ************************************************************************/

package de.sgollmer.solvismax.model;

import java.io.IOException;

import de.sgollmer.solvismax.BaseData;
import de.sgollmer.solvismax.Constants;
import de.sgollmer.solvismax.Constants.Debug;
import de.sgollmer.solvismax.connection.transfer.SolvisStatePackage;
import de.sgollmer.solvismax.error.TerminationException;
import de.sgollmer.solvismax.log.LogManager;
import de.sgollmer.solvismax.log.LogManager.ILogger;
import de.sgollmer.solvismax.model.Solvis.SynchronizedScreenResult;
import de.sgollmer.solvismax.model.objects.Observer.IObserver;
import de.sgollmer.solvismax.model.objects.screen.ScreenSaver;
import de.sgollmer.solvismax.model.objects.screen.ScreenSaver.SaverEvent;
import de.sgollmer.solvismax.model.objects.screen.SolvisScreen;
import de.sgollmer.solvismax.model.objects.unit.Unit;

public class WatchDog {

	private static final ILogger logger = LogManager.getInstance().getLogger(WatchDog.class);

	private static final String XML_END_OF_USER_BY_SCREEN_SAVER = "EndOfUserInterventionDetectionThroughScreenSaver";

	private final Solvis solvis;
	private final ScreenSaver.Exec saver;

	private final int releaseBlockingAfterUserChange_ms;
	private final int releaseBlockingAfterServiceAccess_ms;
	private final boolean endOfUserByScreenSaver;
	private final boolean clearErrorMessageAfterMail;

	private final int watchDogTime;
	private boolean abort = false;
	private boolean waiting = false;
	private HumanAccess humanAccess = HumanAccess.UNKNOWN;
	private boolean serviceScreenDetected = false;
	private boolean powerOff = false;
	private long lastUserAccessTime = 0;
	private long serviceAccessFinishedTime = 0;
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
								processEvent(Event.POWER_OFF, null);
							}
							break;
						case POWER_OFF:
							processEvent(Event.POWER_OFF, null);
							break;
						case SOLVIS_CONNECTED:
							processEvent(Event.POWER_ON, null);
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
		this.clearErrorMessageAfterMail = solvis.getFeatures().isClearErrorMessageAfterMail();
		this.saver = saver.createExecutable(solvis);
		Unit unit = this.solvis.getUnit();
		this.releaseBlockingAfterUserChange_ms = BaseData.DEBUG ? Debug.USER_ACCESS_TIME
				: unit.getReleaseBlockingAfterUserAccess_ms();
		this.releaseBlockingAfterServiceAccess_ms = unit.getReleaseBlockingAfterServiceAccess_ms();
		this.endOfUserByScreenSaver = unit.getFeatures().getFeature(XML_END_OF_USER_BY_SCREEN_SAVER);
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

	public enum HumanAccess {
		USER(false, "User", SolvisStatus.USER_ACCESS_DETECTED),
		SERVICE(false, "Service", SolvisStatus.SERVICE_ACCESS_DETECTED),
		NONE(true, "None", SolvisStatus.HUMAN_ACCESS_FINISHED),
		UNKNOWN(true, "None", SolvisStatus.HUMAN_ACCESS_FINISHED);

		private final boolean serverAccessEnabled;
		private final String accessType;
		private final SolvisStatus status;

		private HumanAccess(boolean serverAccessEnabled, String accessType, SolvisStatus connectionStatus) {
			this.serverAccessEnabled = serverAccessEnabled;
			this.accessType = accessType;
			this.status = connectionStatus;
		}

		private boolean isServerAccessEnabled() {
			return this.serverAccessEnabled;
		}

		private String getAccessType() {
			return this.accessType;
		}

		public SolvisStatus getStatus() {
			return this.status;
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
				this.processEvent(Event.INIT, null);
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

				this.processEvent(event, realScreen);

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

	private void processEvent(final Event event, final SolvisScreen realScreen)
			throws IOException, TerminationException {
		HumanAccess current = this.humanAccess;
		long currentTime = System.currentTimeMillis();
		switch (event) {
			case POWER_OFF:
				this.powerOff = true;
				current = HumanAccess.SERVICE;
				this.solvis.setScreenSaverActive(false);
				break;

			case NONE:
			case SCREENSAVER:
				switch (this.humanAccess) {
					case USER:
						synchronized (this) {
							if (event == Event.SCREENSAVER) {
								current = HumanAccess.NONE;
							} else if (!this.endOfUserByScreenSaver
									&& currentTime > this.lastUserAccessTime + this.releaseBlockingAfterUserChange_ms) {
								current = HumanAccess.NONE;
							}
						}
						break;
					case SERVICE:
						synchronized (this) {
							if ((!this.endOfUserByScreenSaver || event == Event.SCREENSAVER)
									&& !this.serviceScreenDetected && !this.powerOff
									&& currentTime > this.serviceAccessFinishedTime
											+ this.releaseBlockingAfterServiceAccess_ms) {
								current = HumanAccess.NONE;
							}
						}
						break;
				}
				if (event == Event.SCREENSAVER) {
					this.solvis.setScreenSaverActive(true);
				}
				break;

			case POWER_ON:
				this.powerOff = false;
				if (this.humanAccess == HumanAccess.SERVICE) {
					synchronized (this) {
						this.serviceAccessFinishedTime = currentTime;
						this.serviceScreenDetected = false;
					}
				}
				this.solvis.setScreenSaverActive(false);
				break;

			case HUMAN_ACCESS_SERVICE:
				if (this.powerOff) {
					current = HumanAccess.SERVICE;
				} else {
					synchronized (this) {
						this.serviceScreenDetected = true;
						current = HumanAccess.SERVICE;
					}
				}
				this.solvis.setScreenSaverActive(false);
				break;

			case HUMAN_ACCESS_USER:
				if (this.powerOff) {
					current = HumanAccess.SERVICE;
				} else if (this.humanAccess == HumanAccess.SERVICE) {
					synchronized (this) {
						this.serviceAccessFinishedTime = currentTime;
						this.serviceScreenDetected = false;
					}
				} else {
					synchronized (this) {
						this.lastUserAccessTime = currentTime;
						current = HumanAccess.USER;
					}
				}
				this.solvis.setScreenSaverActive(false);
				break;

			case SET_ERROR_BY_BUTTON:
			case SET_ERROR_BY_MESSAGE:
			case RESET_ERROR:
				this.solvis.setScreenSaverActive(false);
				break;

			case INIT:
				if (realScreen != null && realScreen.isService()) {
					synchronized (this) {
						this.serviceScreenDetected = true;
						this.humanAccess = HumanAccess.SERVICE;
					}
				}
				this.solvis.setScreenSaverActive(false);
				break;

			case TRIGGER_SERVICE_BY_COMMAND:
				this.serviceAccessFinishedTime = currentTime;
				current = HumanAccess.SERVICE;
				break;

			case RESET_SERVICE_BY_COMMAND:
				if (!this.serviceScreenDetected && !this.powerOff && !this.humanAccess.isServerAccessEnabled()) {
					this.serviceAccessFinishedTime = 0;
					current = HumanAccess.NONE;
				}
				break;

		}

		processHumanAccess(current);
	}

	private void processHumanAccess(final HumanAccess access) throws IOException, TerminationException {

		if (this.humanAccess != HumanAccess.UNKNOWN) {
			if (this.humanAccess == access) {
				return;
			}
			this.solvis.notifyScreenChangedByHumanObserver(access);
		}

		HumanAccess current = access;

		switch (current) {
			case SERVICE:
			case USER:
				logger.info(current.getAccessType() + " access detected.");
				break;
			case NONE:
				logger.info(this.humanAccess.getAccessType() + " access finished.");
				this.solvis.saveScreen();
				break;
			case UNKNOWN:
				current = HumanAccess.NONE;
				this.solvis.saveScreen();
				break;
		}
		this.humanAccess = current;
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

	synchronized void serviceAccess(Event event) throws IOException, TerminationException {
		switch (event) {
			case TRIGGER_SERVICE_BY_COMMAND:
			case RESET_SERVICE_BY_COMMAND:
				this.processEvent(event, null);
				break;
			default:
				logger.error("Unexpected call of \"serviceAccess\". Ignored");
		}
	}
}
