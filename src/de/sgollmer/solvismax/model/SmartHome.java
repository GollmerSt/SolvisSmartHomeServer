package de.sgollmer.solvismax.model;

import java.util.Collection;

import de.sgollmer.solvismax.model.objects.data.SolvisData;

public interface SmartHome {

	public void send( Collection<SolvisData> datas, Solvis solvis  ) ;

}
