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
import de.sgollmer.solvismax.connection.ServerStatus;
import de.sgollmer.solvismax.crypt.CryptAes;
import de.sgollmer.solvismax.crypt.Ssl;
import de.sgollmer.solvismax.error.MqttConnectionLost;
import de.sgollmer.solvismax.error.MqttInterfaceException;
import de.sgollmer.solvismax.error.XmlException;
import de.sgollmer.solvismax.log.LogManager;
import de.sgollmer.solvismax.log.LogManager.ILogger;
import de.sgollmer.solvismax.model.Instances;
import de.sgollmer.solvismax.model.Solvis;
import de.sgollmer.solvismax.model.SolvisState;
import de.sgollmer.solvismax.model.WatchDog.HumanAccess;
import de.sgollmer.solvismax.model.objects.Observer.IObserver;
import de.sgollmer.solvismax.model.objects.Units.Unit;
import de.sgollmer.solvismax.model.objects.data.SolvisData;
import de.sgollmer.solvismax.xml.BaseCreator;
import de.sgollmer.solvismax.xml.CreatorByXML;

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
					try {
						this.passwordCrypt.decrypt(value);
					} catch (Throwable e) {
						throw new Error("Decrypt error", e);
					}
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

	class PublishChannelObserver implements IObserver<SolvisData> {

		private final Unit unit;

		PublishChannelObserver(Solvis solvis) {
			this.unit = solvis.getUnit();
		}

		@Override
		public void update(SolvisData data, Object source) {
			try {
				Mqtt.this.publish(data.getMqttData());
			} catch (MqttException e) {
				logger.error("Error on mqtt publish <" + data.getId() + "> of unit <" + this.unit.getId() + ">:", e);
			} catch (MqttConnectionLost e) {
				logger.debug("No MQTT connection publish <" + data.getId() + ">");
			}
		}
	}

	class PublishStatusObserver implements IObserver<SolvisState> {

		private final Unit unit;

		PublishStatusObserver(Solvis solvis) {
			this.unit = solvis.getUnit();
		}

		@Override
		public void update(SolvisState data, Object source) {
			try {
				Mqtt.this.publish(data.getMqttData());
			} catch (MqttException e) {
				logger.error("Error on mqtt publish <Status> of unit <" + this.unit.getId() + ">:", e);
			} catch (MqttConnectionLost e) {
				logger.debug("No MQTT connection publish <Status>");
			}
		}

	}

	class PublishHumanAccessObserver implements IObserver<HumanAccess> {

		private final Solvis solvis;

		PublishHumanAccessObserver(Solvis solvis) {
			this.solvis = solvis;
		}

		@Override
		public void update(HumanAccess data, Object source) {
			try {
				Mqtt.this.publish(data.getMqttData(this.solvis));
			} catch (MqttException e) {
				logger.error("Error on mqtt publish <HumannAccess> of unit <" + this.solvis.getUnit().getId() + ">:",
						e);
			} catch (MqttConnectionLost e) {
				logger.debug("No MQTT connection publish <HumannAccess>");
			}
		}

	}

	MqttCallbackExtended callback = new Callback(this);

	public synchronized void publish(MqttData data) throws MqttException, MqttConnectionLost {
		if (data == null) {
			return;
		}
		MqttMessage message = data.message;
		int qoS = message.getQos();
		if (qoS == 0) {
			message.setQos(this.publishQoS);
		}
		StringBuilder builder = new StringBuilder(this.topicPrefix);
		builder.append('/');
		builder.append(data.topicSuffix);
		String topic = builder.toString();
		if (this.client == null || !this.client.isConnected()) {
			logger.debug("Not connected, message not delivered");
			throw new MqttConnectionLost();
		}
		this.client.publish(topic, message);
		logger.debug("Messsage was sent to <" + topic + ">, data: " + message.toString());

	}

	void publishError(String clientId, String message) {
		try {
			this.publish(new MqttData(clientId + Constants.Mqtt.ERROR, message, 0, false));
		} catch (MqttException e) {
			logger.error("Can't deliver error message to iClient: " + message);
		} catch (MqttConnectionLost e) {
			logger.debug("No MQTT connection publish erro message");
		}
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

		if (this.client.isConnected()) {
			try {
				try {
					this.publish(this.getLastWill());
				} catch (MqttConnectionLost e) {
					logger.debug("Can't deliver value of last will at disconnection");
				}
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

	class Client implements IClient {
		private final String clientId;

		Client(String clientId) {
			this.clientId = clientId;
		}

		@Override
		public void sendCommandError(String message) {
			publishError(this.clientId, message);
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

	}

}
