/************************************************************************
 * 
 * $Id$
 *
 * 
 ************************************************************************/

package de.sgollmer.solvismax.model.objects.screen;

import java.io.IOException;

import de.sgollmer.solvismax.error.LearningError;
import de.sgollmer.solvismax.model.Solvis;

public interface IGraficsLearnable {
	public void learn( Solvis solvis) throws IOException, LearningError ;

}
