/************************************************************************
 * 
 * $Id: BaseData.java 266 2020-07-11 10:34:15Z stefa_000 $
 *
 * 
 ************************************************************************/

package de.sgollmer.solvismax.connection.mqtt;

import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.Collection;

import javax.net.ssl.SSLSocketFactory;
import javax.xml.namespace.QName;

import org.eclipse.paho.client.mqttv3.IMqttDeliveryToken;
import org.eclipse.paho.client.mqttv3.MqttCallbackExtended;
import org.eclipse.paho.client.mqttv3.MqttClient;
import org.eclipse.paho.client.mqttv3.MqttConnectOptions;
import org.eclipse.paho.client.mqttv3.MqttException;
import org.eclipse.paho.client.mqttv3.MqttMessage;
import org.eclipse.paho.client.mqttv3.persist.MemoryPersistence;

import de.sgollmer.solvismax.Constants;
import de.sgollmer.solvismax.connection.CommandHandler;
import de.sgollmer.solvismax.connection.IClient;
import de.sgollmer.solvismax.connection.ITransferedData;
import de.sgollmer.solvismax.connection.ServerCommand;
import de.sgollmer.solvismax.connection.ServerStatus;
import de.sgollmer.solvismax.connection.transfer.Command;
import de.sgollmer.solvismax.connection.transfer.JsonPackage;
import de.sgollmer.solvismax.crypt.CryptAes;
import de.sgollmer.solvismax.crypt.Ssl;
import de.sgollmer.solvismax.error.MqttInterfaceException;
import de.sgollmer.solvismax.error.TypeError;
import de.sgollmer.solvismax.error.XmlError;
import de.sgollmer.solvismax.helper.Helper;
import de.sgollmer.solvismax.log.LogManager;
import de.sgollmer.solvismax.log.LogManager.ILogger;
import de.sgollmer.solvismax.model.Instances;
import de.sgollmer.solvismax.model.Solvis;
import de.sgollmer.solvismax.model.SolvisState;
import de.sgollmer.solvismax.model.WatchDog.HumanAccess;
import de.sgollmer.solvismax.model.objects.Observer.IObserver;
import de.sgollmer.solvismax.model.objects.Units.Unit;
import de.sgollmer.solvismax.model.objects.data.BooleanValue;
import de.sgollmer.solvismax.model.objects.data.SingleData;
import de.sgollmer.solvismax.model.objects.data.SolvisData;
import de.sgollmer.solvismax.model.objects.data.StringData;
import de.sgollmer.solvismax.xml.BaseCreator;
import de.sgollmer.solvismax.xml.CreatorByXML;

public class Mqtt {

	private static final ILogger logger = LogManager.getInstance().getLogger(Mqtt.class);

	private static final String XML_SSL = "Ssl";
	private static final Charset UTF_8 = StandardCharsets.UTF_8;

	private MqttClient client = null;

	private final boolean enable;
	private final String brokerUrl;
	private final int port;
	private final String userName;
	private final CryptAes passwordCrypt;
	private final String topicPrefix;
	private final String idPrefix;
	private final int publishQoS;
	private final int subscribeQoS;
	private final Ssl ssl;

	private Instances instances = null;
	private CommandHandler commandHandler = null;
	private MqttThread mqttThread = null;

	public Mqtt(boolean enable, String brokerUrl, int port, String userName, CryptAes passwordCrypt, String topicPrefix,
			String idPrefix, int publishQoS, int subscribeQoS, Ssl ssl) {
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
	}

	public String getBrokerUrl() {
		return this.brokerUrl;
	}

	public int getPort() {
		return this.port;
	}

	public String getTopicPrefix() {
		return this.topicPrefix;
	}

	public String getIdPrefix() {
		return this.idPrefix;
	}

	public int getPublishQoS() {
		return this.publishQoS;
	}

	public int getSubscribeQoS() {
		return this.subscribeQoS;
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
		public Mqtt create() throws XmlError, IOException {
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

	public class MqttThread extends Helper.Runnable {

		private boolean abort = false;

		public MqttThread() {
			super("Mqtt");
		}

		@Override
		public void run() {

			if (!Mqtt.this.client.isConnected()) {
				MqttConnectOptions options = new MqttConnectOptions();
				if (Mqtt.this.userName != null && Mqtt.this.passwordCrypt != null) {
					options.setUserName(Mqtt.this.userName);
					options.setPassword(Mqtt.this.passwordCrypt.cP());
				}
				options.setAutomaticReconnect(true);
				options.setCleanSession(false);
				if (Mqtt.this.ssl != null) {
					SSLSocketFactory sslSocketFactory = Mqtt.this.ssl.getSocketFactory();
					options.setSocketFactory(sslSocketFactory);
				}
				MqttData lastWill = Mqtt.this.getLastWill();
				String topic = Mqtt.this.topicPrefix + '/' + lastWill.topicSuffix;
				options.setWill(topic, lastWill.getPayLoad(), lastWill.getQoS(Mqtt.this.publishQoS),
						lastWill.isRetained());
				options.setMaxInflight(Constants.Mqtt.MAX_INFLIGHT);
				options.setAutomaticReconnect(true);
				Mqtt.this.client.setCallback(Mqtt.this.callback);
				int length = Constants.Mqtt.CMND_SUFFIXES.length;
				String[] topicFilters = new String[length];
				int[] qoSs = new int[length];
				for (int i = 0; i < length; ++i) {
					topicFilters[i] = Mqtt.this.topicPrefix + Constants.Mqtt.CMND_SUFFIXES[i];
					qoSs[i] = Mqtt.this.subscribeQoS;
				}

				boolean connected = false;
				boolean subscribed = false;
				int waitTime = Constants.Mqtt.MIN_CONNECTION_REPEAT_TIME;
				while ((!connected || !subscribed) && !this.abort) {
					try {
						Mqtt.this.client.connect(options);
						connected = true;
						Mqtt.this.client.subscribe(topicFilters, qoSs);
						subscribed = true;
					} catch (MqttException e) {
						if (!connected) {
							logger.info("Mqtt broker not available, will be retried in " + waitTime / 1000 + " s.");
						} else if (!subscribed) {
							logger.error("Error on subscription, will be retried in " + waitTime / 1000 + " s.");
							try {
								synchronized (Mqtt.this) {
									Mqtt.this.client.disconnect();
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

		public synchronized void abort() {
			this.abort = true;
			this.notifyAll();
		}
	}

	public void connect(Instances instances, CommandHandler commandHandler) throws MqttException {

		if (!this.enable) {
			return;
		}

		this.instances = instances;
		this.commandHandler = commandHandler;
		this.mqttThread = new MqttThread();

		if (this.client == null) {
			this.client = new MqttClient("tcp://" + this.brokerUrl + ":" + this.port, // URI
					this.idPrefix + "_" + MqttClient.generateClientId(), // ClientId
					new MemoryPersistence()); // Persistence
		}

		this.mqttThread.submit();
	}

	private class PublishChannelObserver implements IObserver<SolvisData> {

		private final Unit unit;

		public PublishChannelObserver(Solvis solvis) {
			this.unit = solvis.getUnit();
		}

		@Override
		public void update(SolvisData data, Object source) {
			try {
				Mqtt.this.publish(data.getMqttData());
			} catch (MqttException e) {
				logger.error("Error on mqtt publish <" + data.getId() + "> of unit <" + this.unit.getId() + ">:", e);
			}
		}
	}

	private class PublishStatusObserver implements IObserver<SolvisState> {

		private final Unit unit;

		public PublishStatusObserver(Solvis solvis) {
			this.unit = solvis.getUnit();
		}

		@Override
		public void update(SolvisState data, Object source) {
			try {
				Mqtt.this.publish(data.getMqttData());
			} catch (MqttException e) {
				logger.error("Error on mqtt publish <Status> of unit <" + this.unit.getId() + ">:", e);
			}
		}

	}

	private class PublishHumanAccessObserver implements IObserver<HumanAccess> {

		private final Solvis solvis;

		public PublishHumanAccessObserver(Solvis solvis) {
			this.solvis = solvis;
		}

		@Override
		public void update(HumanAccess data, Object source) {
			try {
				Mqtt.this.publish(data.getMqttData(this.solvis));
			} catch (MqttException e) {
				logger.error("Error on mqtt publish <HumannAccess> of unit <" + this.solvis.getUnit().getId() + ">:",
						e);
			}
		}

	}

	private MqttCallbackExtended callback = new MqttCallbackExtended() {

		@Override
		public void connectComplete(boolean reconnect, String serverURI) {
			logger.info("Connection to MQTT successfull");
			synchronized (Mqtt.this) {
				try {
					if (!reconnect) {
						publish(ServerCommand.getMqttMeta(null, true));
						for (Solvis solvis : Mqtt.this.instances.getUnits()) {
							solvis.registerObserver(new PublishChannelObserver(solvis));
							solvis.registerScreenChangedByHumanObserver(new PublishHumanAccessObserver(solvis));
							solvis.getSolvisState().register(new PublishStatusObserver(solvis));
							publish(ServerCommand.getMqttMeta(solvis, false));
							solvis.getSolvisDescription().sendToMqtt(solvis, Mqtt.this);
						}
					}
					for (Solvis solvis : Mqtt.this.instances.getUnits()) {
						Collection<SolvisData> dates = solvis.getAllSolvisData().getMeasurements();
						for (SolvisData data : dates) {
							publish(data.getMqttData());
						}
						publish(solvis.getSolvisState().getMqttData());
						publish(solvis.getHumanAccess().getMqttData(solvis));
					}
					publish(ServerStatus.ONLINE.getMqttData());
				} catch (MqttException e) {
					logger.error("Error: Mqtt exception occured", e);
				}
			}
		}

		@Override
		public void messageArrived(String topic, MqttMessage message) {
			SubscribeData subscribeData;
			try {
				subscribeData = Mqtt.this.analyseReceivedTopic(topic);
			} catch (MqttInterfaceException e) {
				logger.error(e.getMessage());
				return;
			}
			logger.info("Topic <" + topic + "> with received message: " + message.toString());
			Solvis solvis = null;
			if (subscribeData.getUnitId() != null) {
				solvis = Mqtt.this.instances.getUnit(subscribeData.getUnitId());
				if (solvis == null) {
					Mqtt.this.publishError(subscribeData.getClientId(), "Solvis unit unknown.");
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
							Mqtt.this.publishError(subscribeData.getClientId(),
									"Error: Channel <" + subscribeData.getChannelId() + "> not writable.");
							return;
						}
					} catch (TypeError e) {
						Mqtt.this.publishError(subscribeData.getClientId(), "Error: Value error, value: " + string);
						return;
					}
			}
			subscribeData.setValue(data);

			try {
				if (subscribeData.getCommand() != null) {
					Mqtt.this.commandHandler.commandFromClient(subscribeData, new Client(subscribeData.getClientId()));
				}
			} catch (IOException e) {
				logger.error("Error: On command handling", e);
			}
		}

		@Override
		public void deliveryComplete(IMqttDeliveryToken token) {
		}

		@Override
		public void connectionLost(Throwable cause) {
			logger.info("Connection to MQTT broker lost");
		}

	};

	public synchronized void publish(MqttData data) throws MqttException {
		if (this.client == null || !this.client.isConnected()) {
			logger.debug("Not connected, message not delivered");
			return;
		}
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
		this.client.publish(topic, message);
		logger.debug("Messsage was sent to <" + topic + ">, data: " + message.toString());

	}

	public void publishError(String clientId, String message) {
		try {
			this.publish(new MqttData(clientId + Constants.Mqtt.ERROR, message, 0, false));
		} catch (MqttException e) {
			logger.error("Can't deliver error message to iClient: " + message);
		}
	}

	public static class MqttData {
		private final String topicSuffix;
		private final MqttMessage message;

		public MqttData(String topicSuffix, byte[] payload, int qoS, boolean retained) {
			this.topicSuffix = topicSuffix;
			this.message = new MqttMessage(payload);
			this.message.setQos(qoS);
			this.message.setRetained(retained);
		}

		public MqttData(String topicSuffix, String utf8Data, int qoS, boolean retained) {
			this(topicSuffix, utf8Data.getBytes(UTF_8), qoS, retained);
		}

		public MqttData(Solvis solvis, String topicSuffix, String utf8Data, int qoS, boolean retained) {
			this(solvis.getUnit().getId() + topicSuffix, utf8Data, qoS, retained);
		}

		public MqttData(String topicSuffix, String utf8Data) {
			this(topicSuffix, utf8Data, 0, true);
		}

		public String gettopicSuffix() {
			return this.topicSuffix;
		}

		public byte[] getPayLoad() {
			return this.message.getPayload();
		}

		public int getQoS(int defaultQoS) {
			int qoS = this.message.getQos();
			if (qoS == 0) {
				return defaultQoS;
			} else {
				return qoS;
			}
		}

		public boolean isRetained() {
			return this.message.isRetained();
		}
	}

	private MqttData getLastWill() {
		return ServerStatus.OFFLINE.getMqttData();

	}

	private static enum Format {
		FROM_META, STRING, BOOLEAN, NONE
	}

	private static enum SubscribeType {
		SERVER_META(new String[] { "server", "meta" }, 0, false, false, null, Format.NONE), //
		SERVER_COMMAND(new String[] { "server", "cmnd" }, 1, false, false, Command.SERVER_COMMAND, Format.STRING), //
		SERVER_ONLINE(new String[] { "server", "online" }, 0, false, false, null, Format.NONE), //
		CLIENT_ONLINE(new String[] { "online" }, 1, false, false, Command.CLIENT_ONLINE, Format.BOOLEAN), //
		UNIT_STATUS(new String[] { "status" }, 1, true, false, null, Format.NONE), //
		UNIT_SERVER_COMMAND(new String[] { "server", "cmnd" }, 2, true, false, Command.SERVER_COMMAND, Format.STRING), //
		UNIT_CHANNEL_COMMAND(new String[] { "cmnd" }, 3, true, true, Command.SET, Format.FROM_META), //
		UNIT_CHANNEL_UPDATE(new String[] { "update" }, 3, true, true, Command.GET, Format.NONE), //
		UNIT_CHANNEL_DATA(new String[] { "data" }, 2, true, true, null, Format.NONE), //
		UNIT_CHANNEL_META(new String[] { "meta" }, 2, true, true, null, Format.NONE); //

		private final String[] cmp;
		private final int position;
		private final boolean unitId;
		private final boolean channelId;
		private final Command command;
		private final Format format;

		private SubscribeType(String[] cmp, int position, boolean unitId, boolean channelId, Command command,
				Format format) {
			this.cmp = cmp;
			this.position = position;
			this.unitId = unitId;
			this.channelId = channelId;
			this.command = command;
			this.format = format;
		}

		public static SubscribeType get(String[] partsWoPrefix) throws MqttInterfaceException {
			for (SubscribeType type : SubscribeType.values()) {
				boolean found = type.cmp.length <= partsWoPrefix.length - type.position;
				for (int i = 0; found && i < type.cmp.length; ++i) {
					if (!type.cmp[i].equalsIgnoreCase(partsWoPrefix[i + type.position])) {
						found = false;
					}
				}
				if (found) {
					return type;
				}
			}
			return null;
		}

		public boolean hasUnitId() {
			return this.unitId;
		}

		public boolean hasChannelId() {
			return this.channelId;
		}

		public Command getCommand() {
			return this.command;
		}

	}

	private static class SubscribeData implements ITransferedData {
		private final String clientId;
		private final String unitId;
		private final SubscribeType type;
		private final String channelId;
		private Solvis solvis;
		private SingleData<?> value;

		public SubscribeData(String clientId, String unitId, String channelId, SubscribeType type) {
			this.clientId = clientId;
			this.unitId = unitId;
			this.type = type;
			this.channelId = channelId;
		}

		@Override
		public String getClientId() {
			return this.clientId;
		}

		public String getUnitId() {
			return this.unitId;
		}

		@Override
		public Solvis getSolvis() {
			return this.solvis;
		}

		@Override
		public Command getCommand() {
			return this.type.getCommand();
		}

		@Override
		public String getChannelId() {
			return this.channelId;
		}

		@Override
		public SingleData<?> getSingleData() {
			return this.value;
		}

		public void setValue(SingleData<?> value) {
			this.value = value;
		}

		@Override
		public void setSolvis(Solvis solvis) {
			this.solvis = solvis;
		}
	}

	private SubscribeData analyseReceivedTopic(String topic) throws MqttInterfaceException {
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
				this.publish(this.getLastWill());
				this.client.disconnect();
			} catch (MqttException e) {
			}
		}
	}

	public static String formatChannelIn(String mqttChannelId) {
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

	private class Client implements IClient {
		private final String clientId;

		public Client(String clientId) {
			this.clientId = clientId;
		}

		@Override
		public void sendCommandError(String message) {
			publishError(this.clientId, message);
			logger.info(message);

		}

		@Override
		public void send(JsonPackage jsonPackage) {
			logger.error("Unexpected using of iClient");
		}

		@Override
		public void closeDelayed() {
			logger.error("Unexpected using of iClient");
		}
	}

}
