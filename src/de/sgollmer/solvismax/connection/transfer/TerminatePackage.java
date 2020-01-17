/************************************************************************
 * 
 * $Id$
 *
 * 
 ************************************************************************/

package de.sgollmer.solvismax.connection.transfer;

public class TerminatePackage extends JsonPackage {
		
	public TerminatePackage() {
		this.command = Command.TERMINATE ;
		this.data = null ;
	}
	

}
