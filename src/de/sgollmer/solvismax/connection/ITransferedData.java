/************************************************************************
 * 
 * $Id: tefa_000 $
 *
 * 
 ************************************************************************/

package de.sgollmer.solvismax.connection;

import de.sgollmer.solvismax.connection.transfer.Command;
import de.sgollmer.solvismax.model.Solvis;
import de.sgollmer.solvismax.model.objects.data.SingleData;

public interface ITransferedData {
	/**
	 * Get the assigned unit
	 * 
	 * @return assigned unit, null if server
	 */
	public void setSolvis(Solvis solvis);

	public Solvis getSolvis();

	public String getClientId();

	public Command getCommand();

	public String getChannelId();

	public SingleData<?> getSingleData();
}
