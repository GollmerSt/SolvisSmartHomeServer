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
import de.sgollmer.solvismax.model.objects.AllChannelDescriptions;
import de.sgollmer.solvismax.model.objects.configuration.OfConfigs;
import de.sgollmer.solvismax.model.objects.screen.Screen;

public class DescriptionsPackage implements ISendData {

	private final AllChannelDescriptions descriptions;
	private final Solvis solvis;

	public DescriptionsPackage(AllChannelDescriptions descriptions, Solvis solvis) {
		this.descriptions = descriptions;
		this.solvis = solvis;
	}

	@Override
	public JsonPackage createJsonPackage() {

		Frame frame = new Frame();

		for (OfConfigs<de.sgollmer.solvismax.model.objects.ChannelDescription> confDescriptions : this.descriptions
				.get()) {
			de.sgollmer.solvismax.model.objects.ChannelDescription description = confDescriptions.get(this.solvis);
			if (description != null) {
				ChannelDescription descr = new ChannelDescription(description);
				frame.add(descr);
			}
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
