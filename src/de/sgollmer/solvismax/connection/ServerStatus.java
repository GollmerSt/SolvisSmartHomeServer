/************************************************************************
 * 
 * $Id$
 *
 * 
 ************************************************************************/

package de.sgollmer.solvismax.connection;

import de.sgollmer.solvismax.connection.mqtt.MqttData;
import de.sgollmer.solvismax.connection.mqtt.TopicType;

public enum ServerStatus {
	ONLINE(true), OFFLINE(false);

	private final boolean online;

	private ServerStatus(final boolean online) {
		this.online = online;
	}

	public MqttData getMqttData() {
		return new MqttData(TopicType.SERVER_ONLINE, null, null, Boolean.toString(this.online), 0, true);
	}
}
