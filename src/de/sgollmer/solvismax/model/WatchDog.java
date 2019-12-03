package de.sgollmer.solvismax.model;

import java.io.IOException;

import de.sgollmer.solvismax.imagepatternrecognition.image.MyImage;
import de.sgollmer.solvismax.model.objects.screen.ErrorScreen;
import de.sgollmer.solvismax.model.objects.screen.ScreenSaver;

public class WatchDog {

	private final Solvis solvis;
	private final ScreenSaver saver;
	private final ErrorScreen errorScreen ;

	private final int releaseblockingAfterUserChange_ms ;
	
	private long changedTime = -1;

	public WatchDog(Solvis solvis, ScreenSaver saver) {
		this.solvis = solvis;
		this.saver = saver;
		this.errorScreen = new ErrorScreen() ;
		this.releaseblockingAfterUserChange_ms = this.solvis.getSolvisDescription().getMiscellaneous().getReleaseblockingAfterUserChange_ms();
	}

	public boolean execute() {
		long time = System.currentTimeMillis();

		boolean changed = false;
		try {
			MyImage solvisImage = this.solvis.getRealImage();

			if (this.saver.is(solvisImage)) {
				this.solvis.setScreenSaverActive(true);
			} else if ( errorScreen.is(solvisImage)){
				this.solvis.errorScreenDetected();
				changed = true;
			} else {
				this.solvis.setScreenSaverActive(false);
				if (!solvisImage.equals(this.solvis.getCurrentImage())) {
					this.solvis.clearCurrentImage();
					this.changedTime = time;
					changed = true;
				}
			}
			if (!changed) {
				if (changedTime >= 0 && time > changedTime + this.releaseblockingAfterUserChange_ms) {
					this.changedTime = - 1 ;
					solvis.notifyScreenChangedByUserObserver(this.solvis.getCurrentScreen());
				}
			}
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return changed ;
	}

}
