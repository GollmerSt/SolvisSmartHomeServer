/************************************************************************
 * 
 * $Id$
 *
 * 
 ************************************************************************/

package de.sgollmer.solvismax.connection.transfer;

import de.sgollmer.solvismax.connection.ITransferedData;

public class GetPackage extends JsonPackage implements ITransferedData {
	GetPackage() {
		this.command = Command.GET;
	}

	private String id = null;

	@Override
	void finish() {
		Frame f = this.data;
		if (f.size() > 0) {
			Element e = f.get(0);
			this.id = e.name;
		}
		this.data = null;
	}

	@Override
	public String getChannelId() {
		return this.id;
	}

}
