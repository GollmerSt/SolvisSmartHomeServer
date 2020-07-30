package de.sgollmer.solvismax.model.objects.screen;

import de.sgollmer.solvismax.imagepatternrecognition.image.MyImage;
import de.sgollmer.solvismax.model.Solvis;
import de.sgollmer.solvismax.model.objects.OfConfigs;

public interface IScreen extends OfConfigs.IElement<IScreen> {

	public String getId();

	public boolean isScreen(MyImage image, Solvis solvis);

}
