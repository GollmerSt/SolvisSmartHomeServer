/************************************************************************
 * 
 * $Id$
 *
 * 
 ************************************************************************/

package de.sgollmer.solvismax.connection;

import de.sgollmer.solvismax.model.Solvis;

public interface IClient {
	public void sendCommandError(final String message);

	public void send(final ISendData sendData);

	public void closeDelayed();

	public void close();

	public boolean identificationNecessary();

	public String getClientId();

	public Solvis getSolvis();
}
