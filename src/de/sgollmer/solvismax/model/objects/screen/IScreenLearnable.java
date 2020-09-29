/************************************************************************
 * 
 * $Id$
 *
 * 
 ************************************************************************/

package de.sgollmer.solvismax.model.objects.screen;

import java.util.Collection;

import de.sgollmer.solvismax.model.Solvis;

public interface IScreenLearnable {

	public void addLearnScreenGrafics(Collection<IScreenPartCompare> learnGrafics, Solvis solvis);

}
