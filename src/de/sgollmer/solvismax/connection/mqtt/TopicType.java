/************************************************************************
 * 
 * $Id: Mqtt.java 277 2020-07-19 16:00:49Z stefa_000 $
 *
 * 
 ************************************************************************/

package de.sgollmer.solvismax.connection.mqtt;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.List;

import de.sgollmer.solvismax.Constants;
import de.sgollmer.solvismax.connection.mqtt.Mqtt.Format;
import de.sgollmer.solvismax.connection.transfer.Command;
import de.sgollmer.solvismax.log.LogManager;
import de.sgollmer.solvismax.log.LogManager.ILogger;
import de.sgollmer.solvismax.model.Instances;
import de.sgollmer.solvismax.model.Solvis;
import de.sgollmer.solvismax.model.objects.ChannelDescription;
import de.sgollmer.solvismax.model.objects.data.SolvisData;
import de.sgollmer.solvismax.model.objects.data.SolvisData.SmartHomeData;

public enum TopicType {
	SERVER_META(new String[] { Constants.Mqtt.SERVER }, Constants.Mqtt.META_SUFFIX, //
			false, false, false, null, Format.NONE, "Meta description of the server commands"), //
	SERVER_COMMAND(new String[] { Constants.Mqtt.SERVER }, Constants.Mqtt.CMND_SUFFIX, //
			true, false, false, Command.SERVER_COMMAND, Format.STRING, "Server command"), //
	SERVER_ONLINE(new String[] { Constants.Mqtt.SERVER, Constants.Mqtt.ONLINE_STATUS }, null, //
			false, false, false, null, Format.NONE, "Online status of the server"), //
	CLIENT_ONLINE(new String[] { Constants.Mqtt.ONLINE_STATUS }, null, //
			true, false, false, Command.CLIENT_ONLINE, Format.BOOLEAN, "Online status of the Smarthome system"), //
	UNIT_SERVER_META(new String[] { Constants.Mqtt.SERVER }, Constants.Mqtt.META_SUFFIX, //
			false, true, false, null, Format.NONE, "Meta description of the unit related server commands"), //
	UNIT_SERVER_COMMAND(new String[] { Constants.Mqtt.SERVER }, Constants.Mqtt.CMND_SUFFIX, //
			true, true, false, Command.SERVER_COMMAND, Format.STRING, "Unit related server command"), //
	UNIT_SCREEN_META(new String[] { Constants.Mqtt.SCREEN_PREFIX }, Constants.Mqtt.META_SUFFIX, //
			false, true, false, null, Format.NONE, "Meta description of the screen commands"), //
	UNIT_SCREEN_COMMAND(new String[] { Constants.Mqtt.SCREEN_PREFIX }, Constants.Mqtt.CMND_SUFFIX, //
			true, true, false, Command.SELECT_SCREEN, Format.STRING, "Screen command"), //
	UNIT_DEBUG_CHANNEL_META(new String[] { Constants.Mqtt.DEBUG_CHANNEL_PREFIX }, Constants.Mqtt.META_SUFFIX, //
			false, true, false, Command.DEBUG_CHANNEL, Format.STRING,
			"Meta description of the channel debug command (empty)"), //
	UNIT_DEBUG_CHANNEL_COMMAND(new String[] { Constants.Mqtt.DEBUG_CHANNEL_PREFIX }, Constants.Mqtt.CMND_SUFFIX, //
			true, true, false, Command.DEBUG_CHANNEL, Format.STRING, "Debug command"), //
	UNIT_STATUS(new String[] { Constants.Mqtt.STATUS }, null, //
			false, true, false, null, Format.NONE, "Status of the unit (e.g. connection, power off)"), //
	UNIT_HUMAN(new String[] { Constants.Mqtt.HUMAN_ACCESS }, null, //
			false, true, false, null, Format.NONE, "Human access status of the unit (e.g. user, service)"), //
	UNIT_CONTROL(new String[] { Constants.Mqtt.CONTROL }, null, //
			false, true, false, null, Format.NONE, "Execution status of the Gui accesses"), //
	// Channel must be at the end of the list, because the topics are nou unique
	UNIT_CHANNEL_META(new String[0], Constants.Mqtt.META_SUFFIX, //
			false, true, true, null, Format.NONE, "Meta description of channel {}"), //
	UNIT_CHANNEL_COMMAND(new String[0], Constants.Mqtt.CMND_SUFFIX, //
			true, true, true, true, true, Command.SET, Format.FROM_META, "Set value of channel {}"), //
	UNIT_CHANNEL_UPDATE(new String[0], Constants.Mqtt.UPDATE_SUFFIX, //
			true, true, true, false, true, Command.GET, Format.NONE, "Update of channel {}"), //
	UNIT_CHANNEL_DATA(new String[0], Constants.Mqtt.DATA_SUFFIX, //
			false, true, true, null, Format.NONE, "Current data of channel {}"); //

	// Keinen Metas für letzten 3?

	private static final ILogger logger = LogManager.getInstance().getLogger(TopicType.class);

	private final String[] parts;
	private final String suffix;
	private final boolean hasClientId;
	private final boolean hasUnitId;
	private final boolean hasChannelId;
	private final int position;
	private final int partCnt;
	private final Command command;
	private final Format format;
	private final boolean onlyWriteChannels;
	private final boolean onlyPollingChannel;
	private final String comment;

	/**
	 * 
	 * @param cmp
	 * @param hasClientId
	 * @param hasUnitId
	 * @param hasChannelId
	 * @param onlyWriteChannels
	 * @param command           Command, null if publish topic
	 * @param format
	 * @param comment
	 */

	private TopicType(final String[] cmp, final String suffix, final boolean hasClientId, final boolean hasUnitId,
			final boolean hasChannelId, final boolean onlyWriteChannels, final boolean onlyPollingChannel,
			Command command, final Format format, final String comment) {
		this.parts = cmp;
		this.suffix = suffix;
		this.hasClientId = hasClientId;
		this.hasUnitId = hasUnitId;
		this.hasChannelId = hasChannelId;
		this.position = (hasClientId ? 1 : 0) + (hasUnitId ? 1 : 0) + (hasChannelId ? 1 : 0);
		this.partCnt = this.position + cmp.length + (this.suffix != null ? 1 : 0) + 1;
		this.command = command;
		this.format = format;
		this.onlyWriteChannels = onlyWriteChannels;
		this.onlyPollingChannel = onlyPollingChannel;
		this.comment = comment;
	}

	private TopicType(final String[] cmp, final String suffix, final boolean hasClientId, final boolean hasUnitId,
			final boolean hasChannelId, Command command, final Format format, final String comment) {
		this(cmp, suffix, hasClientId, hasUnitId, hasChannelId, false, false, command, format, comment);
	}

	private boolean hits(final String[] partsWoPrefix) {
		int cmpLength = this.parts.length + (this.suffix == null ? 0 : 1);
		if (cmpLength > partsWoPrefix.length - this.position) {
			return false;
		}
		int i;
		for (i = 0; i < this.parts.length; ++i) {
			if (!this.parts[i].equalsIgnoreCase(partsWoPrefix[i + this.position])) {
				return false;
			}
		}
		if (this.suffix != null && !this.suffix.equalsIgnoreCase(partsWoPrefix[i + this.position])) {
			return false;
		}
		return true;
	}

	static TopicType get(final String[] partsWoPrefix) {
		for (TopicType type : TopicType.values()) {
			if (type.hits(partsWoPrefix)) {
				return type;
			}
		}
		return null;
	}

	boolean hasUnitId() {
		return this.hasUnitId;
	}

	boolean hasChannelId() {
		return this.hasChannelId;
	}

	Command getCommand() {
		return this.command;
	}

	public Format getFormat() {
		return this.format;
	}

	public boolean isPublish() {
		return this.command == null;
	}

	public boolean isUnitDependend() {
		return this.hasUnitId;
	}

	public String[] getTopicParts(Instances instances, final Solvis solvis, final String channelId, boolean base) {
		int length = this.partCnt - (base && this.suffix != null ? 1 : 0) - (base && this.hasClientId ? 1 : 0);
		String[] parts = new String[length];
		int i = 0;

		if (instances == null) {
			logger.error("Progarmming error: Instances object is necessary, but null");
			return null;
		}

		parts[i++] = instances.getMqtt().getTopicPrefix();

		if (this.hasClientId && !base) {
			parts[i++] = instances.getMqtt().getSmartHomeId();
		}
		if (this.hasUnitId) {
			if (solvis == null) {
				logger.error("Progarmming error: Solvis object is necessary, but null");
				return null;
			}

			parts[i++] = solvis.getUnit().getId();
		}
		if (this.hasChannelId) {
			if (channelId == null) {
				logger.error("Progarmming error: Channel id is necessary, but null");
				return null;
			}

			parts[i++] = formatChannelPublish(channelId);
		}
		for (String part : this.parts) {
			parts[i++] = part;
		}

		if (!base && this.suffix != null) {
			parts[i++] = this.suffix;
		}
		return parts;
	}

	public String[] getTopicParts(Instances instances, final Solvis solvis, final String channelId) {
		return this.getTopicParts(instances, solvis, channelId, false);
	}

	private static String getTopic(String[] parts) {
		StringBuilder builder = new StringBuilder(parts[0]);

		for (int i = 1; i < parts.length; ++i) {
			builder.append('/');
			builder.append(parts[i]);
		}

		return builder.toString();
	}

	public static class TopicData {
		private final String topic;
		private final String[] parts;
		private final String[] baseParts;
		private final String suffix;
		private final boolean publish;
		private final boolean writeableChannel;
		private final String comment;

		private TopicData(final String topic, final String[] parts, final String[] baseParts, final String suffix,
				final boolean publish, final boolean writeableChannel, final String comment) {
			this.topic = topic;
			this.parts = parts;
			this.baseParts = baseParts;
			this.suffix = suffix;
			this.publish = publish;
			this.writeableChannel = writeableChannel;
			this.comment = comment;
		}

		public String getTopic() {
			return this.topic;
		}

		public String getComment() {
			return this.comment;
		}

		public String[] getParts() {
			return this.parts;
		}

		public boolean isPublish() {
			return this.publish;
		}

		public boolean isWriteableChannel() {
			return this.writeableChannel;
		}

		public String[] getBaseParts() {
			return this.baseParts;
		}

		public String getSuffix() {
			return this.suffix;
		}
	}

	public TopicData getTopicData(Instances instances, Solvis solvis, SolvisData data) {
		TopicData topicData = null;
		if (this.hasChannelId()) {
			if (data == null) {
				logger.error("Progarmming error: SolvisData object is necessary, but null");
				return null;
			}
			ChannelDescription description = data.getDescription();
			boolean writable = description.isWriteable();
			boolean polling = description.mustPolling();
			if (!(this.isOnlyWriteChannels() && !writable) //
					&& !(this.isOnlyPollingChannel() && !polling)) {
				SmartHomeData smartHomeData = data.getSmartHomeData();
				if (smartHomeData != null && !data.isDontSend()) {
					String name = formatChannelSubscribe(smartHomeData.getName());
					String comment = this.getComment().replace("{}", "<" + name + ">");
					String[] parts = this.getTopicParts(instances, solvis, name);
					String[] baseParts = this.getTopicParts(instances, solvis, name, true);
					topicData = new TopicData(getTopic(parts), parts, baseParts, this.suffix, this.isPublish(),
							writable, comment);
				}
			}
		} else {
			String[] parts = this.getTopicParts(instances, solvis, null);
			String[] baseParts = this.getTopicParts(instances, solvis, null, true);
			topicData = new TopicData(getTopic(parts), parts, baseParts, this.suffix, this.isPublish(), false,
					this.getComment());
		}
		return topicData;
	}

	private static void add(final Collection<TopicData> collection, final TopicData topicData) {
		if (topicData != null) {
			collection.add(topicData);
		}
	}

	public static Collection<TopicData> getTopicDatas(final Instances instances) {

		Collection<TopicData> out = new ArrayList<>();

		for (TopicType type : values()) {
			if (!type.isUnitDependend()) {
				add(out, type.getTopicData(instances, null, null));
			}
		}
		for (Solvis solvis : instances.getUnits()) {
			List<SolvisData> list = new ArrayList<>(solvis.getAllSolvisData().getSolvisDatas());
			list.sort(new CompareSolvisData());
			for (TopicType type : values()) {
				if (type.isUnitDependend()) {
					if (type.hasChannelId()) {
						for (SolvisData data : list) {
							add(out, type.getTopicData(instances, solvis, data));
						}
					} else {
						add(out, type.getTopicData(instances, solvis, null));
					}
				}
			}
		}
		return out;
	}

	public static Collection<TopicData> getWritableReadTopics(final Instances instances) {

		Collection<TopicData> out = new ArrayList<>();

		for (Solvis solvis : instances.getUnits()) {
			TopicType type = UNIT_CHANNEL_UPDATE;
			List<SolvisData> list = new ArrayList<>(solvis.getAllSolvisData().getSolvisDatas());
			list.sort(new CompareSolvisData());
			for (SolvisData data : list) {
				boolean writable = data.getDescription().isWriteable();
				if (writable) {
					add(out, type.getTopicData(instances, solvis, data));
				}
			}
		}
		return out;
	}

	public boolean isOnlyWriteChannels() {
		return this.onlyWriteChannels;
	}

	public String getComment() {
		return this.comment;
	}

	public boolean isOnlyPollingChannel() {
		return this.onlyPollingChannel;
	}

	private static class CompareSolvisData implements Comparator<SolvisData> {

		@Override
		public int compare(SolvisData o1, SolvisData o2) {
			return o1.getName().compareTo(o2.getName());
		}

	}

	public static String formatChannelPublish(final String channelId) {
		return channelId.replace('.', ':');
	}
		
		public static String formatChannelSubscribe(final String channelId) {
		return channelId.replace(':', '.');
	}

	public String formatSuffix() {
		return this.formatSuffix(null);
	}

	public String formatSuffix(final String channelId) {
		StringBuilder builder = new StringBuilder();
		boolean first = true;
		for (String part : this.parts) {
			if (first) {
				first = false;
			} else {
				builder.append('/');
			}
			builder.append(part);
		}
		if (channelId != null) {
			if (first) {
				first = false;
			} else {
				builder.append('/');
			}
			builder.append(formatChannelPublish(channelId));
		}
		if (this.suffix != null) {
			if (first) {
				first = false;
			} else {
				builder.append('/');
			}
			builder.append(this.suffix);
		}
		return builder.toString();
	}

}