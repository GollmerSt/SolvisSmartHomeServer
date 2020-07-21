/************************************************************************
 * 
 * $Id$
 *
 * 
 ************************************************************************/

package de.sgollmer.solvismax.model.objects.screen;

import de.sgollmer.solvismax.model.objects.Preparation;

public class History {
	private Preparation lastPreparation;

	public void set(Preparation preparation) {
		this.lastPreparation = preparation;
	}

	Preparation getLastPreparation() {
		return this.lastPreparation;
	}
}
