/************************************************************************
 * 
 * $Id$
 *
 * 
 ************************************************************************/

package de.sgollmer.solvismax.connection;

import de.sgollmer.solvismax.model.Solvis;

public interface IClient {
	public void sendCommandError(String message);

	public void send(ISendData sendData);

	public void closeDelayed();

	public void close();

	public String getClientId();

	public Solvis getSolvis();
}
