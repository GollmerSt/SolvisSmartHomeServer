/************************************************************************
 * 
 * $Id$
 *
 * 
 ************************************************************************/

package de.sgollmer.solvismax.connection.transfer;

import de.sgollmer.solvismax.connection.ITransferedData;
import de.sgollmer.solvismax.model.objects.data.SingleData;
import de.sgollmer.solvismax.model.objects.data.StringData;

public class ReconnectPackage extends JsonPackage implements ITransferedData {

	private Integer clientId = null;

	public ReconnectPackage() {
		this.command = Command.RECONNECT;
	}

	@Override
	public void finish() {
		Frame f = this.data;
		if (f.elements.size() > 0) {
			Element e = f.elements.get(0);
			switch (e.name) {
				case "Id":
					if (e.value instanceof SingleValue) {
						this.clientId = ((SingleValue) e.value).getData().getInt();
					}
			}
		}
		this.data = null;
	}

	@Override
	public String getClientId() {
		return Integer.toString(this.clientId);
	}

	@Override
	public SingleData<?> getSingleData() {
		return new StringData(this.getClientId(), 0);
	}

}
