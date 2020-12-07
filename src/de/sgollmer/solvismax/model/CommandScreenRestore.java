/************************************************************************
 * 
 * $Id$
 *
 * 
 ************************************************************************/

package de.sgollmer.solvismax.model;

import java.io.IOException;

import de.sgollmer.solvismax.connection.mqtt.Mqtt;
import de.sgollmer.solvismax.connection.mqtt.MqttData;
import de.sgollmer.solvismax.connection.transfer.ArrayValue;
import de.sgollmer.solvismax.connection.transfer.SingleValue;
import de.sgollmer.solvismax.error.PowerOnException;
import de.sgollmer.solvismax.model.objects.ResultStatus;
import de.sgollmer.solvismax.model.objects.screen.Screen;

public class CommandScreenRestore extends Command {

	private final boolean enable;

	public CommandScreenRestore(boolean enable) {
		this.enable = enable;
	}

	@Override
	protected ResultStatus execute(Solvis solvis) throws IOException, PowerOnException {
		solvis.screenRestore(this.enable);
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
