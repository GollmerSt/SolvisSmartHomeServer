/************************************************************************
 * 
 * $Id: DisconnectPackage.java 81 2020-01-04 21:05:15Z stefa $
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
