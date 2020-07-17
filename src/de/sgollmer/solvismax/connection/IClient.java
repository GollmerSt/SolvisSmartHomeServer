package de.sgollmer.solvismax.connection;

import de.sgollmer.solvismax.connection.transfer.JsonPackage;

public interface IClient {
	public void sendCommandError( String message ) ;
	public void send(JsonPackage jsonPackage) ;
	public void closeDelayed();
}
