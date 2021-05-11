/************************************************************************
 * 
 * $Id$
 *
 * 
 ************************************************************************/

package de.sgollmer.solvismax.connection;

import de.sgollmer.solvismax.connection.mqtt.Mqtt;
import de.sgollmer.solvismax.connection.mqtt.MqttData;
import de.sgollmer.solvismax.connection.transfer.ArrayValue;
import de.sgollmer.solvismax.connection.transfer.SingleValue;
import de.sgollmer.solvismax.model.Solvis;

public enum ServerCommand {
	BACKUP(true), //
	RESTART(true), //
	TERMINATE(true, false), //
	DEBUG_ENABLE(true, false), //
	DEBUG_DISABLE(true, false), //
	SCREEN_RESTORE_INHIBIT(false), //
	SCREEN_RESTORE_ENABLE(false), //
	COMMAND_OPTIMIZATION_INHIBIT(false), //
	COMMAND_OPTIMIZATION_ENABLE(false), //
	GUI_COMMANDS_ENABLE(false), //
	GUI_COMMANDS_DISABLE(false), //
	SERVICE_RESET(false), //
	UPDATE_CHANNELS(false);

	private final boolean general;
	private final boolean createMeta;

	private ServerCommand(boolean general, boolean createMeta) {
		this.general = general;
		this.createMeta = createMeta;
	}

	private ServerCommand(boolean general) {
		this(general, true);
	}

	boolean isGeneral() {
		return this.general;
	}

	public static MqttData getMqttMeta(Solvis solvis) {
		String topic = Mqtt.formatServerMetaTopic();
		ArrayValue array = new ArrayValue();
		for (ServerCommand command : values()) {
			if (command.isGeneral() == (solvis == null) && command.shouldCreateMeta()) {
				array.add(new SingleValue(command.name()));
			}
		}
		if (solvis == null) {
			return new MqttData(topic, array.toString(), 0, true, null);
		} else {
			return new MqttData(solvis, topic, array.toString(), 0, true);
		}
	}

	public boolean shouldCreateMeta() {
		return this.createMeta;
	}
}
