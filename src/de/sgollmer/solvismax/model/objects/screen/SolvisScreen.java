/************************************************************************
 * 
 * $Id$
 *
 * 
 ************************************************************************/

package de.sgollmer.solvismax.model.objects.screen;

import java.util.Set;

import org.tinylog.Logger;

import de.sgollmer.solvismax.BaseData;
import de.sgollmer.solvismax.error.FatalError;
import de.sgollmer.solvismax.imagepatternrecognition.image.MyImage;
import de.sgollmer.solvismax.model.Solvis;
import de.sgollmer.solvismax.objects.Rectangle;

/**
 * 
 * @author stefa
 * 
 *         This class contains the (current) image of the SolvisControl with the
 *         assigned screen instance, if this has already been scanned.
 *
 */
public class SolvisScreen {
	// private static final ILogger logger =
	// LogManager.getInstance().getLogger(SolvisScreen.class);

	private final Solvis solvis;
	private final MyImage image;
	private boolean scanned = false;
	private AbstractScreen screen = null;

	public SolvisScreen(MyImage image, Solvis solvis) {
		this.solvis = solvis;
		this.image = image;
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
		if (screen == null) {
			return false;
		} else {
			return screen.isService();
		}
	}

	public boolean equalsWoIgnore(final SolvisScreen cmp) {

		if (this.imagesEquals(cmp)) {
			return true;
		}

		AbstractScreen screen = this.get();

		if (screen == null) {
			return false;
		}

		if (screen != SolvisScreen.get(cmp)) {
			return false;
		}

		if (screen.isIgnoreChanges()) {
			return true;
		}

		Set<Rectangle> ignoreRectangles = screen.getIgnoreRectangles();
		if (ignoreRectangles == null) {
			return false;
		}

		MyImage ignoreRectScreen = new MyImage(this.getImage(), false, ignoreRectangles);

		if (ignoreRectScreen.equals(cmp.getImage(), true)) {
			return true;
		} else {

			return false;
		}

	}
	
	public boolean isHomeScreen() {
		return this.solvis.getHomeScreen() == this.get(); 
	}

}
