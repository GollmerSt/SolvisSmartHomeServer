/************************************************************************
 * 
 * $Id$
 *
 * 
 ************************************************************************/

package de.sgollmer.solvismax.connection.transfer;

import de.sgollmer.solvismax.error.JsonError;

public interface IValue {
	public void addTo(StringBuilder builder);

	public int from(String json, int position) throws JsonError;
}
