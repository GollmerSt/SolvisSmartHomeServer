/************************************************************************
 * 
 * $Id$
 *
 * 
 ************************************************************************/

package de.sgollmer.solvismax.connection.transfer;

public class GetPackage extends JsonPackage {
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
		return id;
	}
}
