package de.sgollmer.solvismax.model.objects;

import de.sgollmer.solvismax.model.Solvis;
import de.sgollmer.solvismax.model.objects.data.SolvisData;

public interface DataSourceI extends Assigner {

	public boolean getValue(SolvisData dest, Solvis solvis);

	public boolean setValue(Solvis solvis, SolvisData value);

	public boolean isWriteable();

	public boolean isAverage();

	public Integer getDivisor();

	public String getUnit();

	public void instantiate(Solvis solvis);
}
