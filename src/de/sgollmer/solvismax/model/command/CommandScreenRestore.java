/************************************************************************
 * 
 * $Id$
 *
 * 
 ************************************************************************/

package de.sgollmer.solvismax.model.command;

import java.io.IOException;

import de.sgollmer.solvismax.connection.mqtt.Mqtt;
import de.sgollmer.solvismax.connection.mqtt.MqttData;
import de.sgollmer.solvismax.connection.transfer.ArrayValue;
import de.sgollmer.solvismax.connection.transfer.SingleValue;
import de.sgollmer.solvismax.error.PowerOnException;
import de.sgollmer.solvismax.model.Solvis;
import de.sgollmer.solvismax.model.objects.ResultStatus;
import de.sgollmer.solvismax.model.objects.screen.Screen;

public class CommandScreenRestore extends Command {

	private final boolean enable;
	private final Object service;

	public CommandScreenRestore(boolean enable, Object service) {
		this.enable = enable;
		this.service = service;
	}

	@Override
	public ResultStatus execute(Solvis solvis) throws IOException, PowerOnException {
		solvis.screenRestore(this.enable, this.service);
		return ResultStatus.SUCCESS;
	}

	@Override
	public String toString() {
		return "Screen restore is switched " + (this.enable ? "on." : "off.");

	}

	@Override
	protected void notExecuted() {
	}

	public static MqttData getMqttMeta(Solvis solvis) {
		String topic = Mqtt.formatScreenMetaTopic();
		ArrayValue array = new ArrayValue();
		for (Screen screen : solvis.getSolvisDescription().getScreens().getScreens(solvis)) {
			array.add(new SingleValue(screen.getId()));
		}
		return new MqttData(solvis, '/' + topic, array.toString(), 0, true);
	}

}
