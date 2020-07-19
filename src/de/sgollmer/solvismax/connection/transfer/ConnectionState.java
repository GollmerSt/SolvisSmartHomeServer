/************************************************************************
 * 
 * $Id$
 *
 * 
 ************************************************************************/

package de.sgollmer.solvismax.connection.transfer;

import de.sgollmer.solvismax.connection.ConnectionStatus;
import de.sgollmer.solvismax.connection.ISendData;

public class ConnectionState implements ISendData {

	private final ConnectionStatus status;
	private final String message;

	public ConnectionState(ConnectionStatus status, String message) {
		this.status = status;
		this.message = message;

	}

	public ConnectionState(ConnectionStatus status) {
		this(status, null);
	}

	@Override
	public JsonPackage createJsonPackage() {
		Frame frame = new Frame();
		Element element = new Element();
		frame.add(element);
		element.setName("State");
		element.setValue(new SingleValue(this.status.name()));
		if (this.message != null) {
			element = new Element();
			frame.add(element);
			element.setName("Message");
			element.setValue(new SingleValue(this.message));
		}
		return new JsonPackage(Command.CONNECTION_STATE, frame);

	}

	public ConnectionStatus getStatus() {
		return this.status;
	}

}
