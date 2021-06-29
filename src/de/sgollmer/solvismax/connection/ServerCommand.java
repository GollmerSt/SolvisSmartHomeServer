/************************************************************************
 * 
 * $Id$
 *
 * 
 ************************************************************************/

package de.sgollmer.solvismax.connection;

import de.sgollmer.solvismax.connection.mqtt.MqttData;
import de.sgollmer.solvismax.connection.mqtt.TopicType;
import de.sgollmer.solvismax.connection.transfer.ArrayValue;
import de.sgollmer.solvismax.connection.transfer.SingleValue;
import de.sgollmer.solvismax.model.Solvis;

public enum ServerCommand {
	BACKUP(true), //
	RESTART(true), //
	TERMINATE(true, false), //
	DEBUG_CLEAR(false, true), //
	SCREEN_RESTORE_INHIBIT(false), //
	SCREEN_RESTORE_ENABLE(false), //
	COMMAND_OPTIMIZATION_INHIBIT(false), //
	COMMAND_OPTIMIZATION_ENABLE(false), //
	GUI_COMMANDS_ENABLE(false), //
	GUI_COMMANDS_DISABLE(false), //
	SERVICE_RESET(false), //
	SERVICE_TRIGGER(false), //
	UPDATE_CHANNELS(false), //
	LOG_STANDARD(true, true), //
	LOG_BUFFERED(true, true);

	private final boolean general;
	private final boolean createMeta;

	private ServerCommand(final boolean general, final boolean createMeta) {
		this.general = general;
		this.createMeta = createMeta;
	}

	private ServerCommand(final boolean general) {
		this(general, true);
	}

	boolean isGeneral() {
		return this.general;
	}

	public static MqttData getMqttMeta(final Solvis solvis) {
		ArrayValue array = new ArrayValue();
		for (ServerCommand command : values()) {
			if (command.isGeneral() == (solvis == null) && command.shouldCreateMeta()) {
				array.add(new SingleValue(command.name()));
			}
		}
		if (solvis == null) {
			return new MqttData(TopicType.SERVER_META, solvis, null, array.toString(), 0, true);
		} else {
			return new MqttData(TopicType.UNIT_SERVER_META, solvis, null, array.toString(), 0, true);
		}
	}

	public boolean shouldCreateMeta() {
		return this.createMeta;
	}
}
