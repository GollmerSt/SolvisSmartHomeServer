/************************************************************************
 * 
 * $Id: Mqtt.java 277 2020-07-19 16:00:49Z stefa_000 $
 *
 * 
 ************************************************************************/

package de.sgollmer.solvismax.connection.mqtt;

import de.sgollmer.solvismax.connection.mqtt.Mqtt.Format;
import de.sgollmer.solvismax.connection.transfer.Command;
import de.sgollmer.solvismax.error.MqttInterfaceException;
import de.sgollmer.solvismax.Constants;

enum SubscribeType {
	SERVER_META(new String[] { Constants.Mqtt.SERVER_PREFIX, Constants.Mqtt.META_SUFFIX, }, //
			0, false, false, null, Format.NONE), //
	SERVER_COMMAND(new String[] { Constants.Mqtt.SERVER_PREFIX, Constants.Mqtt.CMND_SUFFIX }, //
			1, false, false, Command.SERVER_COMMAND, Format.STRING), //
	SERVER_ONLINE(new String[] { Constants.Mqtt.SERVER_PREFIX, "online" }, //
			0, false, false, null, Format.NONE), //
	CLIENT_ONLINE(new String[] { Constants.Mqtt.ONLINE_STATUS }, //
			1, false, false, Command.CLIENT_ONLINE, Format.BOOLEAN), //
	UNIT_STATUS(new String[] { Constants.Mqtt.STATUS }, //
			1, true, false, null, Format.NONE), //
	UNIT_SERVER_COMMAND(new String[] { Constants.Mqtt.SERVER_PREFIX, Constants.Mqtt.CMND_SUFFIX }, //
			2, true, false, Command.SERVER_COMMAND, Format.STRING), //
	UNIT_SCREEN_COMMAND(new String[] { Constants.Mqtt.SCREEN_PREFIX, Constants.Mqtt.CMND_SUFFIX }, 2, true, false,
			Command.SELECT_SCREEN, Format.STRING), //
	UNIT_CHANNEL_COMMAND(new String[] { Constants.Mqtt.CMND_SUFFIX }, //
			3, true, true, Command.SET, Format.FROM_META), //
	UNIT_CHANNEL_UPDATE(new String[] { Constants.Mqtt.UPDATE_SUFFIX }, //
			3, true, true, Command.GET, Format.NONE), //
	UNIT_CHANNEL_DATA(new String[] { Constants.Mqtt.DATA_SUFFIX }, //
			2, true, true, null, Format.NONE), //
	UNIT_CHANNEL_META(new String[] { Constants.Mqtt.META_SUFFIX }, //
			2, true, true, null, Format.NONE); //

	private final String[] cmp;
	private final int position;
	private final boolean unitId;
	private final boolean channelId;
	private final Command command;
	private final Format format;

	private SubscribeType(final String[] cmp, final int position, final boolean unitId, final boolean channelId,
			Command command, final Format format) {
		this.cmp = cmp;
		this.position = position;
		this.unitId = unitId;
		this.channelId = channelId;
		this.command = command;
		this.format = format;
	}

	static SubscribeType get(final String[] partsWoPrefix) throws MqttInterfaceException {
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

	boolean hasUnitId() {
		return this.unitId;
	}

	boolean hasChannelId() {
		return this.channelId;
	}

	Command getCommand() {
		return this.command;
	}

	public Format getFormat() {
		return this.format;
	}

}