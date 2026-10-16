/************************************************************************
 *
 * Home Assistant MQTT Discovery
 *
 * Publishes MQTT discovery messages so that Home Assistant automatically
 * creates entities for all Solvis channels.
 *
 * Discovery topic pattern:
 *   <haDiscoveryPrefix>/<domain>/<uniqueId>/config
 *
 * Enabled by setting the "haDiscoveryPrefix" attribute on the <Mqtt>
 * element in base.xml (e.g. haDiscoveryPrefix="homeassistant").
 *
 * Entity ids and unique ids are derived from the channel id (S01, C05, ...)
 * only, so that renaming a channel in base.xml keeps the entity and its
 * history. Display names come from ha-labels.properties, with a fallback
 * derived from the channel name.
 *
 * The entities of a unit are grouped into HA devices following the
 * Solvis schema (Speicher, Solaranlage, Warmwasser, Heizkreis 1-3,
 * Waermeerzeuger), mapping in ha-groups.properties. Each group device is
 * linked to the unit device (via_device). Entity ids do not depend on the
 * grouping.
 *
 * Both property files can be overridden per installation by files of the
 * same name in the writable data directory (next to graficData.xml). The
 * entries of such a file replace or extend the built-in ones.
 *
 * Retained discovery messages of this server which are not published
 * again (renamed units, removed channels, old unique ids) are deleted
 * some seconds after the connection has been established.
 *
 * Home Assistant keeps an existing entity on its device even if the
 * discovery message names another one. Entities whose device has changed
 * compared to the retained message are therefore removed (empty payload)
 * and published again, entity id and history are kept.
 *
 ************************************************************************/

package de.sgollmer.solvismax.connection.mqtt;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Locale;
import java.util.Properties;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import de.sgollmer.solvismax.Constants;
import de.sgollmer.solvismax.Main;
import de.sgollmer.solvismax.log.LogManager;
import de.sgollmer.solvismax.log.LogManager.ILogger;
import de.sgollmer.solvismax.model.Instances;
import de.sgollmer.solvismax.model.Solvis;
import de.sgollmer.solvismax.model.objects.ChannelInstance;
import de.sgollmer.solvismax.model.objects.IChannelSource;
import de.sgollmer.solvismax.model.objects.IChannelSource.UpperLowerStep;
import de.sgollmer.solvismax.model.objects.data.IMode;
import de.sgollmer.solvismax.model.objects.data.SolvisData;

class HomeAssistantDiscovery {

	private static final ILogger logger = LogManager.getInstance().getLogger(HomeAssistantDiscovery.class);

	private static final String RESOURCE_DIR = "/de/sgollmer/solvismax/data/";
	private static final String LABELS_FILE = "ha-labels.properties";
	private static final String GROUPS_FILE = "ha-groups.properties";
	/** key prefix of the group definitions in ha-groups.properties */
	private static final String DEVICE_KEY_PREFIX = "device.";
	private static final String GUI_WRITE_ENABLED_SUFFIX = "gui_write_enabled";
	private static final String CONFIG_SUFFIX = "/config";
	private static final long CLEANUP_DELAY_MS = 20000;
	/** time HA needs to remove an entity before it is published again */
	private static final long REPUBLISH_DELAY_MS = 5000;

	private static final String CATEGORY_DIAGNOSTIC = "diagnostic";
	private static final String CATEGORY_CONFIG = "config";
	private static final String FLAG_DISABLED = "disabled";

	private static final String SECONDS_TO_HOURS_TEMPLATE = "{{ (value | float(0) / 3600) | round(2) }}";

	/** Solvis reports an open or shorted temperature sensor with these values */
	private static final String OPEN_SENSOR_TEMPLATE = "{% set v = value | float(none) %}"
			+ "{{ none if v is none or v >= 250 or v <= -35 else v }}";

	private static final Pattern COUNTER_IDS = Pattern.compile("X0[1-4]|X1[01]|C0[1-3]|C2[45]");
	private static final Pattern CAMEL_CASE = Pattern.compile("(?<=[a-z\\u00e4\\u00f6\\u00fc\\u00df])(?=[A-Z])");
	private static final Pattern UNIT_SUFFIX = Pattern.compile("_(s|h|kWh|kW)$");
	private static final Pattern DEVICE_IDENTIFIER = Pattern
			.compile("\"identifiers\"\\s*:\\s*\\[\\s*\"([^\"]*)\"");

	private final Mqtt mqtt;
	private final Properties labels = new Properties();
	private final Properties groups = new Properties();
	private File overrideDir = null;
	private final Set<String> unknownGroups = new HashSet<>();

	/** discovery topics published in this session with their payload */
	private final Map<String, String> published = new HashMap<>();
	/** retained discovery topics of this server found on the broker */
	private final Set<String> retainedOnBroker = new HashSet<>();
	/** device identifier of the retained discovery messages of former runs */
	private final Map<String, String> retainedDevices = new HashMap<>();
	private boolean cleanupScheduled = false;

	HomeAssistantDiscovery(final Mqtt mqtt) {
		this.mqtt = mqtt;
		loadResource(this.labels, LABELS_FILE);
		loadResource(this.groups, GROUPS_FILE);
	}

	private static void loadResource(final Properties properties, final String fileName) {
		String resource = RESOURCE_DIR + fileName;
		InputStream in = Main.class.getResourceAsStream(resource);
		if (in == null) {
			logger.warn("Resource <" + resource + "> not found.");
			return;
		}
		try (InputStreamReader reader = new InputStreamReader(in, Mqtt.UTF_8)) {
			properties.load(reader);
		} catch (IOException e) {
			logger.error("Error reading <" + resource + ">: " + e.getMessage());
		}
	}

	/**
	 * Loads the installation specific overrides of the labels and groups from
	 * the writable data directory, if present. Executed once.
	 */
	private void loadOverrides(final File dir) {
		if (dir == null || dir.equals(this.overrideDir)) {
			return;
		}
		this.overrideDir = dir;
		loadOverride(this.labels, new File(dir, LABELS_FILE));
		loadOverride(this.groups, new File(dir, GROUPS_FILE));
	}

	private static void loadOverride(final Properties properties, final File file) {
		if (!file.isFile()) {
			return;
		}
		try (InputStreamReader reader = new InputStreamReader(new FileInputStream(file), Mqtt.UTF_8)) {
			Properties override = new Properties();
			override.load(reader);
			properties.putAll(override);
			logger.info("HA discovery: " + override.size() + " entries read from <" + file.getAbsolutePath() + ">.");
		} catch (IOException e) {
			logger.error("Error reading <" + file.getAbsolutePath() + ">: " + e.getMessage());
		}
	}

	boolean isEnabled() {
		String prefix = this.mqtt.getHaDiscoveryPrefix();
		return prefix != null && !prefix.isEmpty();
	}

	/**
	 * Topic filter for the retained discovery messages, used to find stale
	 * entries of this server.
	 */
	String getSubscribeFilter() {
		return this.mqtt.getHaDiscoveryPrefix() + "/+/+" + CONFIG_SUFFIX;
	}

	boolean isDiscoveryTopic(final String topic) {
		return this.isEnabled() && topic.startsWith(this.mqtt.getHaDiscoveryPrefix() + '/')
				&& topic.endsWith(CONFIG_SUFFIX);
	}

	/**
	 * Called for every message received on the discovery filter. Only
	 * messages of this server (unique id starts with the topic prefix) are
	 * remembered.
	 */
	void retainedDiscoveryReceived(final String topic, final byte[] payload) {
		if (payload == null || payload.length == 0) {
			return;
		}
		String[] parts = topic.split("/");
		if (parts.length != 4 || !parts[2].startsWith(this.mqtt.getTopicPrefix() + '_')) {
			return;
		}
		synchronized (this) {
			this.retainedOnBroker.add(topic);
			// The echo of an own publish is identical, everything else is from a former run
			String received = new String(payload, Mqtt.UTF_8);
			if (!received.equals(this.published.get(topic))) {
				this.retainedDevices.put(topic, identifierOf(received));
			}
		}
	}

	private static String identifierOf(final String payload) {
		Matcher matcher = DEVICE_IDENTIFIER.matcher(payload);
		return matcher.find() ? matcher.group(1) : "";
	}

	void publishDiscovery(final Instances instances) {
		if (!this.isEnabled()) {
			return;
		}
		String prefix = this.mqtt.getHaDiscoveryPrefix();
		this.loadOverrides(instances.getWritePath());

		String availabilityTopic = this.mqtt.getTopicPrefix() + "/" + Constants.Mqtt.SERVER + "/"
				+ Constants.Mqtt.ONLINE_STATUS;

		synchronized (this) {
			this.published.clear();
		}

		for (Solvis solvis : instances.getUnits()) {
			String unitId = solvis.getUnit().getId();
			String configurationUrl = solvis.getUnit().getUrl();
			String deviceJson = buildDeviceJson(unitId, configurationUrl);
			Map<String, String> groupDeviceJsons = new HashMap<>();
			boolean guiAccess = solvis.getFeatures().isInteractiveGUIAccess();

			publishUnitStatusDiscovery(prefix, solvis, unitId, deviceJson, availabilityTopic);

			int skippedControl = 0;
			int skippedUnnamed = 0;
			for (SolvisData solvisData : solvis.getAllSolvisData().getSolvisDatas()) {
				if (solvisData.isDontSend() || solvisData.getSmartHomeData() == null) {
					continue;
				}
				ChannelInstance instance = solvisData.getChannelInstance();
				if (instance == null) {
					continue;
				}
				String channelName = solvisData.getName();
				String channelId = instance.getDescription().getId();
				String label = this.getLabel(channelId, channelName);
				String channelDeviceJson = this.getGroupDeviceJson(unitId, channelId, channelName, deviceJson,
						groupDeviceJsons);

				// Values of the SolvisControl GUI are only read with GUI access
				if (!guiAccess && instance.getDescription().getType() == IChannelSource.Type.CONTROL) {
					++skippedControl;
					continue;
				}
				// Channels without assignment (e.g. "I-2") have no meaning in this configuration
				if (channelName.indexOf('.') < 0) {
					++skippedUnnamed;
					continue;
				}
				try {
					publishChannelDiscovery(prefix, unitId, solvisData, instance, channelId, label,
							channelDeviceJson, availabilityTopic);
				} catch (Exception e) {
					logger.error("Error publishing HA discovery for channel <" + channelName + ">: " + e.getMessage());
				}
			}
			if (skippedControl > 0) {
				logger.info("HA discovery: " + skippedControl + " GUI channels of unit <" + unitId
						+ "> not published, feature InteractiveGUIAccess is disabled.");
			}
			if (skippedUnnamed > 0) {
				logger.info("HA discovery: " + skippedUnnamed + " channels of unit <" + unitId
						+ "> without assignment not published.");
			}
		}
		logger.info("Home Assistant MQTT discovery messages published.");
		this.scheduleCleanup();
	}

	/**
	 * Deletes retained discovery messages of this server which were not
	 * published in this session. The retained messages arrive shortly after
	 * the subscription, therefore the cleanup is delayed.
	 */
	private synchronized void scheduleCleanup() {
		if (this.cleanupScheduled) {
			return;
		}
		this.cleanupScheduled = true;
		Thread thread = new Thread(() -> {
			try {
				Thread.sleep(CLEANUP_DELAY_MS);
			} catch (InterruptedException e) {
				return;
			}
			this.cleanup();
		}, "HaDiscoveryCleanup");
		thread.setDaemon(true);
		thread.start();
	}

	private void cleanup() {
		Collection<String> stale = new HashSet<>();
		Map<String, String> moved = new HashMap<>();
		synchronized (this) {
			this.cleanupScheduled = false;
			for (String topic : this.retainedOnBroker) {
				if (!this.published.containsKey(topic)) {
					stale.add(topic);
				}
			}
			for (Map.Entry<String, String> entry : this.retainedDevices.entrySet()) {
				String payload = this.published.get(entry.getKey());
				if (payload != null && !entry.getValue().equals(identifierOf(payload))) {
					moved.put(entry.getKey(), payload);
				}
			}
			this.retainedOnBroker.clear();
			this.retainedDevices.clear();
		}
		for (String topic : stale) {
			this.mqtt.publish(new MqttData(topic, "", 0, true));
			logger.info("HA discovery: stale entry <" + topic + "> deleted.");
		}
		if (moved.isEmpty()) {
			return;
		}
		// HA doesn't move an entity to another device on update, so remove and re-create it
		for (String topic : moved.keySet()) {
			this.mqtt.publish(new MqttData(topic, "", 0, true));
		}
		logger.info("HA discovery: " + moved.size()
				+ " entities assigned to another device, they are removed and published again.");
		try {
			Thread.sleep(REPUBLISH_DELAY_MS);
		} catch (InterruptedException e) {
			return;
		}
		for (Map.Entry<String, String> entry : moved.entrySet()) {
			this.mqtt.publish(new MqttData(entry.getKey(), entry.getValue(), 0, true));
		}
	}

	private void publishConfig(final String topic, final String payload) {
		synchronized (this) {
			this.published.put(topic, payload);
		}
		this.mqtt.publish(new MqttData(topic, payload, 0, true));
	}

	private void publishUnitStatusDiscovery(final String prefix, final Solvis solvis, final String unitId,
			final String deviceJson, final String availabilityTopic) {

		String topicPrefix = this.mqtt.getTopicPrefix();
		String idBase = topicPrefix + "_" + unitId;
		String objectBase = objectId(unitId, null);

		// Verbindungsstatus der Anlage (SOLVIS_CONNECTED, POWER_OFF, ...)
		publishStaticSensor(prefix, idBase + "_status", objectBase + "_status", "Verbindungsstatus",
				topicPrefix + "/" + unitId + "/" + Constants.Mqtt.STATUS, availabilityTopic, deviceJson, true);

		// GUI-Steuerungsstatus (CONTROL_WRITE_ONGOING, CONTROL_FINISHED, ...), wechselt sekuendlich,
		// deshalb in HA standardmaessig deaktiviert
		publishStaticSensor(prefix, idBase + "_gui_access", objectBase + "_gui_status", "GUI-Status",
				topicPrefix + "/" + unitId + "/" + Constants.Mqtt.CONTROL, availabilityTopic, deviceJson, false);

		// Menschlicher Zugriff (USER_ACCESS_DETECTED, SERVICE_ACCESS_DETECTED, ...)
		publishStaticSensor(prefix, idBase + "_human_access", objectBase + "_anlagenzugriff", "Anlagenzugriff",
				topicPrefix + "/" + unitId + "/" + Constants.Mqtt.HUMAN_ACCESS, availabilityTopic, deviceJson, true);

		// Schreibzugriff aktiviert (InteractiveGUIAccess && controlEnabled)
		String guiWriteTopic = topicPrefix + "/" + unitId + "/" + GUI_WRITE_ENABLED_SUFFIX;
		StringBuilder payload = new StringBuilder();
		payload.append('{');
		appendString(payload, "name", "Schreibzugriff aktiviert", true);
		appendString(payload, "unique_id", idBase + "_gui_write_enabled", false);
		appendString(payload, "default_entity_id", "binary_sensor." + objectBase + "_schreibzugriff", false);
		payload.append(",\"has_entity_name\":true");
		appendString(payload, "entity_category", CATEGORY_DIAGNOSTIC, false);
		appendString(payload, "state_topic", guiWriteTopic, false);
		appendString(payload, "payload_on", "true", false);
		appendString(payload, "payload_off", "false", false);
		appendAvailability(payload, availabilityTopic, deviceJson);
		payload.append('}');
		publishConfig(prefix + "/binary_sensor/" + idBase + "_gui_write_enabled" + CONFIG_SUFFIX,
				payload.toString());

		// Aktuellen Wert als retained Message publishen
		String guiWriteValue = Boolean.toString(solvis.isControlEnabled());
		this.mqtt.publish(new MqttData(guiWriteTopic, guiWriteValue, 0, true));
	}

	private void publishStaticSensor(final String prefix, final String uniqueId, final String objectId,
			final String name, final String stateTopic, final String availabilityTopic, final String deviceJson,
			final boolean enabledByDefault) {
		StringBuilder payload = new StringBuilder();
		payload.append('{');
		appendString(payload, "name", name, true);
		appendString(payload, "unique_id", uniqueId, false);
		appendString(payload, "default_entity_id", "sensor." + objectId, false);
		payload.append(",\"has_entity_name\":true");
		if (!enabledByDefault) {
			payload.append(",\"enabled_by_default\":false");
		}
		appendString(payload, "entity_category", CATEGORY_DIAGNOSTIC, false);
		appendString(payload, "state_topic", stateTopic, false);
		appendAvailability(payload, availabilityTopic, deviceJson);
		payload.append('}');
		publishConfig(prefix + "/sensor/" + uniqueId + CONFIG_SUFFIX, payload.toString());
	}

	private String deviceIdentifier(final String unitId) {
		return this.mqtt.getTopicPrefix() + "_" + unitId;
	}

	/** Device of the unit itself (status entities and channels without group) */
	private String buildDeviceJson(final String unitId, final String configurationUrl) {
		StringBuilder sb = new StringBuilder();
		sb.append("{\"identifiers\":[\"").append(jsonEscape(deviceIdentifier(unitId)));
		sb.append("\"],\"name\":\"Solvis ").append(jsonEscape(unitId));
		sb.append("\",\"manufacturer\":\"Solvis GmbH\",\"model\":\"SolvisMax\"");
		if (configurationUrl != null && !configurationUrl.isEmpty()) {
			String fullUrl = configurationUrl.contains("://") ? configurationUrl : "http://" + configurationUrl;
			sb.append(",\"configuration_url\":\"").append(jsonEscape(fullUrl)).append('"');
		}
		sb.append('}');
		return sb.toString();
	}

	/** Device of a group (Speicher, Solaranlage, ...), linked to the unit device */
	private String buildGroupDeviceJson(final String unitId, final String groupKey, final String groupName) {
		StringBuilder sb = new StringBuilder();
		sb.append("{\"identifiers\":[\"").append(jsonEscape(deviceIdentifier(unitId) + "_" + groupKey));
		sb.append("\"],\"name\":\"").append(jsonEscape(unitId + " " + groupName));
		sb.append("\",\"manufacturer\":\"Solvis GmbH\",\"model\":\"SolvisMax\"");
		sb.append(",\"via_device\":\"").append(jsonEscape(deviceIdentifier(unitId))).append('"');
		sb.append('}');
		return sb.toString();
	}

	/**
	 * Determines the device JSON of a channel. The group is looked up in
	 * ha-groups.properties by the channel name first (assignment specific, e.g.
	 * "S09.Heissgas"), then by the channel id. No or an empty entry means the
	 * unit device.
	 */
	private String getGroupDeviceJson(final String unitId, final String channelId, final String channelName,
			final String unitDeviceJson, final Map<String, String> cache) {
		String groupKey = this.groups.getProperty(channelName);
		if (groupKey == null) {
			groupKey = this.groups.getProperty(channelId);
		}
		if (groupKey == null || groupKey.trim().isEmpty()) {
			return unitDeviceJson;
		}
		groupKey = groupKey.trim();
		String groupName = this.groups.getProperty(DEVICE_KEY_PREFIX + groupKey);
		if (groupName == null || groupName.trim().isEmpty()) {
			if (this.unknownGroups.add(groupKey)) {
				logger.warn("HA discovery: group <" + groupKey + "> of channel <" + channelName
						+ "> is not defined (" + DEVICE_KEY_PREFIX + groupKey + "), unit device is used.");
			}
			return unitDeviceJson;
		}
		String json = cache.get(groupKey);
		if (json == null) {
			json = buildGroupDeviceJson(unitId, sanitizeId(groupKey), groupName.trim());
			cache.put(groupKey, json);
		}
		return json;
	}

	private void publishChannelDiscovery(final String prefix, final String unitId, final SolvisData solvisData,
			final ChannelInstance instance, final String channelId, final String labelEntry, final String deviceJson,
			final String availabilityTopic) {

		String channelName = solvisData.getName();
		boolean writeable = instance.isWriteable();
		boolean isBoolean = instance.isBoolean();
		boolean isControl = instance.getDescription().getType() == IChannelSource.Type.CONTROL;
		Collection<? extends IMode<?>> modes = instance.getModes();
		UpperLowerStep uls = instance.getUpperLowerStep();
		String unit = haUnit(instance.getUnit());
		Double accuracy = instance.getAccuracy();

		String domain = determineDomain(writeable, isBoolean, modes);

		String channelIdTopic = TopicType.formatChannelPublish(channelName);
		String stateTopic = this.mqtt.getTopicPrefix() + "/" + unitId + "/" + channelIdTopic + "/"
				+ Constants.Mqtt.DATA_SUFFIX;
		String commandTopic = writeable
				? this.mqtt.getTopicPrefix() + "/" + this.mqtt.getSmartHomeId() + "/" + unitId + "/" + channelIdTopic
						+ "/" + Constants.Mqtt.CMND_SUFFIX
				: null;

		String uniqueId = sanitizeId(this.mqtt.getTopicPrefix() + "_" + unitId + "_" + channelId);
		String objectId = objectId(unitId, channelId);

		// Label entry: "Name|category|flags", category = diagnostic/config, flag "disabled"
		String name;
		String category = null;
		boolean enabledByDefault = true;
		if (labelEntry != null) {
			String[] parts = labelEntry.split("\\|");
			name = parts[0].trim();
			for (int i = 1; i < parts.length; ++i) {
				String part = parts[i].trim();
				if (FLAG_DISABLED.equals(part)) {
					enabledByDefault = false;
				} else if (!part.isEmpty()) {
					category = part;
				}
			}
		} else {
			name = fallbackName(channelName);
		}

		// Runtimes counted by the server in seconds are published in hours
		boolean secondsToHours = "sensor".equals(domain) && "s".equals(unit)
				&& COUNTER_IDS.matcher(channelId).matches();
		if (secondsToHours) {
			unit = "h";
		}

		String deviceClass = deriveDeviceClass(unit, isBoolean, channelId);
		String stateClass = deriveStateClass(channelId, unit, isBoolean, modes);

		StringBuilder payload = new StringBuilder();
		payload.append('{');
		appendString(payload, "name", name, true);
		appendString(payload, "unique_id", uniqueId, false);
		// HA >= 2025.10: default_entity_id (with domain) replaces object_id
		appendString(payload, "default_entity_id", domain + "." + objectId, false);
		payload.append(",\"has_entity_name\":true");
		if (!enabledByDefault) {
			payload.append(",\"enabled_by_default\":false");
		}
		if (category != null && !category.isEmpty()) {
			appendString(payload, "entity_category", category, false);
		}
		appendString(payload, "state_topic", stateTopic, false);

		if (commandTopic != null) {
			appendString(payload, "command_topic", commandTopic, false);
		}

		appendDomainFields(payload, domain, modes, uls, unit);

		if (deviceClass != null) {
			appendString(payload, "device_class", deviceClass, false);
		}
		if (stateClass != null && "sensor".equals(domain)) {
			appendString(payload, "state_class", stateClass, false);
		}
		if (secondsToHours) {
			payload.append(",\"suggested_display_precision\":1");
			appendString(payload, "value_template", SECONDS_TO_HOURS_TEMPLATE, false);
		} else if (accuracy != null && "sensor".equals(domain)) {
			int precision = derivePrecision(accuracy);
			payload.append(",\"suggested_display_precision\":").append(precision);
		}
		if ("sensor".equals(domain) && !isControl
				&& ("temperature".equals(deviceClass) || "pressure".equals(deviceClass))) {
			appendString(payload, "value_template", OPEN_SENSOR_TEMPLATE, false);
		}
		// Numeric channel declared boolean by its assignment (booleanValue = value meaning "false")
		Integer falseValue = instance.getAssignment().getBooleanValue();
		if (isBoolean && falseValue != null && !instance.getDescription().isBoolean()) {
			appendString(payload, "value_template",
					"{{ 'false' if value | float(" + falseValue + ") == " + falseValue + " else 'true' }}", false);
		}
		appendAvailability(payload, availabilityTopic, deviceJson);
		payload.append('}');

		String discoveryTopic = prefix + "/" + domain + "/" + uniqueId + CONFIG_SUFFIX;
		publishConfig(discoveryTopic, payload.toString());

		logger.debug("HA discovery: <" + channelName + "> -> " + domain + " @ <" + discoveryTopic + ">");
	}

	private String getLabel(final String channelId, final String channelName) {
		String label = this.labels.getProperty(channelName);
		if (label == null) {
			label = this.labels.getProperty(channelId);
		}
		return label;
	}

	/**
	 * Derives a readable name from a channel name like "X02.BrennerLaufzeit_s"
	 * -> "Brenner Laufzeit".
	 */
	static String fallbackName(final String channelName) {
		String label = channelName;
		int dot = label.indexOf('.');
		if (dot >= 0) {
			label = label.substring(dot + 1);
		}
		label = UNIT_SUFFIX.matcher(label).replaceAll("");
		label = label.replace('_', ' ');
		label = CAMEL_CASE.matcher(label).replaceAll(" ");
		label = label.replace("fuehler", "f\u00fchler").replace("Fuehler", "F\u00fchler");
		label = label.replace("ruecklauf", "r\u00fccklauf").replace("Ruecklauf", "R\u00fccklauf");
		label = label.replace("stoerung", "st\u00f6rung").replace("Stoerung", "St\u00f6rung");
		label = label.replace("Waerme", "W\u00e4rme").replace("waerme", "w\u00e4rme");
		return label.trim();
	}

	/** HA entity id suffix: solvis_<unit>_<channel id>, lower case, [a-z0-9_] only */
	private static String objectId(final String unitId, final String channelId) {
		String s = "solvis_" + unitId + (channelId != null ? "_" + channelId : "");
		return s.toLowerCase(Locale.ROOT).replaceAll("[^a-z0-9]+", "_");
	}

	private static String sanitizeId(final String id) {
		return id.replace('.', '_').replace(':', '_').replace(' ', '_').replace('/', '_');
	}

	private static String haUnit(final String unit) {
		if (unit == null) {
			return null;
		}
		switch (unit) {
			case "l/h":
				return "L/h";
			case "l/min":
				return "L/min";
			default:
				return unit;
		}
	}

	private static String determineDomain(final boolean writeable, final boolean isBoolean,
			final Collection<? extends IMode<?>> modes) {
		if (isBoolean) {
			return writeable ? "switch" : "binary_sensor";
		}
		if (modes != null && !modes.isEmpty()) {
			return writeable ? "select" : "sensor";
		}
		return writeable ? "number" : "sensor";
	}

	private static void appendDomainFields(final StringBuilder payload, final String domain,
			final Collection<? extends IMode<?>> modes, final UpperLowerStep uls, final String unit) {
		switch (domain) {
			case "binary_sensor":
				appendString(payload, "payload_on", "true", false);
				appendString(payload, "payload_off", "false", false);
				break;
			case "switch":
				appendString(payload, "payload_on", "true", false);
				appendString(payload, "payload_off", "false", false);
				appendString(payload, "state_on", "true", false);
				appendString(payload, "state_off", "false", false);
				break;
			case "select":
				if (modes != null) {
					payload.append(",\"options\":[");
					boolean first = true;
					for (IMode<?> mode : modes) {
						if (!first) {
							payload.append(',');
						}
						payload.append('"').append(jsonEscape(mode.getName())).append('"');
						first = false;
					}
					payload.append(']');
				}
				break;
			case "number":
				if (uls != null) {
					payload.append(",\"min\":").append(uls.getLower());
					payload.append(",\"max\":").append(uls.getUpper());
					payload.append(",\"step\":").append(uls.getStep());
				}
				if (unit != null) {
					appendString(payload, "unit_of_measurement", unit, false);
				}
				break;
			case "sensor":
				if (unit != null) {
					appendString(payload, "unit_of_measurement", unit, false);
				}
				break;
			default:
				break;
		}
	}

	private static String deriveDeviceClass(final String unit, final boolean isBoolean, final String channelId) {
		if (isBoolean) {
			// Stoerungen der externen Waermeerzeuger
			return channelId.startsWith("I-") ? "problem" : null;
		}
		if (unit == null) {
			return null;
		}
		switch (unit) {
			case "\u00B0C": // escaped: sources are compiled with Cp1252
				return "temperature";
			case "K":
				return null; // Differenzen: HA wuerde K als absolute Temperatur umrechnen
			case "W":
			case "kW":
				return "power";
			case "Wh":
			case "kWh":
				return "energy";
			case "L/h":
			case "L/min":
				return "volume_flow_rate";
			case "s":
			case "h":
			case "min":
				return "duration";
			case "%":
				return null; // zu allgemein
			case "bar":
			case "hPa":
				return "pressure";
			case "V":
				return "voltage";
			case "A":
				return "current";
			case "Hz":
				return "frequency";
			default:
				return null;
		}
	}

	private static String deriveStateClass(final String channelId, final String unit, final boolean isBoolean,
			final Collection<? extends IMode<?>> modes) {
		if (isBoolean || (modes != null && !modes.isEmpty())) {
			return null;
		}
		// Zaehler, Laufzeiten und Energiemengen sind monoton steigend
		if (COUNTER_IDS.matcher(channelId).matches() || "Wh".equals(unit) || "kWh".equals(unit)) {
			return "total_increasing";
		}
		if (unit == null) {
			return null;
		}
		return "measurement";
	}

	private static int derivePrecision(final Double accuracy) {
		if (accuracy == null || accuracy >= 1.0) {
			return 0;
		} else if (accuracy >= 0.1) {
			return 1;
		} else if (accuracy >= 0.01) {
			return 2;
		} else {
			return 3;
		}
	}

	private static void appendAvailability(final StringBuilder payload, final String availabilityTopic,
			final String deviceJson) {
		appendString(payload, "availability_topic", availabilityTopic, false);
		appendString(payload, "payload_available", "true", false);
		appendString(payload, "payload_not_available", "false", false);
		payload.append(",\"device\":").append(deviceJson);
	}

	private static void appendString(final StringBuilder sb, final String key, final String value,
			final boolean first) {
		if (!first) {
			sb.append(',');
		}
		sb.append('"').append(key).append("\":\"").append(jsonEscape(value)).append('"');
	}

	private static String jsonEscape(final String s) {
		if (s == null) {
			return "";
		}
		return s.replace("\\", "\\\\").replace("\"", "\\\"").replace("\n", "\\n").replace("\r", "\\r").replace("\t",
				"\\t");
	}
}
