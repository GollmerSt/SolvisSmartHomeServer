package de.sgollmer.solvismax.connection.mqtt;

import java.util.LinkedList;

import org.eclipse.paho.client.mqttv3.MqttException;

import de.sgollmer.solvismax.error.MqttConnectionLost;
import de.sgollmer.solvismax.log.LogManager;
import de.sgollmer.solvismax.log.LogManager.ILogger;
import de.sgollmer.solvismax.model.objects.unit.Unit;
import de.sgollmer.solvismax.helper.Helper.Runnable;

public class MqttQueue extends Runnable {

	private static final ILogger logger = LogManager.getInstance().getLogger(MqttQueue.class);

	private final LinkedList<MqttData> queue = new LinkedList<>();
	private final Mqtt mqtt;
	private boolean abort = false;
	private boolean finished = false;

	public MqttQueue(final Mqtt mqtt) {
		super("MqttQueue");
		this.mqtt = mqtt;
	}

	public synchronized void publish(final MqttData data) {
		if (data != null) {
			this.queue.add(data);
			this.notifyAll();
		}
	}

	@Override
	public void run() {

		try {

			while (true) {
				MqttData data = null;
				synchronized (this) {
					if (this.queue.isEmpty()) {
						if (this.abort) {
							break;
						}
						try {
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
		} catch (Throwable e) {
			logger.error("Unexpected throw occured.", e);
		}
		synchronized (this) {
			this.finished = true;
			this.notifyAll();
		}

	}

	public void abort() {
		synchronized (this) {
			while (!this.abort) {	//not necessary, workaround for FindBugs
				this.abort = true;
				if (!this.finished) {
					this.notifyAll();
					try {
						this.wait();
					} catch (InterruptedException e) {
					}
				}
			}
		}
	}

}
