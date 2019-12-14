package de.sgollmer.solvismax.connection;

import de.sgollmer.solvismax.connection.transfer.Command;
import de.sgollmer.solvismax.connection.transfer.Element;
import de.sgollmer.solvismax.connection.transfer.Frame;
import de.sgollmer.solvismax.connection.transfer.JsonPackage;
import de.sgollmer.solvismax.connection.transfer.SingleValue;

public class ConnectionState {

	private final ConnectionStatus status;
	private final String message;

	public ConnectionState(ConnectionStatus status, String message) {
		this.status = status;
		this.message = message;

	}

	public ConnectionState(ConnectionStatus status) {
		this(status, null);
	}

	public JsonPackage createJsonPackage() {
		Frame frame = new Frame();
		Element element = new Element();
		frame.add(element);
		element.setName("State");
		element.setValue(new SingleValue(status.name()));
		if (message != null) {
			element = new Element();
			frame.add(element);
			element.setName("Message");
			element.setValue(new SingleValue(message));
		}
		return new JsonPackage(Command.CONNECTION_STATE, frame);

	}

	public ConnectionStatus getStatus() {
		return status;
	}

}
