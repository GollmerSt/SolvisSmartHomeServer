package de.sgollmer.solvismax.model.command;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

import de.sgollmer.solvismax.connection.mqtt.Mqtt;
import de.sgollmer.solvismax.connection.mqtt.MqttData;
import de.sgollmer.solvismax.connection.transfer.ArrayValue;
import de.sgollmer.solvismax.connection.transfer.SingleValue;
import de.sgollmer.solvismax.error.PowerOnException;
import de.sgollmer.solvismax.error.TerminationException;
import de.sgollmer.solvismax.error.TypeException;
import de.sgollmer.solvismax.model.Solvis;
import de.sgollmer.solvismax.model.objects.ResultStatus;
import de.sgollmer.solvismax.model.objects.screen.AbstractScreen;
import de.sgollmer.solvismax.model.objects.screen.Screen;
import de.sgollmer.xmllibrary.XmlException;

public class CommandSetScreen extends Command {

	private final AbstractScreen screen;

	public CommandSetScreen(AbstractScreen screen) {
		this.screen = screen;
	}

	@Override
	public ResultStatus execute(Solvis solvis) throws IOException, TerminationException, PowerOnException,
			NumberFormatException, TypeException, XmlException {
		solvis.setDefaultScreen(this.screen);
		return ResultStatus.SUCCESS;
	}

	@Override
	protected void notExecuted() {
	}

	public static MqttData getMqttMeta(Solvis solvis) {
		String topic = Mqtt.formatScreenMetaTopic();
		List<SingleValue> values = new ArrayList<>();
		for (Screen screen : solvis.getSolvisDescription().getScreens().getScreens(solvis)) {
			if (!screen.isNoRestore()) {
				values.add(new SingleValue(screen.getId()));
			}
		}
		values.sort(new Comparator<SingleValue>() {

			@Override
			public int compare(SingleValue o1, SingleValue o2) {
				if (o1 != null) {
					return o1.toString().compareTo(o2.toString());
				} else if (o2 == null) {
					return 0;
				} else {
					return -1;
				}
			}
		});
		ArrayValue array = new ArrayValue(values);
		return new MqttData(solvis, '/' + topic, array.toString(), 0, true);
	}

	@Override
	public String toString() {
		return "Select " + this.screen.getId();

	}

}
