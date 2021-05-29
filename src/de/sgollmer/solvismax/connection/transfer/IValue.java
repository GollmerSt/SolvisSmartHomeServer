/************************************************************************
 * 
 * $Id$
 *
 * 
 ************************************************************************/

package de.sgollmer.solvismax.connection.transfer;

import de.sgollmer.solvismax.error.JsonException;

public interface IValue {
	public void addTo(final StringBuilder builder);

	public int from(final String json, final int position, final long timeStamp) throws JsonException;
}
