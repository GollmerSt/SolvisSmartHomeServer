/************************************************************************
 * 
 * $Id$
 *
 * 
 ************************************************************************/

package de.sgollmer.solvismax.connection;

import de.sgollmer.solvismax.connection.transfer.Command;
import de.sgollmer.solvismax.model.Solvis;
import de.sgollmer.solvismax.model.objects.data.SingleData;

public interface IReceivedData {
	/**
	 * Get the assigned unit
	 * 
	 * @return assigned unit, null if server
	 */
	public void setSolvis(final Solvis solvis);

	public Solvis getSolvis();

	public String getClientId();

	public Command getCommand();

	public String getChannelId();

	public SingleData<?> getSingleData();
}
