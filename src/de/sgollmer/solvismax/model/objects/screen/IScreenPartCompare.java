/************************************************************************
 * 
 * $Id$
 *
 * 
 ************************************************************************/

package de.sgollmer.solvismax.model.objects.screen;

import java.io.IOException;

import de.sgollmer.solvismax.error.TerminationException;
import de.sgollmer.solvismax.imagepatternrecognition.image.MyImage;
import de.sgollmer.solvismax.model.Solvis;

public interface IScreenPartCompare {
	public boolean isElementOf(MyImage image, Solvis solvis);

	public boolean isLearned(Solvis solvis);

	public void learn(Solvis solvis) throws IOException, TerminationException;
}
