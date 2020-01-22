/************************************************************************
 * 
 * $Id$
 *
 * 
 ************************************************************************/

package de.sgollmer.solvismax.model.objects;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

import de.sgollmer.solvismax.model.objects.screen.ScreenGraficDescription;

public class AllScreenGraficDescriptions implements Assigner {
	private final Map<String, ScreenGraficDescription> screenGraficDescriptions = new HashMap<>();

	public void add(ScreenGraficDescription grafic) {
		this.screenGraficDescriptions.put(grafic.getId(), grafic);
	}

	public void addAll(Collection<ScreenGraficDescription> grafics) {
		for (ScreenGraficDescription grafic : grafics) {
			this.screenGraficDescriptions.put(grafic.getId(), grafic);
		}
	}

	public ScreenGraficDescription get(String id) {
		return this.screenGraficDescriptions.get(id);
	}

	@Override
	public void assign(SolvisDescription description) {
		for ( ScreenGraficDescription grafic : this.screenGraficDescriptions.values() ) {
			grafic.assign(description);
		}
		
	}
}
