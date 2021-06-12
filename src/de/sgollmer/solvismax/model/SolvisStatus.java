package de.sgollmer.solvismax.model;

import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

import de.sgollmer.solvismax.Constants;

public enum SolvisStatus {
	POWER_OFF(Constants.Mqtt.STATUS), //
	REMOTE_CONNECTED(Constants.Mqtt.STATUS), //
	SOLVIS_CONNECTED(Constants.Mqtt.STATUS), //
	SOLVIS_DISCONNECTED(Constants.Mqtt.STATUS), //
	ERROR(Constants.Mqtt.STATUS), //
	USER_ACCESS_DETECTED(Constants.Mqtt.HUMAN_ACCESS), //
	SERVICE_ACCESS_DETECTED(Constants.Mqtt.HUMAN_ACCESS), //
	HUMAN_ACCESS_FINISHED(Constants.Mqtt.HUMAN_ACCESS), //
	CONTROL_WRITE_ONGOING(Constants.Mqtt.CONTROL),//
	CONTROL_READ_ONGOING(Constants.Mqtt.CONTROL),//
	CONTROL_MONITORING(Constants.Mqtt.CONTROL),//
	CONTROL_FINISHED(Constants.Mqtt.CONTROL),//
	UNDEFINED(Constants.Mqtt.STATUS);
	
	private final String mqttPrefix;

	private SolvisStatus(final String mqttPrefix) {
		this.mqttPrefix = mqttPrefix;

	}

	public String getMqttPrefix() {
		return this.mqttPrefix;
	}
	
	public static Collection< String > getMqttPrefixes() {
		Set<String> set = new HashSet<>();
		for ( SolvisStatus status: values()) {
			set.add(status.mqttPrefix);
		}
		return set;
	}
}