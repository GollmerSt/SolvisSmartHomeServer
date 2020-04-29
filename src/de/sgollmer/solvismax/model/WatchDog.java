/************************************************************************
 * 
 * $Id$
 *
 * 
 ************************************************************************/

package de.sgollmer.solvismax.model;

import java.io.IOException;
import java.util.Collection;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import de.sgollmer.solvismax.BaseData;
import de.sgollmer.solvismax.Constants;
import de.sgollmer.solvismax.connection.ConnectionStatus;
import de.sgollmer.solvismax.imagepatternrecognition.image.MyImage;
import de.sgollmer.solvismax.model.objects.Miscellaneous;
import de.sgollmer.solvismax.model.objects.Observer.ObserverI;
import de.sgollmer.solvismax.model.objects.screen.ScreenSaver;
import de.sgollmer.solvismax.model.objects.screen.SolvisScreen;
import de.sgollmer.solvismax.objects.Rectangle;

public class WatchDog {

	private static final Logger logger = LogManager.getLogger(WatchDog.class);

	private final Solvis solvis;
	private final ScreenSaver saver;

	private SolvisScreen lastScreen = null;
	private SolvisScreen realScreen = null;
	private final int releaseBlockingAfterUserChange_ms;
	private final int releaseBlockingAfterServiceAccess_ms;

	private final int watchDogTime;
	private int errorDetected = 0;
	private boolean abort = false;
	private HumanAccess humanAccess = HumanAccess.NONE;
	private HumanAccess formerHumanAccess = HumanAccess.NONE;
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
						processHumanAccess(Event.POWER_OFF);
						break;
					case SOLVIS_CONNECTED:
						processHumanAccess(Event.POWER_ON);
						break;
				}
			}
		}

	}

	public WatchDog(Solvis solvis, ScreenSaver saver) {
		this.solvis = solvis;
		this.saver = saver;
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
		SCREENSAVER, ERROR, NONE, CHANGED, INIT, POWER_OFF, POWER_ON
	}

	public void execute() {

		this.abort = false;

		if (!this.initialized) {
			this.initialized = true;
			try {
				this.isScreenSaver();
				this.processHumanAccess(Event.INIT);
			} catch (Throwable e) {
			}
		}
		while (!this.abort) { // loop in case off user access or error detected

			try {

				boolean errorPending = this.errorDetected > 0
						&& this.errorDetected < Constants.ERROR_DETECTED_AFTER_N_ERROR_SCREENS;
				Event event = Event.NONE;

				this.realScreen = this.solvis.getRealScreen();
				if (!errorPending && this.realScreen.imagesEquals(this.lastScreen)) {
					// do nothing
				} else if (this.isScreenSaver()) {
					event = Event.SCREENSAVER;
//					event = this.isHumanAccess() ? Event.CHANGED : Event.SCREENSAVER;
				} else if (this.isError()) {
					if (this.errorDetected < Constants.ERROR_DETECTED_AFTER_N_ERROR_SCREENS) {
						++this.errorDetected;
					} else {
						event = Event.ERROR;
					}
				} else {
					this.errorDetected = 0;
					event = this.isHumanAccess() ? Event.CHANGED : Event.NONE;
				}
				this.lastScreen = this.realScreen;

				if (event == Event.CHANGED) {
					this.solvis.setCurrentScreen(this.realScreen);
				}

				this.processHumanAccess(event);

				this.abort = !this.humanAccess.mustWait() && this.errorDetected == 0;

				boolean errorDetected = this.errorDetected >= Constants.ERROR_DETECTED_AFTER_N_ERROR_SCREENS;
				this.solvis.getSolvisState().error(errorDetected, "Error screen", this.realScreen.getImage());

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

	private final Detect screenSaverDetect = new Detect() {

		@Override
		public boolean detect(SolvisScreen screen) throws IOException {
			boolean screenSaverActive = false;
			if (WatchDog.this.saver.is(screen)) {
				screenSaverActive = true;
			} else {
				if (!WatchDog.this.saver.getDebugInfo().isEmpty()) {
					logger.info(WatchDog.this.saver.getDebugInfo());
				}
			}
			return screenSaverActive;
		}
	};

	private boolean isScreenSaver() throws IOException {
		boolean screenSaverActive = this.getTwiceState(this.screenSaverDetect);
		WatchDog.this.solvis.setScreenSaverActive(screenSaverActive);
		return screenSaverActive;
	}

	private final Detect humanAccessDetect = new Detect() {

		@Override
		public boolean detect(SolvisScreen screen) throws IOException {
			boolean humanAccess = false;

			if (!screen.imagesEquals(WatchDog.this.solvis.getCurrentScreen(false))) {

				if (screen.get() != null && screen.get() == WatchDog.this.solvis.getCurrentScreen(false).get()) {
					if (!screen.get().isIgnoreChanges()) {
						Collection<Rectangle> ignoreRectangles = screen.get().getIgnoreRectangles();
						if (ignoreRectangles == null) {
							humanAccess = true;
						} else {
							MyImage ignoreRectScreen = new MyImage(screen.getImage(), false, ignoreRectangles);
							if (!ignoreRectScreen.equals(WatchDog.this.solvis.getCurrentScreen(false).getImage(),
									true)) {
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

	};

	private boolean isHumanAccess() throws IOException {
		return this.getTwiceState(this.humanAccessDetect);
	}

	private final Detect errorDetect = new Detect() {

		@Override
		public boolean detect(SolvisScreen screen) {
			return WatchDog.this.solvis.getSolvisDescription().getErrorDetection().is(screen);
		}

	};

	private boolean isError() throws IOException {
		return this.getTwiceState(this.errorDetect);
	}

	private HumanAccess processHumanAccess(Event event) {
		long currentTime = System.currentTimeMillis();
		switch (event) {
			case POWER_OFF:
				this.powerOff = true;
				this.humanAccess = HumanAccess.SERVICE;
				break;
			case NONE:
			case SCREENSAVER:
				switch (this.humanAccess) {
					case USER:
						synchronized (this) {
							if (event == Event.SCREENSAVER) {
								this.humanAccess = HumanAccess.NONE;
							} else if (currentTime > this.lastUserAccessTime + this.releaseBlockingAfterUserChange_ms) {
								this.humanAccess = HumanAccess.NONE;
							}
						}
						break;
					case SERVICE:
						synchronized (this) {
							if (!this.serviceScreenDetected && !this.powerOff
									&& currentTime > this.serviceAccessFinishedTime
											+ this.releaseBlockingAfterServiceAccess_ms) {
								this.humanAccess = HumanAccess.NONE;
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
					this.humanAccess = HumanAccess.SERVICE;
				} else if (this.realScreen != null && this.solvis.getSolvisDescription().getService()
						.isServiceScreen(this.realScreen.get(), this.solvis)
						&& this.solvis.getUnit().getFeatures().isDetectServiceAccess()) {
					synchronized (this) {
						this.serviceScreenDetected = true;
						this.humanAccess = HumanAccess.SERVICE;
					}
				} else if (this.humanAccess == HumanAccess.SERVICE) {
					synchronized (this) {
						this.serviceAccessFinishedTime = currentTime;
						this.serviceScreenDetected = false;
					}
				} else {
					synchronized (this) {
						this.lastUserAccessTime = currentTime;
						this.humanAccess = HumanAccess.USER;
					}
				}
				break;
			case ERROR:
				break;
			case INIT:
				if (this.solvis.getSolvisDescription().getService().isServiceScreen(this.realScreen.get(),
						this.solvis)) {
					synchronized (this) {
						this.serviceScreenDetected = true;
						this.humanAccess = HumanAccess.SERVICE;
					}
				}
		}

		humanAccessChanged();
		return this.humanAccess;
	}

	private void humanAccessChanged() {
		if (this.formerHumanAccess != this.humanAccess) {
			this.solvis.notifyScreenChangedByHumanObserver(this.humanAccess);

			switch (this.humanAccess) {
				case SERVICE:
				case USER:
					logger.info(this.humanAccess.getAccessType() + " access detected.");
					break;
				case NONE:
					logger.info(this.formerHumanAccess.getAccessType() + " access finished.");
			}
		}
		this.formerHumanAccess = this.humanAccess;
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

	private static abstract class Detect {
		private Boolean former = null;

		public abstract boolean detect(SolvisScreen screen) throws IOException;
	}

	private boolean getTwiceState(final Detect detect) throws IOException {
		Boolean state = detect.former;
		boolean currentState = false;
		boolean repeat = true;
		SolvisScreen realScreen = WatchDog.this.realScreen;

		for (int r = 0; r < 2 && repeat && !this.abort; ++r) {

			if (realScreen == null) {
				realScreen = WatchDog.this.solvis.getRealScreen();
			}
			currentState = detect.detect(realScreen);

			if (state == null || currentState == state) {
				repeat = false;
			}
			if (repeat && r == 0) {
				realScreen = null;
				synchronized (this) {
					if (!WatchDog.this.abort) {
						try {
							this.wait(Constants.WAIT_AFTER_FIRST_ASYNC_DETECTION);
						} catch (InterruptedException e) {
						}
					}
				}
			}
		}
		WatchDog.this.realScreen = realScreen;
		detect.former = currentState;
		return currentState;
	}
}
