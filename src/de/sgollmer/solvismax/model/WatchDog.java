/************************************************************************
 * 
 * $Id$
 *
 * 
 ************************************************************************/

package de.sgollmer.solvismax.model;

import java.util.Collection;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import de.sgollmer.solvismax.BaseData;
import de.sgollmer.solvismax.Constants;
import de.sgollmer.solvismax.imagepatternrecognition.image.MyImage;
import de.sgollmer.solvismax.model.objects.Miscellaneous;
import de.sgollmer.solvismax.model.objects.Observer.ObserverI;
import de.sgollmer.solvismax.model.objects.screen.ErrorScreen;
import de.sgollmer.solvismax.model.objects.screen.Screen;
import de.sgollmer.solvismax.model.objects.screen.ScreenSaver;
import de.sgollmer.solvismax.objects.Rectangle;

public class WatchDog {

	private static final Logger logger = LogManager.getLogger(WatchDog.class);

	private final Solvis solvis;
	private final ScreenSaver saver;

	private MyImage lastImage = null;
	private final int releaseblockingAfterUserChange_ms;
	private final int watchDogTime;
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

	private enum UserAcess {
		DETECTED, RESET, NONE
	}

	public void execute() {

		long changedTime = -1;
		this.abort = false;
		boolean screenSaverActive = false;

		while (!abort) {

			long time = System.currentTimeMillis();

			boolean errorDetected = false;

			try {
				boolean finished = false;
				boolean repeat = false;
				MyImage solvisImage = null;
				UserAcess userAcess = UserAcess.NONE;
				for (int cnt = 0; (!finished || repeat) && cnt < 2; ++cnt) {
					userAcess = UserAcess.NONE;
					repeat = false;
					solvisImage = this.solvis.getRealImage();
					if (solvisImage.equals(this.lastImage)) {
						finished = true;
					} else if (this.saver.is(solvisImage)) {
						finished = true;
						screenSaverActive = true;
						this.solvis.setScreenSaverActive(true);
						userAcess = UserAcess.RESET;
					} else {
						if (screenSaverActive) {
							repeat = true;
							screenSaverActive = false;
						}
						if (ErrorScreen.is(solvisImage)) {
							finished = true;
							errorDetected = true;
							userAcess = UserAcess.RESET;
						} else {

							if (solvisImage.equals(this.solvis.getCurrentImage())) {
								finished = true;

							} else {

								Screen screen = solvis.getSolvisDescription().getScreens().getScreen(solvisImage,
										solvis);

								if (screen != null && screen == solvis.getCurrentScreen()) {
									if (screen.isIgnoreChanges()) {
										finished = true;
									} else {
										Collection<Rectangle> ignoreRectangles = screen.getIgnoreRectangles();
										if (ignoreRectangles == null) {
											userAcess = UserAcess.DETECTED;
										} else {
											MyImage ignoreRectScreen = new MyImage(solvisImage, false,
													ignoreRectangles);
											if (ignoreRectScreen.equals(this.solvis.getCurrentImage(), true)) {
												finished = true;
											} else {
												userAcess = UserAcess.DETECTED;
											}
										}
									}
								} else {
									userAcess = UserAcess.DETECTED;
								}
							}
						}
					}
					if (repeat) {
						synchronized (this) {
							if (!abort) {
								this.wait(Constants.WAIT_AFTER_SCREEN_SAVER_FINISHED_DETECTED);
							}
						}
					}

				}
				this.lastImage = solvisImage;

				if (!screenSaverActive) {
					this.solvis.setScreenSaverActive(false);
				}

				if (userAcess != UserAcess.DETECTED && changedTime >= 0
						&& time > changedTime + this.releaseblockingAfterUserChange_ms) {
					userAcess = UserAcess.RESET;
				}

				switch (userAcess) {
					case NONE:
						if (changedTime > 0) {
							abort = false;
						} else {
							abort = true;
						}
						break;
					case RESET:
						if (changedTime > 0) {
							this.solvis.clearCurrentImage();
							solvis.notifyScreenChangedByUserObserver(false);
							logger.info("User access finished");
						}
						changedTime = -1;
						abort = true;
						break;
					case DETECTED:
						this.solvis.clearCurrentImage();
						this.solvis.getCurrentImage();
						if (changedTime < 0) {
							solvis.notifyScreenChangedByUserObserver(true);
							logger.info("User access detected");
						}
						changedTime = time;
						abort = false;
						break;
				}

				this.solvis.getSolvisState().error(errorDetected);

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

	private synchronized void abort() {
		this.abort = true;
		this.notifyAll();
	}

	public synchronized void bufferNotEmpty() {
		this.notifyAll();
	}
}
