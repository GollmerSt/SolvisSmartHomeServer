/************************************************************************
 * 
 * $Id$
 *
 * 
 ************************************************************************/

package de.sgollmer.solvismax.connection.transfer;

import de.sgollmer.solvismax.connection.ISendData;
import de.sgollmer.solvismax.connection.ServerCommand;
import de.sgollmer.solvismax.model.Solvis;
import de.sgollmer.solvismax.model.objects.AllChannelDescriptions;
import de.sgollmer.solvismax.model.objects.configuration.OfConfigs;
import de.sgollmer.solvismax.model.objects.screen.Screen;

public class DescriptionsPackage extends JsonPackage implements ISendData {

	public DescriptionsPackage(AllChannelDescriptions descriptions, Solvis solvis) {
		this.command = Command.DESCRIPTIONS;
		this.data = new Frame();

		for (OfConfigs<de.sgollmer.solvismax.model.objects.ChannelDescription> confDescriptions : descriptions.get()) {
			de.sgollmer.solvismax.model.objects.ChannelDescription description = confDescriptions.get(solvis);
			if (description != null) {
				ChannelDescription descr = new ChannelDescription(description);
				this.data.add(descr);
			}
		}

		for (ServerCommand command : ServerCommand.values()) {
			if (command.shouldCreateMeta()) {
				ServerCommandDescription descr = new ServerCommandDescription(command);
				this.data.add(descr);
			}
		}

		for (Screen screen : solvis.getSolvisDescription().getScreens().getScreens(solvis)) {
			if (!screen.isNoRestore()) {
				SelectScreenDescription descr = new SelectScreenDescription(screen.getId());
				this.data.add(descr);
			}
		}

	}

	@Override
	public JsonPackage createJsonPackage() {
		return this;
	}

}
