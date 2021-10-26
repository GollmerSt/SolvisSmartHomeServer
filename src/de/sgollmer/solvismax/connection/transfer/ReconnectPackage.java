/************************************************************************
 * 
 * $Id$
 *
 * 
 ************************************************************************/

package de.sgollmer.solvismax.connection.transfer;

import de.sgollmer.solvismax.connection.IReceivedData;
import de.sgollmer.solvismax.error.TypeException;
import de.sgollmer.solvismax.model.objects.data.SingleData;
import de.sgollmer.solvismax.model.objects.data.StringData;

public class ReconnectPackage extends JsonPackage implements IReceivedData {

	private Long clientId = null;

	ReconnectPackage() {
		this.command = Command.RECONNECT;
	}

	@Override
	void finish() throws TypeException {
		Frame f = this.data;
		if (f.elements.size() > 0) {
			Element e = f.elements.get(0);
			switch (e.name) {
				case "Id":
					if (e.value instanceof SingleValue) {
						this.clientId = ((SingleValue) e.value).getData().getLong();
					}
			}
		}
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
