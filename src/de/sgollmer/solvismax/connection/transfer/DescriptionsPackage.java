/************************************************************************
 * 
 * $Id$
 *
 * 
 ************************************************************************/

package de.sgollmer.solvismax.connection.transfer;

import java.util.Collection;

import de.sgollmer.solvismax.connection.ISendData;
import de.sgollmer.solvismax.connection.ServerCommand;
import de.sgollmer.solvismax.connection.mqtt.MqttData;
import de.sgollmer.solvismax.model.Solvis;
import de.sgollmer.solvismax.model.objects.data.SolvisData;
import de.sgollmer.solvismax.model.objects.screen.Screen;

public class DescriptionsPackage implements ISendData {

	private final Solvis solvis;

	public DescriptionsPackage(final Solvis solvis) {
		this.solvis = solvis;
	}

	@Override
	public JsonPackage createJsonPackage() {

		Frame frame = new Frame();

		for (SolvisData data : this.solvis.getAllSolvisData().getSolvisDatas()) {
			ChannelDescription descr = new ChannelDescription(data.getChannelInstance());
			frame.add(descr);
		}

		for (ServerCommand command : ServerCommand.values()) {
			if (command.shouldCreateMeta()) {
				ServerCommandDescription descr = new ServerCommandDescription(command);
				frame.add(descr);
			}
		}

		for (Screen screen : this.solvis.getSolvisDescription().getScreens().getScreens(this.solvis)) {
			if (!screen.isNoRestore()) {
				SelectScreenDescription descr = new SelectScreenDescription(screen.getId());
				frame.add(descr);
			}
		}
		return new JsonPackage(Command.DESCRIPTIONS, frame);
	}

	@Override
	public Collection<MqttData> createMqttData() {
		return null;
	}

}
