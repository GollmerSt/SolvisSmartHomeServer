package de.sgollmer.solvismax.model;

import java.io.IOException;

import de.sgollmer.solvismax.connection.mqtt.Mqtt;
import de.sgollmer.solvismax.connection.mqtt.MqttData;
import de.sgollmer.solvismax.connection.transfer.ArrayValue;
import de.sgollmer.solvismax.connection.transfer.SingleValue;
import de.sgollmer.solvismax.error.PowerOnException;
import de.sgollmer.solvismax.error.TerminationException;
import de.sgollmer.solvismax.error.TypeException;
import de.sgollmer.solvismax.model.objects.ResultStatus;
import de.sgollmer.solvismax.model.objects.screen.AbstractScreen;
import de.sgollmer.solvismax.model.objects.screen.Screen;
import de.sgollmer.xmllibrary.XmlException;

public class CommandSetScreen extends Command {
	
	private final AbstractScreen screen ;
	
	public CommandSetScreen( AbstractScreen screen) {
		this.screen = screen;
	}

	@Override
	protected ResultStatus execute(Solvis solvis) throws IOException, TerminationException, PowerOnException,
			NumberFormatException, TypeException, XmlException {
		solvis.setDefaultScreen(this.screen);
		return ResultStatus.SUCCESS;
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
