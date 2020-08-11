/************************************************************************
 * 
 * $Id$
 *
 * 
 ************************************************************************/

package de.sgollmer.solvismax.model.objects.screen;

import java.io.File;
import java.io.IOException;

import de.sgollmer.solvismax.Constants;
import de.sgollmer.solvismax.error.FatalError;
import de.sgollmer.solvismax.helper.FileHelper;
import de.sgollmer.solvismax.imagepatternrecognition.image.MyImage;
import de.sgollmer.solvismax.log.LogManager;
import de.sgollmer.solvismax.log.LogManager.ILogger;
import de.sgollmer.solvismax.model.Solvis;

public class SolvisScreen {
	private static final ILogger logger = LogManager.getInstance().getLogger(SolvisScreen.class);

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
			this.screen = this.solvis.getSolvisDescription().getScreens().getScreen(this.image, this.solvis);
			this.scanned = true;
		}
		if (this.screen != null && !this.screen.isScreen()) {
			throw new FatalError("Only an object of type Screen is allowed.");
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

	public void writeLearningImage(String id) {
		File parent = new File(this.solvis.getWritePath(), Constants.Files.RESOURCE_DESTINATION);
		parent = new File(parent, Constants.Files.LEARN_DESTINATION);
		String baseName = this.solvis.getUnit().getId() + "__" + id + "__";
		int cnt = 0;
		boolean found = true;
		File file = null;
		while (found) {
			String name = FileHelper.makeOSCompatible( baseName + Integer.toString(cnt) + ".png");
			file = new File(parent, name);
			found = file.exists();
			++cnt;
		}
		try {
			this.image.writeWhole(file);
		} catch (IOException e) {
			logger.error("Error on writing the image of the learned screen <" + id + ">.");
		}

	}
}
