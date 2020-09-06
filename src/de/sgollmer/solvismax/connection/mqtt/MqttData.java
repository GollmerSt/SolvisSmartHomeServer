/************************************************************************
 * 
 * $Id: Mqtt.java 277 2020-07-19 16:00:49Z stefa_000 $
 *
 * 
 ************************************************************************/

package de.sgollmer.solvismax.connection.mqtt;

import org.eclipse.paho.client.mqttv3.MqttMessage;

import de.sgollmer.solvismax.model.Solvis;

public class MqttData implements Cloneable {
	final String topicSuffix;
	final MqttMessage message;

	private MqttData(String topicSuffix, byte[] payload, int qoS, boolean retained) {
		this.topicSuffix = topicSuffix;
		this.message = new MqttMessage(payload);
		this.message.setQos(qoS);
		this.message.setRetained(retained);
	}

	@Override
	public MqttData clone() {
		return new MqttData(
				this.topicSuffix, this.message.getPayload(), this.message.getQos(), this.message.isRetained());

	}

	public MqttData(String topicSuffix, String utf8Data, int qoS, boolean retained) {
		this(topicSuffix, utf8Data.getBytes(Mqtt.UTF_8), qoS, retained);
	}

	public MqttData(Solvis solvis, String topicSuffix, String utf8Data, int qoS, boolean retained) {
		this(solvis.getUnit().getId() + topicSuffix, utf8Data, qoS, retained);
	}

	byte[] getPayLoad() {
		return this.message.getPayload();
	}

	int getQoS(int defaultQoS) {
		int qoS = this.message.getQos();
		if (qoS == 0) {
			return defaultQoS;
		} else {
			return qoS;
		}
	}

	boolean isRetained() {
		return this.message.isRetained();
	}

	public void prepareDeleteRetained() {
		this.message.setRetained(false);
	}
}