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
import de.sgollmer.xmllibrary.XmlException;

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

	public ScreenGraficDescription get(String id) throws XmlException {
		ScreenGraficDescription description = this.screenGraficDescriptions.get(id);
		if ( description == null) {
			throw new XmlException( "ScreenGrafic of <" + id + "> not known, check the control.xml");
		}
		return description;
	}

	@Override
	public void assign(SolvisDescription description) {
	}
}
