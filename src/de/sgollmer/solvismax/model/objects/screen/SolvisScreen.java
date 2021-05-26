/************************************************************************
 * 
 * $Id$
 *
 * 
 ************************************************************************/

package de.sgollmer.solvismax.model.objects.screen;

import org.tinylog.Logger;

import de.sgollmer.solvismax.BaseData;
import de.sgollmer.solvismax.error.FatalError;
import de.sgollmer.solvismax.imagepatternrecognition.image.MyImage;
import de.sgollmer.solvismax.model.Solvis;

public class SolvisScreen {
	// private static final ILogger logger =
	// LogManager.getInstance().getLogger(SolvisScreen.class);

	private final Solvis solvis;
	private final MyImage image;
	private boolean scanned = false;
	private AbstractScreen screen = null;

	public SolvisScreen(MyImage image, Solvis solvis) {
		this.image = image;
		this.solvis = solvis;
	}

	public AbstractScreen get() {
		if (this.screen == null && !this.scanned) {

			AbstractScreen previousScreen = this.solvis.getPreviousScreen();

			if (previousScreen != null) {

				this.screen = previousScreen.getSurroundScreen(this.image, this.solvis);
			}
			if (this.screen == null) {
				this.screen = this.solvis.getSolvisDescription().getScreens().getScreen(this.image, this.solvis);

				if (BaseData.DEBUG && this.screen != null && previousScreen != null) {
					Logger.error("Warning: Error within the xml file? Screen <" + this.screen
							+ "> not found arround the previous screen <" + previousScreen + ">.");
				}
			}
			this.scanned = true;
		}
		if (this.screen != null && !this.screen.isScreen()) {
			throw new FatalError("Only an object of type Screen is allowed.");
		}
		this.solvis.setPreviousScreen(this.screen);
		return this.screen;
	}

	public Solvis getSolvis() {
		return this.solvis;
	}

	public MyImage getImage() {
		return this.image;
	}

	public void forceScreen(Screen screen) {
		this.screen = screen;
		this.scanned = true;
	}

	public static AbstractScreen get(SolvisScreen screen) {
		if (screen == null) {
			return null;
		}
		return screen.get();
	}

	public static MyImage getImage(SolvisScreen screen) {
		if (screen == null) {
			return null;
		}
		return screen.getImage();
	}

	public boolean imagesEquals(SolvisScreen screen) {
		return this.image.equals(SolvisScreen.getImage(screen));
	}
	
	public boolean isService() {
		AbstractScreen screen = this.get();
		if ( screen == null ) {
			return false;
		} else {
			return screen.isService();
		}
	}

}
