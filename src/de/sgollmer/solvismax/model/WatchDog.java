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
	private boolean screenSaverActive = false;
	private boolean errorDetected = false;
	private boolean abort = false;
	private HumanAccess humanAccess = HumanAccess.NONE;
	private HumanAccess formerHumanAccess = HumanAccess.NONE;
	private boolean serviceScreenDetected = false;
	private long lastUserAccessTime = 0;
	private long serviceAccessFinishedTime = 0;
	private boolean initialized = false;
	private SolvisStateObserver solvisStateObserver = new SolvisStateObserver();

	private class SolvisStateObserver implements ObserverI<SolvisState> {

		@Override
		public void update(SolvisState data, Object source) {
			switch (data.getState()) {
				case POWER_OFF:
				case REMOTE_CONNECTED:
					synchronized (WatchDog.this) {
						humanAccess = HumanAccess.SERVICE;
						serviceScreenDetected = true;
						humanAccessChanged();
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
			return wait;
		}

		public String getAccessType() {
			return accessType;
		}

		public ConnectionStatus getConnectionStatus() {
			return connectionStatus;
		}

	}

	public enum Event {
		SCREENSAVER, ERROR, NONE, CHANGED, INIT
	}

	public void execute() {

		this.abort = false;

		if (!initialized) {
			this.initialized = true;
			try {
				this.isScreenSaver();
				this.realScreen = this.solvis.getCurrentScreen();
				this.processHumanAccess(Event.INIT);
			} catch (Throwable e) {
			}
		}
		while (!abort) { // loop in case off user access or error detected

			try {
				Event event = Event.NONE;

				this.realScreen = this.solvis.getRealScreen();
				if (this.realScreen.imagesEquals(this.lastScreen)) {
					// do nothing
				} else if (this.isScreenSaver()) {
					event = Event.SCREENSAVER;
				} else if (solvis.getSolvisDescription().getErrorDetection().is(this.realScreen)) {
					this.errorDetected = true;
					event = Event.ERROR;
				} else {
					this.errorDetected = false;
					event = this.checkHumanAccess() ? Event.CHANGED : Event.NONE;
				}
				this.lastScreen = this.realScreen;

				if (event == Event.CHANGED) {
					solvis.setCurrentCreen(this.realScreen);
				}

				this.processHumanAccess(event);

				abort = !this.humanAccess.mustWait() && !errorDetected;

				solvis.getSolvisState().error(errorDetected, "Error screen");

				synchronized (this) {
					if (!abort) {
						try {
							this.wait(this.watchDogTime);
						} catch (InterruptedException e) {
						}
					}
				}

			} catch (IOException e) {
			}
		}
	}

	private boolean isScreenSaver() throws IOException {
		boolean finished = false;
		SolvisScreen realScreen = this.realScreen;
		while (!finished && !this.abort) {
			finished = true;
			if (realScreen == null) {
				realScreen = this.solvis.getRealScreen();
			}
			if (this.saver.is(realScreen)) {
				this.screenSaverActive = true;
				this.solvis.setScreenSaverActive(true);
			} else {
				if (this.screenSaverActive) {
					finished = false;
					realScreen = null; // read image again
					this.screenSaverActive = false;
				} else {
					this.solvis.setScreenSaverActive(false);
					if (!this.saver.getDebugInfo().isEmpty()) {
						logger.info(this.saver.getDebugInfo());
					}
				}
			}
			if (!finished) {
				synchronized (this) {
					if (!this.abort) {
						try {
							this.wait(Constants.WAIT_AFTER_SCREEN_SAVER_FINISHED_DETECTED);
						} catch (InterruptedException e) {
						}
					}
				}
			}
		}
		this.realScreen = realScreen;
		return this.screenSaverActive;
	}

	private boolean checkHumanAccess() throws IOException {
		boolean humanAccess = false;
		SolvisScreen realScreen = this.realScreen;
		boolean repeat = false;
		boolean finished = false;

		while (!finished && !this.abort) {
			if (realScreen == null) {
				realScreen = this.solvis.getRealScreen();
			}

			humanAccess = false;

			if (realScreen.imagesEquals(this.solvis.getCurrentScreen())) {
				finished = true;
			} else {

				if (realScreen.get() != null && realScreen.get() == solvis.getCurrentScreen().get()) {
					if (realScreen.get().isIgnoreChanges()) {
						finished = true;
					} else {
						Collection<Rectangle> ignoreRectangles = realScreen.get().getIgnoreRectangles();
						if (ignoreRectangles == null) {
							humanAccess = true;
							finished = false;
						} else {
							MyImage ignoreRectScreen = new MyImage(realScreen.getImage(), false, ignoreRectangles);
							if (ignoreRectScreen.equals(this.solvis.getCurrentScreen().getImage(), true)) {
								finished = true;
							} else {
								humanAccess = true;
								finished = false;
							}
						}
					}
				} else {
					humanAccess = true;
					finished = false;
				}
			}

			if (!finished && !repeat) {
				repeat = true;
				realScreen = null;
				synchronized (this) {
					if (!abort) {
						try {
							this.wait(Constants.WAIT_AFTER_SCREEN_SAVER_FINISHED_DETECTED);
						} catch (InterruptedException e) {
						}
					}
				}
			} else {
				finished = true;
			}

		}
		this.realScreen = realScreen;
		return humanAccess;
	}

	private void processHumanAccess(Event event) {
		long currentTime = System.currentTimeMillis();
		switch (event) {
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
							if (!serviceScreenDetected && currentTime > this.serviceAccessFinishedTime
									+ this.releaseBlockingAfterServiceAccess_ms) {
								this.humanAccess = HumanAccess.NONE;
							}
						}
						break;
				}
				break;
			case CHANGED:
				if (this.solvis.getSolvisDescription().getService().isServiceScreen(this.realScreen.get(), this.solvis)
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
	}

	private void humanAccessChanged() {
		if (this.formerHumanAccess != this.humanAccess) {
			solvis.notifyScreenChangedByHumanObserver(this.humanAccess);

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
		if (!this.serviceScreenDetected && this.humanAccess == HumanAccess.SERVICE) {
			this.serviceAccessFinishedTime = 0;
		}

	}
}
