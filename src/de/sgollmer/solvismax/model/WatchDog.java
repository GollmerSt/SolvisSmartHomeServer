/************************************************************************
 * 
 * $Id$
 *
 * 
 ************************************************************************/

package de.sgollmer.solvismax.model;

import java.io.IOException;
import java.util.Collection;

import de.sgollmer.solvismax.BaseData;
import de.sgollmer.solvismax.Constants;
import de.sgollmer.solvismax.connection.ConnectionStatus;
import de.sgollmer.solvismax.imagepatternrecognition.image.MyImage;
import de.sgollmer.solvismax.log.LogManager;
import de.sgollmer.solvismax.log.LogManager.Logger;
import de.sgollmer.solvismax.model.Solvis.SynchronizedScreenResult;
import de.sgollmer.solvismax.model.objects.Miscellaneous;
import de.sgollmer.solvismax.model.objects.Observer.ObserverI;
import de.sgollmer.solvismax.model.objects.screen.ScreenSaver;
import de.sgollmer.solvismax.model.objects.screen.ScreenSaver.SaverState;
import de.sgollmer.solvismax.model.objects.screen.SolvisScreen;
import de.sgollmer.solvismax.objects.Rectangle;

public class WatchDog {

	private static final Logger logger = LogManager.getInstance().getLogger(WatchDog.class);

	private final Solvis solvis;
	private final ScreenSaver.getState saver;

	private final int releaseBlockingAfterUserChange_ms;
	private final int releaseBlockingAfterServiceAccess_ms;
	private final boolean clearErrorMessageAfterMail;

	private final int watchDogTime;
	private boolean abort = false;
	private HumanAccess humanAccess = HumanAccess.NONE;
	private boolean errorMessageVisible = false;
	private boolean serviceScreenDetected = false;
	private boolean powerOff = false;
	private long lastUserAccessTime = 0;
	private long serviceAccessFinishedTime = 0;
	private boolean initialized = false;
	private SolvisStateObserver solvisStateObserver = new SolvisStateObserver();

	private class SolvisStateObserver implements ObserverI<SolvisState> {

		@Override
		public void update(SolvisState data, Object source) {
			synchronized (WatchDog.this) {
				switch (data.getState()) {
					case POWER_OFF:
					case REMOTE_CONNECTED:
						getCurrentHumanAccess(Event.POWER_OFF, null);
						break;
					case SOLVIS_CONNECTED:
						getCurrentHumanAccess(Event.POWER_ON, null);
						break;
				}
			}
		}

	}

	public WatchDog(Solvis solvis, ScreenSaver saver) {
		this.solvis = solvis;
		this.clearErrorMessageAfterMail = solvis.getUnit().getFeatures().isClearErrorMessageAfterMail();
		this.saver = saver.createExecutable(solvis);
		Miscellaneous misc = this.solvis.getSolvisDescription().getMiscellaneous();
		this.releaseBlockingAfterUserChange_ms = BaseData.DEBUG ? Constants.DEBUG_USER_ACCESS_TIME
				: misc.getReleaseBlockingAfterUserAccess_ms();
		this.releaseBlockingAfterServiceAccess_ms = misc.getReleaseBlockingAfterServiceAccess_ms();
		this.watchDogTime = this.solvis.getUnit().getWatchDogTime_ms();
		this.solvis.registerAbortObserver(new ObserverI<Boolean>() {

			@Override
			public void update(Boolean data, Object source) {
				if (data) {
					abort();
				}

			}
		});
		if (this.solvis.getUnit().getFeatures().isPowerOffIsServiceAccess()
				&& this.solvis.getUnit().getFeatures().isDetectServiceAccess()) {
			this.solvis.getSolvisState().register(this.solvisStateObserver);
		}
	}

	public enum HumanAccess {
		USER(true, "User", ConnectionStatus.USER_ACCESS_DETECTED),
		SERVICE(true, "Service", ConnectionStatus.SERVICE_ACCESS_DETECTED),
		NONE(false, "", ConnectionStatus.HUMAN_ACCESS_FINISHED);

		private final boolean wait;
		private final String accessType;
		private final ConnectionStatus connectionStatus;

		private HumanAccess(boolean wait, String accessType, ConnectionStatus connectionStatus) {
			this.wait = wait;
			this.accessType = accessType;
			this.connectionStatus = connectionStatus;
		}

		public boolean mustWait() {
			return this.wait;
		}

		public String getAccessType() {
			return this.accessType;
		}

		public ConnectionStatus getConnectionStatus() {
			return this.connectionStatus;
		}

	}

	public enum Event {
		SCREENSAVER, SET_ERROR, RESET_ERROR, NONE, CHANGED, INIT, POWER_OFF, POWER_ON
	}

	public void execute() {

		this.abort = false;
		boolean handleErrorSuccessfull = true;

		if (!this.initialized) {
			this.initialized = true;
			try {
				this.getCurrentHumanAccess(Event.INIT, null);
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

					SaverState saverState = this.saver.getSaverState(realScreen) ;
					if ( saverState == SaverState.SCREENSAVER) {
						event = Event.SCREENSAVER;
						possibleErrorScreen = null;
					} else if ( saverState != SaverState.POSSIBLE) {						
						event = this.checkError(realScreen);  // RESET_ERROR or SET_ERROR

						if (event == null) {
							event = this.isHumanAccess(realScreen) ? Event.CHANGED : Event.NONE;
						}
					}
				}

				if (event == Event.CHANGED) {
					this.solvis.setCurrentScreen(realScreen);
				}

				if (event == Event.RESET_ERROR || event == Event.SET_ERROR) {
					handleErrorSuccessfull = this.solvis.getSolvisState().setError(event == Event.SET_ERROR,
							possibleErrorScreen);
				}

				HumanAccess currentHA = this.getCurrentHumanAccess(event, realScreen);

				if (currentHA != this.humanAccess) {
					humanAccessChanged(currentHA, this.humanAccess);
					this.humanAccess = currentHA;
				}

				boolean abortEnable1 = !this.solvis.getSolvisState().isErrorMessage()
						|| this.clearErrorMessageAfterMail && handleErrorSuccessfull;

				boolean abortEnable2 = !this.errorMessageVisible;

				this.abort = !currentHA.mustWait() && (abortEnable1 || abortEnable2);

				synchronized (this) {
					if (!this.abort) {
						try {
							this.wait(this.watchDogTime);
						} catch (InterruptedException e) {
						}
					}
				}

			} catch (IOException e) {
				synchronized (this) {
					if (!this.abort) {
						try {
							this.wait(Constants.WAIT_TIME_AFTER_IO_ERROR);
						} catch (InterruptedException e1) {
						}
					}
				}
			}
		}

	}

	private Event checkError(SolvisScreen realScreen) {
		Event event = null;
		this.errorMessageVisible = false;
		switch (this.solvis.getSolvisDescription().getErrorDetection().getType(realScreen)) {
			case MESSAGE_BOX:
				this.errorMessageVisible = true;
				event = Event.SET_ERROR;
				break;
			case ERROR_BUTTON:
				event = Event.SET_ERROR;
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

	private boolean isHumanAccess(SolvisScreen screen) throws IOException {
		boolean humanAccess = false;
		if (!screen.imagesEquals(WatchDog.this.solvis.getCurrentScreen(false))) {

			if (screen.get() != null && screen.get() == WatchDog.this.solvis.getCurrentScreen(false).get()) {
				if (!screen.get().isIgnoreChanges()) {
					Collection<Rectangle> ignoreRectangles = screen.get().getIgnoreRectangles();
					if (ignoreRectangles == null) {
						humanAccess = true;
					} else {
						MyImage ignoreRectScreen = new MyImage(screen.getImage(), false, ignoreRectangles);
						if (!ignoreRectScreen.equals(WatchDog.this.solvis.getCurrentScreen(false).getImage(), true)) {
							humanAccess = true;
						}
					}
				}
			} else {
				humanAccess = true;
			}
		}
		return humanAccess;
	}

	private HumanAccess getCurrentHumanAccess(Event event, SolvisScreen realScreen) {
		HumanAccess current = this.humanAccess;
		long currentTime = System.currentTimeMillis();
		switch (event) {
			case POWER_OFF:
				this.powerOff = true;
				current = HumanAccess.SERVICE;
				break;
			case NONE:
			case SCREENSAVER:
				switch (this.humanAccess) {
					case USER:
						synchronized (this) {
							if (event == Event.SCREENSAVER) {
								current = HumanAccess.NONE;
							} else if (currentTime > this.lastUserAccessTime + this.releaseBlockingAfterUserChange_ms) {
								current = HumanAccess.NONE;
							}
						}
						break;
					case SERVICE:
						synchronized (this) {
							if (!this.serviceScreenDetected && !this.powerOff
									&& currentTime > this.serviceAccessFinishedTime
											+ this.releaseBlockingAfterServiceAccess_ms) {
								current = HumanAccess.NONE;
							}
						}
						break;
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
				break;
			case CHANGED:
				if (this.powerOff) {
					current = HumanAccess.SERVICE;
				} else if (realScreen != null && this.solvis.getSolvisDescription().getService()
						.isServiceScreen(realScreen.get(), this.solvis)
						&& this.solvis.getUnit().getFeatures().isDetectServiceAccess()) {
					synchronized (this) {
						this.serviceScreenDetected = true;
						current = HumanAccess.SERVICE;
					}
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
				break;
			case SET_ERROR:
			case RESET_ERROR:
				break;
			case INIT:
				if (realScreen != null && this.solvis.getSolvisDescription().getService()
						.isServiceScreen(realScreen.get(), this.solvis)) {
					synchronized (this) {
						this.serviceScreenDetected = true;
						this.humanAccess = HumanAccess.SERVICE;
					}
				}
		}

		return current;
	}

	private void humanAccessChanged(HumanAccess current, HumanAccess former) {
		this.solvis.notifyScreenChangedByHumanObserver(current);

		switch (current) {
			case SERVICE:
			case USER:
				logger.info(current.getAccessType() + " access detected.");
				break;
			case NONE:
				logger.info(former.getAccessType() + " access finished.");
		}
	}

	private synchronized void abort() {
		this.abort = true;
		this.notifyAll();
	}

	public synchronized void bufferNotEmpty() {
		this.notifyAll();
	}

	public synchronized void serviceReset() {
		if (!this.serviceScreenDetected && !this.powerOff && this.humanAccess == HumanAccess.SERVICE) {
			this.serviceAccessFinishedTime = 0;
		}

	}
}
