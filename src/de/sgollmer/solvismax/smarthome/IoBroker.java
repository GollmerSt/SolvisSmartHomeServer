package de.sgollmer.solvismax.smarthome;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.UnsupportedEncodingException;
import java.io.Writer;

import javax.xml.namespace.QName;

import de.sgollmer.solvismax.BaseData;
import de.sgollmer.solvismax.Constants;
import de.sgollmer.solvismax.connection.mqtt.TopicType;
import de.sgollmer.solvismax.connection.mqtt.TopicType.TopicData;
import de.sgollmer.solvismax.helper.FileHelper;
import de.sgollmer.solvismax.helper.Helper;
import de.sgollmer.solvismax.model.Instances;
import de.sgollmer.xmllibrary.BaseCreator;
import de.sgollmer.xmllibrary.CreatorByXML;
import de.sgollmer.xmllibrary.XmlException;

public class IoBroker {
	private final String mqttInterface;
	private final String javascriptInterface;
	private BaseData baseData;
	private boolean first = true;

	private IoBroker(final String mqttInterface, final String javascriptInterface) {
		this.mqttInterface = mqttInterface;
		this.javascriptInterface = javascriptInterface;
	}

	public IoBroker() {
		this.mqttInterface = Constants.IoBroker.DEFAULT_MQTT_INTERFACE;
		this.javascriptInterface = Constants.IoBroker.DEFAULT_JAVASCRIPT_INTERFACE;
	}

	public void writeObjectList(final Instances instances) throws IOException {

		Writer writer = null;
		File directory = instances.getAppendixPath();
		String name = this.mqttInterface + '.' + Constants.IoBroker.OBJECT_LIST_NAME;
		File file = new File(directory, name);

		FileHelper.mkdir(directory);

		try {
			writer = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(file), "UTF-8"));
		} catch (UnsupportedEncodingException e) {
			throw new IOException(e);
		}

		writer.append("{\n");

		for (TopicData topic : TopicType.getTopicDatas(instances)) {
			this.appendObject(writer, topic);
		}

		writer.append('}');

		writer.flush();
		writer.close();

		System.out.println("File <" + file.getAbsolutePath() + "> created.");
	}

	private String catWithSeparator(final boolean withInterface, final String[] topicParts, String suffix,
			char separator) throws IOException {
		return this.catWithSeparator(withInterface, topicParts, suffix, separator, -1);
	}

	private String catWithSeparator(final boolean withInterface, final String[] topicParts, String suffix,
			char separator, int cnt) throws IOException {

		StringBuilder builder = new StringBuilder();

		boolean first = true;

		if (withInterface) {
			builder.append(this.mqttInterface);
			first = false;
		}

		for (String p : this.baseData.getMqtt().getTopicPrefix().split("/")) {
			if (!first) {
				builder.append(separator);
			} else {
				first = false;
			}
			builder.append(p);
		}

		int size = cnt > 0 ? cnt : topicParts.length;

		for (int i = 1; i < size; ++i) {
			builder.append(separator);
			builder.append(topicParts[i]);

		}

		if (suffix != null) {
			builder.append(separator);
			builder.append(suffix);

		}
		return builder.toString();
	}

	private void appendObject(final Writer writer, TopicData topic) throws IOException {
		String name = topic.getParts()[topic.getParts().length - 1];
		if (name.charAt(0) == '/') {
			name = name.substring(1);
		}

		String writeString = Boolean.toString(!topic.isPublish());
		String readString = Boolean.toString(topic.isPublish());
		String ioBrokerPathWInterface = this.catWithSeparator(true, topic.getBaseParts(), topic.getSuffix(), '.');
		String serverTopic = this.catWithSeparator(false, topic.getParts(), null, '/');

		if (!this.first) {
			writer.append(",\n");
		} else {
			this.first = false;
		}
		writer.append("  \"");
		writer.append(ioBrokerPathWInterface);
		writer.append("\": {\n");
		writer.append("    \"type\": \"state\",\n");
		writer.append("    \"role\": \"text\",\n");
		writer.append("    \"common\": {\n");
		writer.append("      \"name\": \"");
		writer.append(name);
		writer.append("\",\n");
		writer.append("      \"type\": \"mixed\",\n");
		writer.append("      \"read\": ");
		writer.append(readString);
		writer.append(",\n");
		writer.append("      \"write\": ");
		writer.append(writeString);
		writer.append(",\n");
		writer.append("      \"desc\": \"");
		writer.append(Helper.escaping(topic.getComment()));
		writer.append("\",\n");
		writer.append("      \"custom\": {\n");
		writer.append("        \"");
		writer.append(this.mqttInterface);
		writer.append("\": {\n");
		writer.append("          \"enabled\": true,\n");
		writer.append("          \"topic\": \"");
		writer.append(serverTopic);
		writer.append("\",\n");
		writer.append("          \"publish\": ");
		writer.append(writeString);
		writer.append(",\n");
		writer.append("          \"pubChangesOnly\": false,\n");
		writer.append("          \"pubAsObject\": false,\n");
		writer.append("          \"qos\": 0,\n");
		writer.append("          \"retain\": false,\n");
		writer.append("          \"subscribe\": ");
		writer.append(readString);
		writer.append(",\n");
		writer.append("          \"subChangesOnly\": false,\n");
		writer.append("          \"subAsObject\": false,\n");
		writer.append("          \"subQos\": 0,\n");
		writer.append("          \"setAck\": true\n");
		writer.append("        }\n");
		writer.append("      }\n");
		writer.append("    },\n");
		writer.append("    \"native\": {\n");
		writer.append("      \"topic\": \"");
		writer.append(serverTopic);
		writer.append("\"\n");
		writer.append("    },\n");
		writer.append("    \"from\": \"system.adapter.");
		writer.append(this.mqttInterface);
		writer.append("\",\n");
		writer.append("    \"user\": \"system.user.admin\",\n");
		// writer.append(" \"ts\": 1594748406875,\n");
		writer.append("    \"_id\": \"");
		writer.append(ioBrokerPathWInterface);
		writer.append("\",\n");
		writer.append("    \"acl\": {\n");
		writer.append("      \"object\": 1636,\n");
		writer.append("      \"state\": 1636,\n");
		writer.append("      \"owner\": \"system.user.admin\",\n");
		writer.append("      \"ownerGroup\": \"system.group.administrator\"\n");
		writer.append("    }\n");
		writer.append("  }");
	}

	public void writePairingScript(final Instances instances) throws IOException {

		Writer writer = null;
		File directory = instances.getAppendixPath();
		String name = Constants.IoBroker.PAIRING_SCRIPT_NAME;
		File file = new File(directory, name);

		FileHelper.mkdir(directory);

		try {
			writer = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(file), "UTF-8"));
		} catch (UnsupportedEncodingException e) {
			throw new IOException(e);
		}

		writer.append("var channels = [\n");

		boolean first = true;

		for (TopicData topic : TopicType.getWritableReadTopics(instances)) {
			if (topic.isWriteableChannel()) {
				if (!first) {
					writer.append("',\n");
				} else {
					first = false;
				}
				writer.append("    '");
			}
//					String id = data.getChannelInstance().getName();
//					String topic = Mqtt.formatChannelOut(id);

			String baseTopic = catWithSeparator(false, topic.getBaseParts(), null, '.');
			writer.append(baseTopic);
		}

		writer.append("'\n]\n\n");

		String connection = "'" + this.mqttInterface + ".info.connection" + "'";
		TopicData online = TopicType.CLIENT_ONLINE.getTopicData(instances, null, null);
		String onlinePathWInterface = this.catWithSeparator(true, online.getBaseParts(), online.getSuffix(), '.');

		writer.append("if ( getState(" + connection + ") ) {\n");
		writer.append("    init();\n");
		writer.append("} else {\n");
		writer.append("    on ({id: " + connection + ", change: 'ne'}, function(obj){\n");
		writer.append("        var val = obj.state ? obj.state.val : false ;\n");
		writer.append("        if ( val ) {\n");
		writer.append("            init();\n");
		writer.append("        }\n");
		writer.append("    });\n");
		writer.append("}\n\n");

		writer.append("function init() {\n");
		writer.append("    if ( !getState('mqtt-client.0.info.connection') ) {\n");
		writer.append("        return;\n");
		writer.append("    }\n\n");

		writer.append("    // Beim Starten dem SolvisSmartHomeServer mitteilen, dass ioBroker online ist:\n");
		writer.append("    setState('");
		writer.append(onlinePathWInterface);

		writer.append("', 'true');\n\n");

		writer.append("    channels.forEach(function(channel) {\n");
		writer.append(
				"        var solvisread = '" + this.mqttInterface + ".' + channel + '." + Constants.Mqtt.DATA_SUFFIX);
		writer.append("';\n");

		writer.append(
				"        var solviswrite = '" + this.mqttInterface + ".' + channel + '." + Constants.Mqtt.CMND_SUFFIX);
		writer.append("';\n");

		writer.append("        var combined = '" + this.javascriptInterface + ".' + channel + '.rw';\n\n");

		writer.append("        //kombiniertes Objekt erzeugen\n");
		writer.append("        createState(combined);\n\n");

		writer.append("        // Initialen Wert aus Solvis in die kombinierte Objekt schreiben:\n");
		writer.append("        setState(combined, getState(solvisread).val, true);\n\n");

		writer.append("        // Wert wurde durch Solvis geändert:\n");
		writer.append("        on({id: solvisread}, function(obj) {\n");

		writer.append("            var val = obj.state ? obj.state.val : '';\n");
		writer.append("            setState(combined, (obj.state ? obj.state.val : ''), true);\n");
//		writer.append("            if (val != getState(combined).val) {\n");
//		writer.append("                setState(combined, (obj.state ? obj.state.val : ''), true);\n");
//		writer.append("            }\n");

		writer.append("       });\n\n");

		writer.append("        // Wert wird durch ioBroker web.0 geändert\n");
		writer.append("        on({id: combined, change: 'ne'}, function(obj) {\n");
		writer.append("            setState(solviswrite, (obj.state ? (obj.state.val === true ?"
				+ " 1 : (obj.state.val === false ? 0 : obj.state.val)) : ''), false);\n");
		writer.append("        });\n\n");

		writer.append("        // Beim Beenden dem SolvisSmartHomeServer mitteilen, dass ioBroker offline ist\n");
		writer.append("        onStop (function(){\n");
		writer.append("            setState('");
		writer.append(onlinePathWInterface);
		writer.append("', 'false');\n");
		writer.append("        }, 2000);\n");
		writer.append("    });\n}\n");

		writer.flush();
		writer.close();

		System.out.println("File <" + file.getAbsolutePath() + "> created.");

	}

	public static class Creator extends CreatorByXML<IoBroker> {

		private String mqttInterface = Constants.IoBroker.DEFAULT_MQTT_INTERFACE;
		private String javascriptInterface = Constants.IoBroker.DEFAULT_JAVASCRIPT_INTERFACE;

		public Creator(final String id, final BaseCreator<?> creator) {
			super(id, creator);
		}

		@Override
		public void setAttribute(final QName name, final String value) throws XmlException {
			switch (name.getLocalPart()) {
				case "mqttInterface":
					this.mqttInterface = value;
					break;
				case "javascriptInterface":
					this.javascriptInterface = value;
					break;
			}

		}

		@Override
		public IoBroker create() throws XmlException, IOException {
			return new IoBroker(this.mqttInterface, this.javascriptInterface);
		}

		@Override
		public CreatorByXML<?> getCreator(final QName name) {
			return null;
		}

		@Override
		public void created(final CreatorByXML<?> creator, final Object created) throws XmlException {

		}

	}

	public void setBaseData(final BaseData baseData) {
		this.baseData = baseData;

	}
}
