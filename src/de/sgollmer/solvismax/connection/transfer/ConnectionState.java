/************************************************************************
 * 
 * $Id$
 *
 * 
 ************************************************************************/

package de.sgollmer.solvismax.connection.transfer;

import java.util.ArrayList;
import java.util.Collection;

import de.sgollmer.solvismax.connection.ConnectionStatus;
import de.sgollmer.solvismax.connection.ISendData;
import de.sgollmer.solvismax.connection.mqtt.MqttData;

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
		Element element = new Element("State", new SingleValue(this.status.name()));
		frame.add(element);
		if (this.message != null) {
			element = new Element("Message", new SingleValue(this.message));
			frame.add(element);
		}
		return new JsonPackage(Command.CONNECTION_STATE, frame);

	}

	@Override
	public Collection<MqttData> createMqttData() {
		return new ArrayList<MqttData>();
	}

}
