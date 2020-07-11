/************************************************************************
 * 
 * $Id$
 *
 * 
 ************************************************************************/

package de.sgollmer.solvismax.connection.transfer;

import de.sgollmer.solvismax.connection.ITransferedData;

public class GetPackage extends JsonPackage implements ITransferedData{
	public GetPackage() {
		this.command = Command.GET;
	}
	
	private String id = null ;
	
	@Override
	public void finish() {
		Frame f = this.data ;
		if (f.size() > 0) {
			Element e = f.get(0) ;
			this.id = e.name ;
		}
		this.data = null ;
	}

	public String getId() {
		return this.id;
	}

	@Override
	public String getChannelId() {
		return this.getId();
	}

}
