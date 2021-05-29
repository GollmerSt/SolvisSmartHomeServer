package de.sgollmer.solvismax.iobroker;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.UnsupportedEncodingException;
import java.io.Writer;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

import javax.xml.namespace.QName;

import de.sgollmer.solvismax.BaseData;
import de.sgollmer.solvismax.Constants;
import de.sgollmer.solvismax.connection.mqtt.Mqtt;
import de.sgollmer.solvismax.helper.FileHelper;
import de.sgollmer.solvismax.model.Instances;
import de.sgollmer.solvismax.model.Solvis;
import de.sgollmer.solvismax.model.objects.ChannelDescription;
import de.sgollmer.solvismax.model.objects.data.SolvisData;
import de.sgollmer.xmllibrary.BaseCreator;
import de.sgollmer.xmllibrary.CreatorByXML;
import de.sgollmer.xmllibrary.XmlException;

public class IoBroker {
	private final String ioBrokerId;
	private final String mqttInterface;
	private final String javascriptInterface;
	private BaseData baseData;

	private IoBroker(final String ioBrokerId, final String mqttInterface, final String javascriptInterface) {
		this.ioBrokerId = ioBrokerId;
		this.mqttInterface = mqttInterface;
		this.javascriptInterface = javascriptInterface;
	}

	public IoBroker() {
		this.ioBrokerId = Constants.IoBroker.DEFAULT_IOBROKER_ID;
		this.mqttInterface = Constants.IoBroker.DEFAULT_MQTT_INTERFACE;
		this.javascriptInterface = Constants.IoBroker.DEFAULT_JAVASCRIPT_INTERFACE;
	}

	private enum Type {
		READ, WRITE, WRITEONLY
	};

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
		this.appendObject(writer, false, false, this.ioBrokerId, Constants.Mqtt.ERROR);
		this.appendObject(writer, true, false, this.ioBrokerId, Constants.Mqtt.ONLINE_STATUS);

		for (Solvis solvis : instances.getUnits()) {

			String solvisId = solvis.getUnit().getId();

			List<SolvisData> list = new ArrayList<>(solvis.getAllSolvisData().getSolvisDatas());
			list.sort(new Comparator<SolvisData>() {

				@Override
				public int compare(SolvisData o1, SolvisData o2) {
					return o1.getId().compareTo(o2.getId());
				}
			});

			for (SolvisData data : list) {
				if (!data.isDontSend()) {
					this.appendChannel(writer, solvisId, data);
				}
			}

			this.appendObject(writer, false, false, solvisId, Constants.Mqtt.HUMAN_ACCESS);
			this.appendObject(writer, false, false, solvisId, Constants.Mqtt.STATUS);
			this.appendChannel(writer, Type.WRITEONLY, false, solvisId, Constants.Mqtt.SCREEN_PREFIX);

		}

		this.appendChannel(writer, Type.WRITEONLY, false, null, Constants.Mqtt.SERVER_PREFIX);
		this.appendObject(writer, false, true, Constants.Mqtt.SERVER_PREFIX, Constants.Mqtt.ONLINE_STATUS);

		writer.append('}');

		writer.flush();
		writer.close();

		System.out.println("File <" + file.getAbsolutePath() + "> created.");
	}

	private void appendWithSeparator(final Writer writer, final boolean withInterface, final boolean withSmartHomeId,
			final char separator, final String... args) throws IOException {

		boolean first = true;

		if (withInterface) {
			writer.append(this.mqttInterface);
			first = false;
		}

		for (String p : this.baseData.getMqtt().getTopicPrefix().split("/")) {
			if (!first) {
				writer.append(separator);
			} else {
				first = false;
			}
			writer.append(p);
		}

		if (withSmartHomeId) {
			writer.append(separator);
			writer.append(this.ioBrokerId);
		}

		for (String a : args) {
			if (a != null) {
				writer.append(separator);
				writer.append(a);

			}
		}
	}

	private void appendObject(final Writer writer, final boolean write, final boolean last, final String... args)
			throws IOException {
		String name = args[args.length - 1];
		if (name.charAt(0) == '/') {
			name = name.substring(1);
		}

		String writeString = Boolean.toString(write);
		String readString = Boolean.toString(!write);

		writer.append("  \"");
		this.appendWithSeparator(writer, true, false, '.', args);
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
		writer.append("      \"desc\": \"created from SolvisSmartHomeServer\",\n");
		writer.append("      \"custom\": {\n");
		writer.append("        \"");
		writer.append(this.mqttInterface);
		writer.append("\": {\n");
		writer.append("          \"enabled\": true,\n");
		writer.append("          \"topic\": \"");
		this.appendWithSeparator(writer, false, write, '/', args);
		writer.append("\",\n");
		writer.append("          \"publish\": ");
		writer.append(Boolean.toString(write));
		writer.append(",\n");
		writer.append("          \"pubChangesOnly\": false,\n");
		writer.append("          \"pubAsObject\": false,\n");
		writer.append("          \"qos\": 0,\n");
		writer.append("          \"retain\": false,\n");
		writer.append("          \"subscribe\": ");
		writer.append(Boolean.toString(!write));
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
		this.appendWithSeparator(writer, false, write, '/', args);
		writer.append("\"\n");
		writer.append("    },\n");
		writer.append("    \"from\": \"system.adapter.");
		writer.append(this.mqttInterface);
		writer.append("\",\n");
		writer.append("    \"user\": \"system.user.admin\",\n");
		// writer.append(" \"ts\": 1594748406875,\n");
		writer.append("    \"_id\": \"");
		this.appendWithSeparator(writer, true, false, '.', args);
		writer.append("\",\n");
		writer.append("    \"acl\": {\n");
		writer.append("      \"object\": 1636,\n");
		writer.append("      \"state\": 1636,\n");
		writer.append("      \"owner\": \"system.user.admin\",\n");
		writer.append("      \"ownerGroup\": \"system.group.administrator\"\n");
		writer.append("    }\n");
		writer.append("  }");
		if (!last) {
			writer.append(",\n");
		}
	}

	private void appendChannel(final Writer writer, final String solvisId, final SolvisData data) throws IOException {
		String name = data.getChannelInstance().getName();
		String topic = Mqtt.formatChannelOut(name);
		this.appendChannel(writer, data.getDescription().isWriteable() ? Type.WRITE : Type.READ,
				data.getDescription().getType() == ChannelDescription.Type.CONTROL, solvisId, topic);
	}

	private void appendChannel(final Writer writer, final Type type, final boolean update, final String solvisId,
			final String topic) throws IOException {

		if (type != Type.WRITEONLY) {
			this.appendObject(writer, false, false, solvisId, topic, Constants.Mqtt.DATA_SUFFIX);
			if (update) {
				this.appendObject(writer, true, false, solvisId, topic, Constants.Mqtt.UPDATE_SUFFIX);
			}
		}
		this.appendObject(writer, false, false, solvisId, topic, Constants.Mqtt.META_SUFFIX);

		if (type != Type.READ) {
			this.appendObject(writer, true, false, solvisId, topic, Constants.Mqtt.CMND_SUFFIX);
		}
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

		for (Solvis solvis : instances.getUnits()) {

			String solvisId = solvis.getUnit().getId();

			List<SolvisData> list = new ArrayList<>(solvis.getAllSolvisData().getSolvisDatas());
			list.sort(new Comparator<SolvisData>() {

				@Override
				public int compare(SolvisData o1, SolvisData o2) {
					return o1.getId().compareTo(o2.getId());
				}
			});

			boolean first = true;

			for (SolvisData data : list) {
				if (!data.isDontSend() && data.getDescription().isWriteable()) {
					if (!first) {
						writer.append("',\n");
					} else {
						first = false;
					}
					writer.append("    '");
					String id = data.getChannelInstance().getName();
					String topic = Mqtt.formatChannelOut(id);
					this.appendWithSeparator(writer, false, false, '.', solvisId, topic);
				}
			}
			writer.append("'\n]\n\n");

			String connection = "'" + this.mqttInterface + ".info.connection" + "'";

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
			this.appendWithSeparator(writer, true, true, '.', Constants.Mqtt.ONLINE_STATUS);
			writer.append("', 'true');\n\n");

			writer.append("    channels.forEach(function(channel) {\n");
			writer.append("        var solvisread = '" + this.mqttInterface + ".' + channel + '."
					+ Constants.Mqtt.DATA_SUFFIX);
			writer.append("';\n");

			writer.append("        var solviswrite = '" + this.mqttInterface + ".' + channel + '."
					+ Constants.Mqtt.CMND_SUFFIX);
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
//			writer.append("            if (val != getState(combined).val) {\n");
//			writer.append("                setState(combined, (obj.state ? obj.state.val : ''), true);\n");
//			writer.append("            }\n");

			writer.append("       });\n\n");

			writer.append("        // Wert wird durch ioBroker web.0 geändert\n");
			writer.append("        on({id: combined, change: 'ne'}, function(obj) {\n");
			writer.append("            setState(solviswrite, (obj.state ? (obj.state.val === true ?"
					+ " 1 : (obj.state.val === false ? 0 : obj.state.val)) : ''), false);\n");
			writer.append("        });\n\n");

			writer.append("        // Beim Beenden dem SolvisSmartHomeServer mitteilen, dass ioBroker offline ist\n");
			writer.append("        onStop (function(){\n");
			writer.append("            setState('");
			this.appendWithSeparator(writer, true, true, '.', Constants.Mqtt.ONLINE_STATUS);
			writer.append("', 'false');\n");
			writer.append("        }, 2000);\n");
			writer.append("    });\n}\n");

		}

		writer.flush();
		writer.close();

		System.out.println("File <" + file.getAbsolutePath() + "> created.");

	}

	public static class Creator extends CreatorByXML<IoBroker> {

		private String mqttInterface = Constants.IoBroker.DEFAULT_MQTT_INTERFACE;
		private String javascriptInterface = Constants.IoBroker.DEFAULT_JAVASCRIPT_INTERFACE;
		private String ioBrokerId = Constants.IoBroker.DEFAULT_IOBROKER_ID;

		public Creator(final String id, final BaseCreator<?> creator) {
			super(id, creator);
		}

		@Override
		public void setAttribute(final QName name, final String value) throws XmlException {
			switch (name.getLocalPart()) {
				case "iobrokerName":
					this.ioBrokerId = value;
					break;
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
			return new IoBroker(this.ioBrokerId, this.mqttInterface, this.javascriptInterface);
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
