package de.sgollmer.solvismax.model;

import java.util.Collection;

import org.slf4j.LoggerFactory;

import de.sgollmer.solvismax.Constants;
import de.sgollmer.solvismax.imagepatternrecognition.image.MyImage;
import de.sgollmer.solvismax.model.objects.Miscellaneous;
import de.sgollmer.solvismax.model.objects.Screen;
import de.sgollmer.solvismax.model.objects.screen.ErrorScreen;
import de.sgollmer.solvismax.model.objects.screen.ScreenSaver;
import de.sgollmer.solvismax.objects.Rectangle;

public class WatchDog {

	private static final org.slf4j.Logger logger = LoggerFactory.getLogger(WatchDog.class);

	private final Solvis solvis;
	private final ScreenSaver saver;

	private final int releaseblockingAfterUserChange_ms;
	private final int watchDogTime;
	private boolean finished = false;

	public WatchDog(Solvis solvis, ScreenSaver saver) {
		this.solvis = solvis;
		this.saver = saver;
		Miscellaneous misc = this.solvis.getSolvisDescription().getMiscellaneous();
		int releaseBlocking = misc.getReleaseblockingAfterUserChange_ms();
		if ( Constants.DEBUG ) {
			releaseBlocking = Constants.DEBUG_USER_ACCESS_TIME ;
		}
		this.releaseblockingAfterUserChange_ms = releaseBlocking;
		this.watchDogTime = misc.getWatchDogTime_ms();
	}

	private enum UserAcess {
		DETECTED, RESET, NONE
	}

	public void execute() {

		long changedTime = -1;
		this.finished = false ;

		while (!finished) {

			long time = System.currentTimeMillis();

			UserAcess userAcess = UserAcess.NONE;
			boolean errorDetected = false;

			try {
				MyImage solvisImage = this.solvis.getRealImage();

				if (this.saver.is(solvisImage)) {
					this.solvis.setScreenSaverActive(true);
					userAcess = UserAcess.RESET;
				} else {
					this.solvis.setScreenSaverActive(false);
					if (ErrorScreen.is(solvisImage)) {
						errorDetected = true;
						userAcess = UserAcess.RESET;
					} else {
						this.solvis.setScreenSaverActive(false);
						if (!solvisImage.equals(this.solvis.getCurrentImage())) {

							Screen screen = solvis.getSolvisDescription().getScreens().getScreen(solvisImage, solvis);

							if (screen != null && screen == solvis.getCurrentScreen()) {
								if (!screen.isIgnoreChanges()) {
									Collection<Rectangle> ignoreRectangles = screen.getIgnoreRectangles();
									if (ignoreRectangles != null) {
										MyImage ignoreRectScreen = new MyImage(solvisImage, false, ignoreRectangles);
										if (!ignoreRectScreen.equals(this.solvis.getCurrentImage(), true)) {
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

				if (userAcess != UserAcess.DETECTED && changedTime >= 0
						&& time > changedTime + this.releaseblockingAfterUserChange_ms) {
					userAcess = UserAcess.RESET;
				}

				switch (userAcess) {
					case NONE:
						if (changedTime > 0) {
							finished = false;
						} else {
							finished = true;
						}
						break;
					case RESET:
						if (changedTime > 0) {
							this.solvis.clearCurrentImage();
							solvis.notifyScreenChangedByUserObserver(false);
							logger.info("User access finished");
						}
						changedTime = -1;
						finished = true;
						break;
					case DETECTED:
						this.solvis.clearCurrentImage();
						this.solvis.getCurrentImage();
						if (changedTime < 0) {
							solvis.notifyScreenChangedByUserObserver(true);
							logger.info("User access detected");
						}
						changedTime = time;
						finished = false;
						break;
				}
				
				this.solvis.getSolvisState().error(errorDetected);

				if (errorDetected) {
					finished = false;
				}
				
				synchronized (this) {
					if (!finished) {
						this.wait(this.watchDogTime);
					}
				}

			} catch (

			Throwable e) {
			}
		}
	}

	public synchronized void terminate() {
		this.finished = true;
		this.notifyAll();
	}
	
	public synchronized void bufferNotEmpty() {
		this.notifyAll();
	}
}
