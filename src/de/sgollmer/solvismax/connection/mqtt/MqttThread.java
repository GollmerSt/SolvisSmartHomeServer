/************************************************************************
 * 
 * $Id: Mqtt.java 277 2020-07-19 16:00:49Z stefa_000 $
 *
 * 
 ************************************************************************/

package de.sgollmer.solvismax.connection.mqtt;

import javax.net.ssl.SSLSocketFactory;

import org.eclipse.paho.client.mqttv3.MqttConnectOptions;
import org.eclipse.paho.client.mqttv3.MqttException;

import de.sgollmer.solvismax.Constants;
import de.sgollmer.solvismax.helper.Helper;

public class MqttThread extends Helper.Runnable {

	/**
	 * 
	 */
	private final Mqtt mqtt;
	private boolean abort = false;

	MqttThread(final Mqtt mqtt) {
		super("Mqtt");
		this.mqtt = mqtt;
	}

	@Override
	public void run() {

		if (!this.mqtt.client.isConnected()) {
			MqttConnectOptions options = new MqttConnectOptions();
			if (this.mqtt.userName != null && this.mqtt.passwordCrypt != null) {
				options.setUserName(this.mqtt.userName);
				options.setPassword(this.mqtt.passwordCrypt.cP());
			}
			options.setAutomaticReconnect(true);
			options.setCleanSession(false);
			if (this.mqtt.ssl != null) {
				SSLSocketFactory sslSocketFactory = this.mqtt.ssl.getSocketFactory();
				options.setSocketFactory(sslSocketFactory);
			}
			MqttData lastWill = this.mqtt.getLastWill();
			String topic = this.mqtt.topicPrefix + '/' + lastWill.topicSuffix;
			options.setWill(topic, lastWill.getPayLoad(), lastWill.getQoS(this.mqtt.publishQoS), lastWill.isRetained());
			options.setMaxInflight(Constants.Mqtt.MAX_INFLIGHT);
			options.setAutomaticReconnect(true);
			this.mqtt.client.setCallback(this.mqtt.callback);
			int length = Constants.Mqtt.CMND_SUFFIXES.length;
			String[] topicFilters = new String[length];
			int[] qoSs = new int[length];
			for (int i = 0; i < length; ++i) {
				topicFilters[i] = this.mqtt.topicPrefix + Constants.Mqtt.CMND_SUFFIXES[i];
				qoSs[i] = this.mqtt.subscribeQoS;
			}

			boolean connected = false;
			boolean subscribed = false;
			int waitTime = Constants.Mqtt.MIN_CONNECTION_REPEAT_TIME;
			while ((!connected || !subscribed) && !this.abort) {
				try {
					this.mqtt.client.connect(options);
					connected = true;
					this.mqtt.client.subscribe(topicFilters, qoSs);
					subscribed = true;
				} catch (MqttException e) {
					if (!connected) {
						Mqtt.logger.info("Mqtt broker not available, will be retried in " + waitTime / 1000 + " s.");
					} else if (!subscribed) {
						Mqtt.logger.error("Error on subscription, will be retried in " + waitTime / 1000 + " s.");
						try {
							synchronized (this.mqtt) {
								this.mqtt.client.disconnect();
							}
						} catch (MqttException e1) {
						}
					}
					synchronized (this) {
						if (!this.abort) {
							try {
								this.wait(waitTime);
							} catch (InterruptedException e1) {
							}
							waitTime *= 2;
							if (waitTime > Constants.Mqtt.MAX_CONNECTION_REPEAT_TIME) {
								waitTime = Constants.Mqtt.MAX_CONNECTION_REPEAT_TIME;
							}
						}
					}
				}
			}
		}

	}

	synchronized void abort() {
		this.abort = true;
		this.notifyAll();
	}
}