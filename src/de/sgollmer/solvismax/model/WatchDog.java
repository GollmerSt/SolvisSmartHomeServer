package de.sgollmer.solvismax.model;

import java.util.Collection;

import de.sgollmer.solvismax.imagepatternrecognition.image.MyImage;
import de.sgollmer.solvismax.model.objects.Screen;
import de.sgollmer.solvismax.model.objects.screen.ErrorScreen;
import de.sgollmer.solvismax.model.objects.screen.ScreenSaver;
import de.sgollmer.solvismax.objects.Rectangle;

public class WatchDog {

	private final Solvis solvis;
	private final ScreenSaver saver;

	private final int releaseblockingAfterUserChange_ms ;
	
	private long changedTime = -1;

	public WatchDog(Solvis solvis, ScreenSaver saver) {
		this.solvis = solvis;
		this.saver = saver;
		this.releaseblockingAfterUserChange_ms = this.solvis.getSolvisDescription().getMiscellaneous().getReleaseblockingAfterUserChange_ms();
	}

	public boolean execute() {
		long time = System.currentTimeMillis();

		boolean changed = false;
		try {
			MyImage solvisImage = this.solvis.getRealImage();
			
			if (this.saver.is(solvisImage)) {
				this.solvis.setScreenSaverActive(true);
			} else if ( ErrorScreen.is(solvisImage)){
				this.solvis.errorScreenDetected();
				changed = true;
			} else {
				this.solvis.setScreenSaverActive(false);
				changed = false ;
				if (!solvisImage.equals(this.solvis.getCurrentImage())) {
					
					Screen screen = solvis.getSolvisDescription().getScreens().getScreen(solvisImage, solvis);
					
					if ( screen != null && screen == solvis.getCurrentScreen() ) {
						Collection<Rectangle> ignoreRectangles = screen.getIgnoreRectangles() ;
						if ( ignoreRectangles != null ) {
							MyImage ingnoreRectScreen = new MyImage(solvisImage, false, ignoreRectangles) ;
							if ( ! ingnoreRectScreen.equals(this.solvis.getCurrentImage())) {
								changed = true ;
							}
						}
					} else {
						changed = true ;
					}
				}
				if ( changed) {
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
		} catch (Throwable e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return changed ;
	}

}
