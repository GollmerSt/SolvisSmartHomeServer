package de.sgollmer.solvismax.connection.mqtt;

import java.util.LinkedList;

import org.eclipse.paho.client.mqttv3.MqttException;

import de.sgollmer.solvismax.error.MqttConnectionLost;
import de.sgollmer.solvismax.log.LogManager;
import de.sgollmer.solvismax.log.LogManager.ILogger;
import de.sgollmer.solvismax.model.objects.Units.Unit;
import de.sgollmer.solvismax.helper.Helper.Runnable;

public class MqttQueue extends Runnable {

	private static final ILogger logger = LogManager.getInstance().getLogger(MqttQueue.class);

	private final LinkedList<MqttData> queue = new LinkedList<>();
	private final Mqtt mqtt;
	private boolean abort = false;

	public MqttQueue(Mqtt mqtt) {
		super("MqttQueue");
		this.mqtt = mqtt;
		this.submit();
	}

	public synchronized void publish(MqttData data) {
		if (data != null) {
			this.queue.add(data);
			this.notifyAll();
		}
	}

	@Override
	public void run() {

		while (!this.abort) {
			MqttData data = null;
			synchronized (this) {
				if (this.queue.isEmpty()) {
					try {
						this.notifyAll();
						this.wait();
					} catch (InterruptedException e) {
					}
				} else {
					data = this.queue.poll();
				}
			}

			if (data != null) {
				try {
					this.mqtt.publishRaw(data);
				} catch (MqttException e) {
					Unit unit = data.getUnit();
					String topic = this.mqtt.getTopic(data);
					if (unit == null) {
						logger.error("Error on mqtt publish <" + topic + ">:", e);
					} else {
						logger.error("Error on mqtt publish <" + topic + "> of unit <" + unit.getId() + ">:", e);
					}
				} catch (MqttConnectionLost e) {
					logger.debug("No MQTT connection publish <ResultStatus>");
				}
			}
		}
		this.notifyAll();

	}

	public void abort() {
		boolean finished = false;
		synchronized (this) {
			while (!finished) {
				if (this.queue.isEmpty()) {
					this.abort = true;
					this.notifyAll();
					finished = true;
				} else {
					try {
						this.wait();
					} catch (InterruptedException e) {
					}
				}
			}
		}

	}

}
