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

enum SubscribeType {
	SERVER_META(new String[] { "server", "meta" }, 0, false, false, null, Format.NONE), //
	SERVER_COMMAND(new String[] { "server", "cmnd" }, 1, false, false, Command.SERVER_COMMAND, Format.STRING), //
	SERVER_ONLINE(new String[] { "server", "online" }, 0, false, false, null, Format.NONE), //
	CLIENT_ONLINE(new String[] { "online" }, 1, false, false, Command.CLIENT_ONLINE, Format.BOOLEAN), //
	UNIT_STATUS(new String[] { "status" }, 1, true, false, null, Format.NONE), //
	UNIT_SERVER_COMMAND(new String[] { "server", "cmnd" }, 2, true, false, Command.SERVER_COMMAND, Format.STRING), //
	UNIT_SCREEN_COMMAND(new String[] { "screen", "cmnd" }, 2, true, false, Command.SELECT_SCREEN, Format.STRING), //
	UNIT_CHANNEL_COMMAND(new String[] { "cmnd" }, 3, true, true, Command.SET, Format.FROM_META), //
	UNIT_CHANNEL_UPDATE(new String[] { "update" }, 3, true, true, Command.GET, Format.NONE), //
	UNIT_CHANNEL_DATA(new String[] { "data" }, 2, true, true, null, Format.NONE), //
	UNIT_CHANNEL_META(new String[] { "meta" }, 2, true, true, null, Format.NONE); //

	private final String[] cmp;
	private final int position;
	private final boolean unitId;
	private final boolean channelId;
	private final Command command;
	final Format format;

	private SubscribeType(String[] cmp, int position, boolean unitId, boolean channelId, Command command,
			Format format) {
		this.cmp = cmp;
		this.position = position;
		this.unitId = unitId;
		this.channelId = channelId;
		this.command = command;
		this.format = format;
	}

	static SubscribeType get(String[] partsWoPrefix) throws MqttInterfaceException {
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

}