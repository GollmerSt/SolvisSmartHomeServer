/************************************************************************
 * 
 * $Id: Mqtt.java 277 2020-07-19 16:00:49Z stefa_000 $
 *
 * 
 ************************************************************************/

package de.sgollmer.solvismax.connection.mqtt;

import org.eclipse.paho.client.mqttv3.MqttMessage;

import de.sgollmer.solvismax.model.Solvis;
import de.sgollmer.solvismax.model.objects.unit.Unit;

public class MqttData implements Cloneable {
	final String topicSuffix;
	final MqttMessage message;
	final Unit unit;

	private MqttData(final String topicSuffix, final byte[] payload, final int qoS, final boolean retained,
			final Unit unit) {
		this.topicSuffix = topicSuffix;
		this.message = new MqttMessage(payload);
		this.message.setQos(qoS);
		this.message.setRetained(retained);
		this.unit = unit;
	}

	@Override
	public MqttData clone() {
		return new MqttData(this.topicSuffix, this.message.getPayload(), this.message.getQos(),
				this.message.isRetained(), this.unit);

	}

	public MqttData(final String topicSuffix, final String utf8Data, final int qoS, final boolean retained,
			final Unit unit) {
		this(topicSuffix, utf8Data.getBytes(Mqtt.UTF_8), qoS, retained, unit);
	}

	public MqttData(final Solvis solvis, final String topicSuffix, final String utf8Data, final int qoS,
			final boolean retained) {
		this(solvis.getUnit().getId() + '/' + topicSuffix, utf8Data, qoS, retained, solvis.getUnit());
	}

	byte[] getPayLoad() {
		return this.message.getPayload();
	}

	int getQoS(final int defaultQoS) {
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

	public Unit getUnit() {
		return this.unit;
	}

	@Override
	public String toString() {
		return this.topicSuffix;
	}
}