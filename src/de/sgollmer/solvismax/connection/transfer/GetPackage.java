/************************************************************************
 * 
 * $Id$
 *
 * 
 ************************************************************************/

package de.sgollmer.solvismax.connection.transfer;

import de.sgollmer.solvismax.connection.IReceivedData;

public class GetPackage extends JsonPackage implements IReceivedData {
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
