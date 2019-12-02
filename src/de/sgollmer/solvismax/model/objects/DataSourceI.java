package de.sgollmer.solvismax.model.objects;

import java.io.IOException;

import de.sgollmer.solvismax.model.Solvis;
import de.sgollmer.solvismax.model.objects.data.SolvisData;

public interface DataSourceI extends Assigner, GraficsLearnable {

	public boolean getValue(SolvisData dest, Solvis solvis) throws IOException;

	public boolean setValue(Solvis solvis, SolvisData value) throws IOException;

	public boolean isWriteable();

	public boolean isAverage();

	public Integer getDivisor();

	public String getUnit();

	public void instantiate(Solvis solvis);
	
	public Type getType() ;
	
	public enum Type {
		CONTROL, CALCULATION, MEASUREMENT
	}
	
	public Screen getScreen() ;

}
