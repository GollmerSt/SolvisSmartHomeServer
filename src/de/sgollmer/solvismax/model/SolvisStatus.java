package de.sgollmer.solvismax.model;

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
	UNDEFINED(Constants.Mqtt.STATUS);
	
	private final String mqttPrefix;
	
	private SolvisStatus( String mqttPrefix ) { 
		this.mqttPrefix = mqttPrefix;
		
	}

	public String getMqttPrefix() {
		return this.mqttPrefix;
	}
}