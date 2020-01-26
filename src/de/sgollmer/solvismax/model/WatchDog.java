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
	private final int releaseblockingAfterUserChange_ms;
	private final int watchDogTime;
	private boolean screenSaverActive = false;
	private boolean errorDetected = false;
	private boolean abort = false;

	public WatchDog(Solvis solvis, ScreenSaver saver) {
		this.solvis = solvis;
		this.saver = saver;
		Miscellaneous misc = this.solvis.getSolvisDescription().getMiscellaneous();
		int releaseBlocking = misc.getReleaseblockingAfterUserChange_ms();
		if (BaseData.DEBUG) {
			releaseBlocking = Constants.DEBUG_USER_ACCESS_TIME;
		}
		this.releaseblockingAfterUserChange_ms = releaseBlocking;
		this.watchDogTime = misc.getWatchDogTime_ms();
		this.solvis.registerAbortObserver(new ObserverI<Boolean>() {

			@Override
			public void update(Boolean data, Object source) {
				if (data) {
					abort();
				}

			}
		});
	}

	private enum UserAccess {
		DETECTED, RESET, NONE
	}

	public void execute() {

		long changedTime = -1;
		this.abort = false;

		while (!abort) { // loop in case off user access or error detected

			long time = System.currentTimeMillis();

			try {
				UserAccess userAccess = UserAccess.NONE;

				this.realScreen = this.solvis.getRealScreen();
				if (this.realScreen.imagesEquals(this.lastScreen)) {
					// do nothing
				} else if (solvis.getSolvisDescription().getErrorScreen().is(this.realScreen)) {
					this.errorDetected = true;
					abort = false;
					userAccess = UserAccess.RESET;
				} else if (this.isScreenSaver()) {
					userAccess = UserAccess.RESET;
				} else {
					this.errorDetected = false;
					userAccess = this.checkUserAccess();
				}
				this.lastScreen = this.realScreen;

				if (userAccess != UserAccess.DETECTED && changedTime >= 0
						&& time > changedTime + this.releaseblockingAfterUserChange_ms) {
					userAccess = UserAccess.RESET;
				}

				switch (userAccess) {
					case NONE:
						if (changedTime > 0) {
							abort = false;
						} else {
							abort = true;
						}
						break;
					case RESET:
						if (changedTime > 0) {
							this.solvis.clearCurrentScreen();
							solvis.notifyScreenChangedByUserObserver(false);
							logger.info("User access finished");
						}
						changedTime = -1;
						abort = true;
						break;
					case DETECTED:
						this.solvis.clearCurrentScreen();
						this.solvis.getCurrentScreen();
						if (changedTime < 0) {
							solvis.notifyScreenChangedByUserObserver(true);
							logger.info("User access detected");
						}
						changedTime = time;
						abort = false;
						break;
				}

				solvis.getSolvisState().error(errorDetected);

				if (errorDetected) {
					abort = false;
				}

				synchronized (this) {
					if (!abort) {
						this.wait(this.watchDogTime);
					}
				}

			} catch (Throwable e) {

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

	private UserAccess checkUserAccess() throws IOException {
		UserAccess userAccess = UserAccess.NONE;
		SolvisScreen realScreen = this.realScreen;
		boolean repeat = false;
		boolean finished = false;

		while (!finished && !this.abort) {
			if (realScreen == null) {
				realScreen = this.solvis.getRealScreen();
			}

			userAccess = UserAccess.NONE;

			if (realScreen.imagesEquals(this.solvis.getCurrentScreen())) {
				finished = true;
			} else {

				if (realScreen.get() != null && realScreen.get() == solvis.getCurrentScreen().get()) {
					if (realScreen.get().isIgnoreChanges()) {
						finished = true;
					} else {
						Collection<Rectangle> ignoreRectangles = realScreen.get().getIgnoreRectangles();
						if (ignoreRectangles == null) {
							userAccess = UserAccess.DETECTED;
							finished = false;
						} else {
							MyImage ignoreRectScreen = new MyImage(realScreen.getImage(), false, ignoreRectangles);
							if (ignoreRectScreen.equals(this.solvis.getCurrentScreen().getImage(), true)) {
								finished = true;
							} else {
								userAccess = UserAccess.DETECTED;
								finished = false;
							}
						}
					}
				} else {
					userAccess = UserAccess.DETECTED;
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
		return userAccess;
	}

	private synchronized void abort() {
		this.abort = true;
		this.notifyAll();
	}

	public synchronized void bufferNotEmpty() {
		this.notifyAll();
	}
}
