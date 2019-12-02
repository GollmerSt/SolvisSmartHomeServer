package de.sgollmer.solvismax.model.objects;

import de.sgollmer.solvismax.imagepatternrecognition.image.MyImage;
import de.sgollmer.solvismax.model.Solvis;

public interface ScreenCompare {
	public boolean isElementOf(MyImage image, Solvis solvis);
}
