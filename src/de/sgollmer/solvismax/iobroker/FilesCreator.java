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

import de.sgollmer.solvismax.Constants;
import de.sgollmer.solvismax.connection.mqtt.Mqtt;
import de.sgollmer.solvismax.helper.FileHelper;
import de.sgollmer.solvismax.model.Instances;
import de.sgollmer.solvismax.model.Solvis;
import de.sgollmer.solvismax.model.objects.ChannelDescription;
import de.sgollmer.solvismax.model.objects.data.SolvisData;

public class FilesCreator {
	private final Instances instances;
	private final String mqttInterface;
	private final String javascriptInterface;
	private final String[] prefix;
	private final String smartHomeId;

	public FilesCreator(final Instances instances, final String ioBrokerInterface, final String javascriptInterface,
			final String[] prefix, final String smartHomeId) {
		this.instances = instances;
		this.mqttInterface = ioBrokerInterface;
		this.javascriptInterface = javascriptInterface;
		this.prefix = prefix;
		this.smartHomeId = smartHomeId;
	}

	private enum Type {
		READ, WRITE, WRITEONLY
	};

	public void writeObjectList() throws IOException {

		Writer writer = null;
		File directory = this.instances.getAppendixPath();
		String name = this.mqttInterface + '.' + Constants.IoBroker.OBJECT_LIST_NAME;
		File file = new File(directory, name);

		FileHelper.mkdir(directory);

		try {
			writer = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(file), "UTF-8"));
		} catch (UnsupportedEncodingException e) {
			throw new IOException(e);
		}

		writer.append("{\n");
		this.appendObject(writer, false, false, this.smartHomeId, Constants.Mqtt.ERROR);
		this.appendObject(writer, false, false, this.smartHomeId, Constants.Mqtt.ONLINE_STATUS);

		for (Solvis solvis : this.instances.getUnits()) {

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

		for (String p : this.prefix) {
			if (!first) {
				writer.append(separator);
			} else {
				first = false;
			}
			writer.append(p);
		}

		if (withSmartHomeId) {
			writer.append(separator);
			writer.append(this.smartHomeId);
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
		writer.append("      \"read\": true,\n");
		writer.append("      \"write\": true,\n");
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
				this.appendObject(writer, false, false, solvisId, topic, Constants.Mqtt.UPDATE_SUFFIX);
			}
		}
		this.appendObject(writer, false, false, solvisId, topic, Constants.Mqtt.META_SUFFIX);

		if (type != Type.READ) {
			this.appendObject(writer, true, false, solvisId, topic, Constants.Mqtt.CMND_SUFFIX);
		}
	}

	public void writePairingScript() throws IOException {

		Writer writer = null;
		File directory = this.instances.getAppendixPath();
		String name = Constants.IoBroker.PAIRING_SCRIPT_NAME;
		File file = new File(directory, name);

		FileHelper.mkdir(directory);

		try {
			writer = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(file), "UTF-8"));
		} catch (UnsupportedEncodingException e) {
			throw new IOException(e);
		}

		writer.append("var channels = [\n");

		for (Solvis solvis : this.instances.getUnits()) {

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
			writer.append("            if (val != getState(combined).val) {\n");
			writer.append("                setState(combined, (obj.state ? obj.state.val : ''), true);\n");
			writer.append("            }\n");

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

}
