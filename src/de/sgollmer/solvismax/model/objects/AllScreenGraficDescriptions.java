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

import de.sgollmer.solvismax.log.LogManager;
import de.sgollmer.solvismax.log.LogManager.ILogger;
import de.sgollmer.solvismax.model.objects.screen.ScreenGraficDescription;

public class AllScreenGraficDescriptions implements IAssigner {

	private static final ILogger logger = LogManager.getInstance().getLogger(AllScreenGraficDescriptions.class);

	private final Map<String, ScreenGraficDescription> screenGraficDescriptions = new HashMap<>();

	public void add(ScreenGraficDescription grafic) {
		ScreenGraficDescription former = this.screenGraficDescriptions.put(grafic.getId(), grafic);
		if (former != null) {
			logger.error("ScreenGraficDescription <" + grafic.getId() + "> is not unique.");
		}
	}

	void addAll(Collection<ScreenGraficDescription> grafics) {
		for (ScreenGraficDescription grafic : grafics) {
			this.add(grafic);
		}
	}

	public ScreenGraficDescription get(String id) {
		return this.screenGraficDescriptions.get(id);
	}

	@Override
	public void assign(SolvisDescription description) {
	}
}
