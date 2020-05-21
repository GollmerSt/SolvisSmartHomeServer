/************************************************************************
 * 
 * $Id$
 *
 * 
 ************************************************************************/

package de.sgollmer.solvismax.model.objects.screen;

import de.sgollmer.solvismax.imagepatternrecognition.image.MyImage;
import de.sgollmer.solvismax.model.Solvis;

public class SolvisScreen {
	private final Solvis solvis;
	private final MyImage image;
	private boolean scanned = false;
	private Screen screen = null;

	public SolvisScreen(MyImage image, Solvis solvis) {
		this.image = image;
		this.solvis = solvis;
	}

	public Screen get() {
		if (this.screen == null && !this.scanned) {
			this.screen = this.solvis.getSolvisDescription().getScreens().getScreen(this.image, this.solvis);
			this.scanned = true;
		}
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

	public static Screen get(SolvisScreen screen) {
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
}
