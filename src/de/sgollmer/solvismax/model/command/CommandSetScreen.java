package de.sgollmer.solvismax.model.command;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

import de.sgollmer.solvismax.connection.mqtt.MqttData;
import de.sgollmer.solvismax.connection.mqtt.TopicType;
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

	public CommandSetScreen(final AbstractScreen screen) {
		this.screen = screen;
	}

	@Override
	public ResultStatus execute(final Solvis solvis, final Handling.QueueStatus queueStatus) throws IOException,
			TerminationException, PowerOnException, NumberFormatException, TypeException, XmlException {
		solvis.setDefaultScreen(this.screen);
		return ResultStatus.SUCCESS;
	}

	@Override
	protected void notExecuted() {
	}

	public static MqttData getMqttMeta(final Solvis solvis) {
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
		return new MqttData(TopicType.UNIT_SCREEN_META, solvis, null, array.toString(), 0, true);
	}

	@Override
	public String toString() {
		if (this.screen == null) {
			return "SelectScreen is set to default (NONE)";
		} else {
			return "Select " + this.screen.getId();
		}

	}

}
