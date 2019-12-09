package de.sgollmer.solvismax.model.transfer;

import de.sgollmer.solvismax.error.JsonError;

public interface Value {
	public void addTo( StringBuilder builder) ;
	public int from(String json, int position) throws JsonError ;
}
