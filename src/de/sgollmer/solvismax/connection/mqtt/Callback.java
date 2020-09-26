/************************************************************************
 * 
 * $Id: Mqtt.java 277 2020-07-19 16:00:49Z stefa_000 $
 *
 * 
 ************************************************************************/

package de.sgollmer.solvismax.connection.mqtt;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Collection;

import org.eclipse.paho.client.mqttv3.IMqttDeliveryToken;
import org.eclipse.paho.client.mqttv3.MqttCallbackExtended;
import org.eclipse.paho.client.mqttv3.MqttException;
import org.eclipse.paho.client.mqttv3.MqttMessage;

import de.sgollmer.solvismax.connection.ServerCommand;
import de.sgollmer.solvismax.connection.ServerStatus;
import de.sgollmer.solvismax.error.ClientAssignmentException;
import de.sgollmer.solvismax.error.JsonException;
import de.sgollmer.solvismax.error.MqttConnectionLost;
import de.sgollmer.solvismax.error.MqttInterfaceException;
import de.sgollmer.solvismax.error.TypeException;
import de.sgollmer.solvismax.model.Solvis;
import de.sgollmer.solvismax.model.objects.data.BooleanValue;
import de.sgollmer.solvismax.model.objects.data.SingleData;
import de.sgollmer.solvismax.model.objects.data.SolvisData;
import de.sgollmer.solvismax.model.objects.data.StringData;

final class Callback implements MqttCallbackExtended {
	/**
	 * 
	 */
	private final Mqtt mqtt;

	/**
	 * @param mqtt
	 */
	Callback(Mqtt mqtt) {
		this.mqtt = mqtt;
	}

	@Override
	public void connectComplete(boolean reconnect, String serverURI) {
		Mqtt.logger.info("Connection to MQTT successfull");
		synchronized (this.mqtt) {
			try {
				if (!reconnect) {
					this.mqtt.publish(ServerCommand.getMqttMeta(null, true));
					for (Solvis solvis : this.mqtt.instances.getUnits()) {
						solvis.registerObserver(this.mqtt.new PublishChannelObserver(solvis));
						solvis.registerScreenChangedByHumanObserver(this.mqtt.new PublishHumanAccessObserver(solvis));
						solvis.getSolvisState().register(this.mqtt.new PublishStatusObserver(solvis));
						this.mqtt.publish(ServerCommand.getMqttMeta(solvis, false));
						solvis.getSolvisDescription().sendToMqtt(solvis, this.mqtt, false);
					}
				}
				for (Solvis solvis : this.mqtt.instances.getUnits()) {
					Collection<SolvisData> dates = solvis.getAllSolvisData().getMeasurements().cloneAndClear();
					for (SolvisData data : dates) {
						this.mqtt.publish(data.getMqttData());
					}
					this.mqtt.publish(solvis.getSolvisState().getState().getMqttData(solvis.getSolvisState()));
					this.mqtt.publish(solvis.getHumanAccess().getMqttData(solvis));
				}
				this.mqtt.publish(ServerStatus.ONLINE.getMqttData());
				Mqtt.logger.info("New MQTT connection handling successfull");
			} catch (MqttException e) {
				Mqtt.logger.error("Error: Mqtt exception occured", e);
			} catch (MqttConnectionLost e) {
				Mqtt.logger.error("Connection lost on reconnection");
			}
		}
	}

	@Override
	public void messageArrived(String topic, MqttMessage message) {
		SubscribeData subscribeData;
		try {
			subscribeData = this.mqtt.analyseReceivedTopic(topic);
		} catch (MqttInterfaceException e) {
			Mqtt.logger.error(e.getMessage());
			return;
		}
		Mqtt.logger.info("Topic <" + topic + "> with received message: " + message.toString());
		Solvis solvis = null;
		if (subscribeData.getUnitId() != null) {
			solvis = this.mqtt.instances.getUnit(subscribeData.getUnitId());
			if (solvis == null) {
				this.mqtt.publishError(subscribeData.getClientId(), "Solvis unit unknown.");
				return;
			}
			subscribeData.setSolvis(solvis);
		}
		String string = new String(message.getPayload(), StandardCharsets.UTF_8);
		SingleData<?> data = null;
		switch (subscribeData.type.format) {
			case BOOLEAN:
				data = new BooleanValue(Boolean.parseBoolean(string), 0);
				break;
			case STRING:
				data = new StringData(string, 0);
				break;
			case FROM_META:
				de.sgollmer.solvismax.model.objects.ChannelDescription description = solvis
						.getChannelDescription(subscribeData.getChannelId());
				try {
					data = description.interpretSetData(new StringData(string, 0));
					if (data == null) {
						this.mqtt.publishError(subscribeData.getClientId(),
								"Error: Channel <" + subscribeData.getChannelId() + "> not writable.");
						return;
					}
				} catch (TypeException e) {
					this.mqtt.publishError(subscribeData.getClientId(), "Error: Value error, value: " + string);
					return;
				}
		}
		subscribeData.setValue(data);

		try {
			if (subscribeData.getCommand() != null) {
				this.mqtt.commandHandler.commandFromClient(subscribeData,
						this.mqtt.new Client(subscribeData.getClientId()));
			}
		} catch (IOException | ClientAssignmentException | JsonException e) {
			Mqtt.logger.error("Error: On command handling", e);
		}
	}

	@Override
	public void deliveryComplete(IMqttDeliveryToken token) {
	}

	@Override
	public void connectionLost(Throwable cause) {
		Mqtt.logger.info("Connection to MQTT broker lost");
	}
}