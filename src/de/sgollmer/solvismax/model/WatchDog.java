package de.sgollmer.solvismax.model;

import java.io.IOException;
import java.util.Collection;

import org.slf4j.LoggerFactory;

import de.sgollmer.solvismax.imagepatternrecognition.image.MyImage;
import de.sgollmer.solvismax.model.objects.Screen;
import de.sgollmer.solvismax.model.objects.screen.ErrorScreen;
import de.sgollmer.solvismax.model.objects.screen.ScreenSaver;
import de.sgollmer.solvismax.objects.Rectangle;

public class WatchDog {

	private static final org.slf4j.Logger logger = LoggerFactory.getLogger(WatchDog.class);

	private final Solvis solvis;
	private final ScreenSaver saver;

	private final int releaseblockingAfterUserChange_ms;

	private long changedTime = -1;

	public WatchDog(Solvis solvis, ScreenSaver saver) {
		this.solvis = solvis;
		this.saver = saver;
		this.releaseblockingAfterUserChange_ms = this.solvis.getSolvisDescription().getMiscellaneous()
				.getReleaseblockingAfterUserChange_ms();
	}

	public boolean execute() {
		long time = System.currentTimeMillis();

		boolean changed = false;
		try {
			MyImage solvisImage = this.solvis.getRealImage();

			if (this.saver.is(solvisImage)) {
				this.solvis.setScreenSaverActive(true);
				changed = false;
				userAccessFinished();
			} else if (ErrorScreen.is(solvisImage)) {
				this.solvis.errorScreenDetected();
				changed = true;
			} else {
				this.solvis.setScreenSaverActive(false);
				changed = false;
				if (!solvisImage.equals(this.solvis.getCurrentImage())) {

					Screen screen = solvis.getSolvisDescription().getScreens().getScreen(solvisImage, solvis);

					if (screen != null && screen == solvis.getCurrentScreen()) {
						if (!screen.isIgnoreChanges()) {
							Collection<Rectangle> ignoreRectangles = screen.getIgnoreRectangles();
							if (ignoreRectangles != null) {
								MyImage ignoreRectScreen = new MyImage(solvisImage, false, ignoreRectangles);
								if (!ignoreRectScreen.equals(this.solvis.getCurrentImage(), true)) {
									changed = true;
								}
							}
						}
					} else {
						changed = true;
					}
				}
				if (changed) {
					this.solvis.clearCurrentImage();
					if (changedTime < 0) {
						logger.info("User access detected");
					}
					this.changedTime = time;
				}
			}
			if (!changed) {
				if (changedTime >= 0 && time > changedTime + this.releaseblockingAfterUserChange_ms) {
					userAccessFinished();
				}
			}
		} catch (

		Throwable e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return changed;
	}

	private void userAccessFinished() throws IOException, Throwable {
		if (this.changedTime >= 0) {
			solvis.notifyScreenChangedByUserObserver(this.solvis.getCurrentScreen());
			logger.info("User access finished");
		}
		this.changedTime = -1;

	}

}
