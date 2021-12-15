/************************************************************************
 * 
 * $Id$
 *
 * 
 ************************************************************************/

package de.sgollmer.solvismax.connection.transfer;

import de.sgollmer.solvismax.connection.IReceivedData;
import de.sgollmer.solvismax.error.PackageException;
import de.sgollmer.solvismax.error.TypeException;
import de.sgollmer.solvismax.model.objects.data.SingleData;
import de.sgollmer.solvismax.model.objects.data.StringData;

public class ReconnectPackage extends JsonPackage implements IReceivedData {

	private Long clientId = null;

	ReconnectPackage() {
		this.command = Command.RECONNECT;
	}

	@Override
	void finish() throws TypeException, PackageException {
		Frame f = this.data;
		Element e = f.get("Id");
		this.clientId = e.getValue().getSingleData().getLong();
		this.data = null;
	}

	@Override
	public String getClientId() {
		return Long.toString(this.clientId);
	}

	@Override
	public SingleData<?> getSingleData() {
		return new StringData(this.getClientId(), -1L);
	}

}
