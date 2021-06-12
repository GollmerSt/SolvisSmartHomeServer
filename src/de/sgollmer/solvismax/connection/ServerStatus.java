/************************************************************************
 * 
 * $Id$
 *
 * 
 ************************************************************************/

package de.sgollmer.solvismax.connection;

import de.sgollmer.solvismax.Constants;
import de.sgollmer.solvismax.connection.mqtt.MqttData;

public enum ServerStatus {
	ONLINE(true), OFFLINE(false);

	private final boolean online;

	private ServerStatus(final boolean online) {
		this.online = online;
	}

	public MqttData getMqttData() {
		return new MqttData(Constants.Mqtt.SERVER + '/' + Constants.Mqtt.ONLINE_STATUS,
				Boolean.toString(this.online), 0, true, null);
	}
}
