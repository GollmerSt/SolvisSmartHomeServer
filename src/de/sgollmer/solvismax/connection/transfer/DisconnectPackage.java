package de.sgollmer.solvismax.connection.transfer;

public class DisconnectPackage extends JsonPackage {
		
	public DisconnectPackage() {
		this.command = Command.DISCONNECT ;
		this.data = null ;
	}
	

}
