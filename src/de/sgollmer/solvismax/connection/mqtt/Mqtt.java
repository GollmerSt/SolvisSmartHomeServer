/************************************************************************
 * 
 * $Id$
 *
 * 
 ************************************************************************/

package de.sgollmer.solvismax.connection.mqtt;

import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.Collection;

import javax.xml.namespace.QName;

import org.eclipse.paho.client.mqttv3.MqttCallbackExtended;
import org.eclipse.paho.client.mqttv3.MqttClient;
import org.eclipse.paho.client.mqttv3.MqttException;
import org.eclipse.paho.client.mqttv3.MqttMessage;
import org.eclipse.paho.client.mqttv3.logging.LoggerFactory;
import org.eclipse.paho.client.mqttv3.persist.MemoryPersistence;

import de.sgollmer.solvismax.Constants;
import de.sgollmer.solvismax.connection.CommandHandler;
import de.sgollmer.solvismax.connection.IClient;
import de.sgollmer.solvismax.connection.ISendData;
import de.sgollmer.solvismax.connection.ServerCommand;
import de.sgollmer.solvismax.connection.ServerStatus;
import de.sgollmer.solvismax.crypt.CryptAes;
import de.sgollmer.solvismax.crypt.Ssl;
import de.sgollmer.solvismax.error.CryptDefaultValueException;
import de.sgollmer.solvismax.error.CryptExeception;
import de.sgollmer.solvismax.error.MqttConnectionLost;
import de.sgollmer.solvismax.error.MqttInterfaceException;
import de.sgollmer.solvismax.log.LogManager;
import de.sgollmer.solvismax.log.LogManager.DelayedMessage;
import de.sgollmer.solvismax.log.LogManager.ILogger;
import de.sgollmer.solvismax.log.LogManager.Level;
import de.sgollmer.solvismax.model.Instances;
import de.sgollmer.solvismax.model.Solvis;
import de.sgollmer.solvismax.model.objects.Observer.IObserver;
import de.sgollmer.solvismax.model.objects.Units.Unit;
import de.sgollmer.solvismax.model.objects.data.SolvisData;
import de.sgollmer.xmllibrary.BaseCreator;
import de.sgollmer.xmllibrary.CreatorByXML;
import de.sgollmer.xmllibrary.XmlException;

public class Mqtt {

	static final ILogger logger = LogManager.getInstance().getLogger(Mqtt.class);

	private static final String XML_SSL = "Ssl";
	static final Charset UTF_8 = StandardCharsets.UTF_8;

	MqttClient client = null;

	private final boolean enable;
	private final String brokerUrl;
	private final int port;
	final String userName;
	final CryptAes passwordCrypt;
	final String topicPrefix;
	private final String idPrefix;
	final int publishQoS;
	final int subscribeQoS;
	final Ssl ssl;

	Instances instances = null;
	CommandHandler commandHandler = null;
	private MqttThread mqttThread = null;
	private final MqttQueue mqttQueue;

	private Mqtt(boolean enable, String brokerUrl, int port, String userName, CryptAes passwordCrypt,
			String topicPrefix, String idPrefix, int publishQoS, int subscribeQoS, Ssl ssl) {
		this.enable = enable;
		this.brokerUrl = brokerUrl;
		this.port = port;
		this.userName = userName;
		this.passwordCrypt = passwordCrypt;
		this.topicPrefix = topicPrefix;
		this.idPrefix = idPrefix;
		this.publishQoS = publishQoS;
		this.subscribeQoS = subscribeQoS;
		this.ssl = ssl;
		this.mqttQueue = new MqttQueue(this);

		LoggerFactory.setLogger("de.sgollmer.solvismax.connection.mqtt.Logger");
	}

	public static class Creator extends CreatorByXML<Mqtt> {

		private boolean enable = false;
		private String brokerUrl;
		private int port;
		private String userName;
		private final CryptAes passwordCrypt = new CryptAes();
		private String topicPrefix;
		private String idPrefix;
		private int publishQoS;
		private int subscribeQoS;
		private Ssl ssl;

		public Creator(String id, BaseCreator<?> creator) {
			super(id, creator);
		}

		@Override
		public void setAttribute(QName name, String value) {
			try {
				switch (name.getLocalPart()) {
					case "enable":
						this.enable = Boolean.parseBoolean(value);
						break;
					case "brokerUrl":
						this.brokerUrl = value;
						break;
					case "port":
						this.port = Integer.parseInt(value);
						break;
					case "userName":
						this.userName = value;
						break;
					case "passwordCrypt":
						this.passwordCrypt.decrypt(value);
						break;
					case "topicPrefix":
						this.topicPrefix = value;
						break;
					case "idPrefix":
						this.idPrefix = value;
						break;
					case "publishQoS":
						this.publishQoS = Integer.parseInt(value);
						break;
					case "subscribeQoS":
						this.subscribeQoS = Integer.parseInt(value);
						break;
				}
			} catch (CryptDefaultValueException | CryptExeception e) {
				this.enable = false;
				String m = "base.xml error of passwordCrypt in Mqtt tag, MQTT disabled: " + e.getMessage();
				Level level = Level.ERROR;
				if (e instanceof CryptDefaultValueException) {
					level = Level.WARN;
				}
				LogManager.getInstance().addDelayedErrorMessage(new DelayedMessage(level, m, Mqtt.class, null));
			}

		}

		@Override
		public Mqtt create() throws XmlException, IOException {
			return new Mqtt(this.enable, this.brokerUrl, this.port, this.userName, this.passwordCrypt, this.topicPrefix,
					this.idPrefix, this.publishQoS, this.subscribeQoS, this.ssl);
		}

		@Override
		public CreatorByXML<?> getCreator(QName name) {
			String id = name.getLocalPart();
			switch (id) {
				case XML_SSL:
					return new Ssl.Creator(id, this.getBaseCreator());
			}
			return null;
		}

		@Override
		public void created(CreatorByXML<?> creator, Object created) {
			switch (creator.getId()) {
				case XML_SSL:
					this.ssl = (Ssl) created;
			}
		}

	}

	public void connect(Instances instances, CommandHandler commandHandler) throws MqttException {

		if (!this.enable) {
			return;
		}

		this.instances = instances;
		this.commandHandler = commandHandler;
		this.mqttThread = new MqttThread(this);

		if (this.client == null) {
			this.client = new MqttClient("tcp://" + this.brokerUrl + ":" + this.port, // URI
					this.idPrefix + "_" + MqttClient.generateClientId(), // ClientId
					new MemoryPersistence()); // Persistence
		}

		this.mqttThread.submit();
	}

	class PublishObserver implements IObserver<ISendData> {

		@Override
		public void update(ISendData sendData, Object source) {
			Mqtt.this.publish(sendData);
		}
	}

	class PublishStatusObserver implements IObserver<ISendData> {

		@Override
		public void update(ISendData data, Object source) {
			Mqtt.this.publish(data);
		}

	}

	MqttCallbackExtended callback = new Callback(this);

	public void unpublish(MqttData data) throws MqttException, MqttConnectionLost {
		if (data != null) {
			data = (MqttData) data.clone();
			data.prepareDeleteRetained();
			this.mqttQueue.publish(data);
		}
	}
	
	public void unpublish(ISendData sendData) throws MqttException, MqttConnectionLost {
		Collection<MqttData> collection = sendData.createMqttData();
		if (collection != null) {
			for (MqttData data : collection) {
				if (data != null) {
					this.unpublish(data);
				}
			}
		}
	}

	public void publish(MqttData data) {
		this.mqttQueue.publish(data);
	}

	public void publish(ISendData sendData) {
		Collection<MqttData> collection = sendData.createMqttData();
		if (collection != null) {
			for (MqttData data : collection) {
				if (data != null) {
					this.mqttQueue.publish(data);
				}
			}
		} else {
			logger.debug("Warning, \"ISendData\" object without data");
		}
	}

	public synchronized void publishRaw(MqttData data) throws MqttException, MqttConnectionLost {
		if (data == null) {
			return;
		}
		MqttMessage message = data.message;
		int qoS = message.getQos();
		if (qoS == 0) {
			message.setQos(this.publishQoS);
		}
		String topic = this.getTopic(data);
		if (this.client == null || !this.client.isConnected()) {
			logger.debug("Not connected, message not delivered");
			throw new MqttConnectionLost();
		}
		this.client.publish(topic, message);
		logger.debug("Messsage was sent to <" + topic + ">, data: " + message.toString());

	}

	public String getTopic(MqttData data) {
		StringBuilder builder = new StringBuilder(this.topicPrefix);
		builder.append('/');
		builder.append(data.topicSuffix);
		return builder.toString();
	}

	void publishError(String clientId, String message, Unit unit) {
		this.publish(new MqttData(clientId + Constants.Mqtt.ERROR, message, 0, false, unit));
	}

	MqttData getLastWill() {
		return ServerStatus.OFFLINE.getMqttData();

	}

	static enum Format {
		FROM_META, STRING, BOOLEAN, NONE
	}

	SubscribeData analyseReceivedTopic(String topic) throws MqttInterfaceException {
		if (!topic.startsWith(this.topicPrefix + '/')) {
			throw new MqttInterfaceException("Error: Wrong prefix of MQTT topic <" + topic + ">.");
		}
		String t = topic.substring(this.topicPrefix.length() + 1);

		String[] partsWoPrefix = t.split("/");

		if (partsWoPrefix.length < 1) {
			throw new MqttInterfaceException("Error: Wrong MQTT topic too short <" + topic + ">.");
		}
		String clientId = partsWoPrefix[0];

		SubscribeType type = SubscribeType.get(partsWoPrefix);
		if (type == null) {
			throw new MqttInterfaceException("Error: MQTT topic unknown <" + topic + ">.");
		}

		String unitId = null;

		if (type.hasUnitId()) {
			unitId = partsWoPrefix[1];
		}

		String channelId = null;

		if (type.hasChannelId()) {
			channelId = Mqtt.formatChannelIn(partsWoPrefix[2]);
		}

		return new SubscribeData(clientId, unitId, channelId, type);
	}

	public void abort() {
		if (this.mqttThread != null) {
			this.mqttThread.abort();
		}

		this.publish(this.getLastWill());
		this.mqttQueue.abort();

		if (this.client.isConnected()) {
			try {
				this.client.disconnect();
				this.client.close();
			} catch (MqttException e) {
			}
		}
	}

	private static String formatChannelIn(String mqttChannelId) {
		String channelId = mqttChannelId.replace(':', '.');
		return channelId;
	}

	public static String formatChannelOutTopic(String channelId) {
		String mqttChannelId = channelId.replace('.', ':');
		return '/' + mqttChannelId + Constants.Mqtt.DATA_SUFFIX;
	}

	public static String formatChannelMetaTopic(String channelId) {
		String mqttChannelId = channelId.replace('.', ':');
		return '/' + mqttChannelId + Constants.Mqtt.META_SUFFIX;
	}

	public static String formatServerMetaTopic() {
		return Constants.Mqtt.SERVER_PREFIX + Constants.Mqtt.META_SUFFIX;
	}

	public static String formatScreenMetaTopic() {
		return Constants.Mqtt.SCREEN_PREFIX + Constants.Mqtt.META_SUFFIX;
	}

	class Client implements IClient {
		private final String clientId;

		Client(String clientId) {
			this.clientId = clientId;
		}

		@Override
		public void sendCommandError(String message) {
			publishError(this.clientId, message, this.getSolvis().getUnit());
			logger.info(message);

		}

		@Override
		public void send(ISendData sendData) {
			logger.error("Unexpected using of iClient");
		}

		@Override
		public void closeDelayed() {
			logger.error("Unexpected using of iClient");
		}

		@Override
		public String getClientId() {
			return this.clientId;
		}

		@Override
		public Solvis getSolvis() {
			return null;
		}

		@Override
		public boolean equals(Object obj) {
			if (!(obj instanceof Client)) {
				return false;
			}
			if (this.clientId == null) {
				return false;
			}
			return this.clientId.equals(((Client) obj).getClientId());
		}

		@Override
		public int hashCode() {
			if (this.clientId == null) {
				return 263;
			}
			return this.clientId.hashCode();
		}

		@Override
		public void close() {
			logger.error("Unexpected using of iClient");
		}

	}

	public void deleteRetainedTopics() throws MqttException, MqttConnectionLost {
		for (Solvis solvis : this.instances.getUnits()) {
			this.unpublish(ServerCommand.getMqttMeta(solvis));
			solvis.getSolvisDescription().sendToMqtt(solvis, this, true);
			Collection<SolvisData> dates = solvis.getAllSolvisData().getMeasurements().cloneAndClear();
			for (SolvisData solvisData : dates) {
				this.unpublish(solvisData.getMqttData());
			}
			this.unpublish(solvis.getSolvisState().getSolvisStatePackage());
			this.unpublish(solvis.getHumanAccessPackage());
		}
		this.publish(ServerCommand.getMqttMeta(null));

	}
}
