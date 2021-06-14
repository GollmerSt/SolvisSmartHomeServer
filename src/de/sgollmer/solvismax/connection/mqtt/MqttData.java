/************************************************************************
 * 
 * $Id: Mqtt.java 277 2020-07-19 16:00:49Z stefa_000 $
 *
 * 
 ************************************************************************/

package de.sgollmer.solvismax.connection.mqtt;

import org.eclipse.paho.client.mqttv3.MqttMessage;

import de.sgollmer.solvismax.helper.Helper;
import de.sgollmer.solvismax.model.Solvis;
import de.sgollmer.solvismax.model.objects.unit.Unit;

public class MqttData implements Cloneable {
	private final TopicType topicType;
	private final Solvis solvis;
	private final String channelId;
	final MqttMessage message;

	private MqttData(final TopicType topicType, final Solvis solvis, final String channelId, final byte[] payload,
			final int qoS, final boolean retained) {
		this.topicType = topicType;
		this.solvis = solvis;
		this.channelId = channelId;
		this.message = new MqttMessage(payload);
		this.message.setQos(qoS);
		this.message.setRetained(retained);
	}

	@Override
	public MqttData clone() {
		return new MqttData(this.topicType, this.solvis, this.channelId, this.message.getPayload(),
				this.message.getQos(), this.message.isRetained());

	}

	public MqttData(final TopicType topicType, final Solvis solvis, final String channelId, final String utf8Data,
			final int qoS, final boolean retained) {
		this(topicType, solvis, channelId, utf8Data.getBytes(Mqtt.UTF_8), qoS, retained);
	}

//	public MqttData(final Solvis solvis, final String topicSuffix, final String utf8Data, final int qoS,
//			final boolean retained) {
//		this(solvis.getUnit().getId() + '/' + topicSuffix, utf8Data, qoS, retained, solvis.getUnit());
//	}
//
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
		if (this.solvis == null) {
			return null;
		} else {
			return this.solvis.getUnit();
		}
	}

	@Override
	public String toString() {
		StringBuilder builder = new StringBuilder("TopicType: ");
		builder.append(this.topicType.name());
		if (this.solvis != null) {
			builder.append(", Unit: ");
			builder.append(this.solvis.getUnit().getId());
		}
		if (this.channelId != null) {
			builder.append(", ChannelId: ");
			builder.append(this.channelId);
		}

		return builder.toString();
	}

	String getTopic(Mqtt mqtt) {
		String[] parts = this.topicType.getTopicParts(mqtt, this.solvis, this.channelId);
		return Helper.cat(parts, "/");
	}
}