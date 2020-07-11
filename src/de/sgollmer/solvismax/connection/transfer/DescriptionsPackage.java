/************************************************************************
 * 
 * $Id$
 *
 * 
 ************************************************************************/

package de.sgollmer.solvismax.connection.transfer;

import de.sgollmer.solvismax.connection.ServerCommand;
import de.sgollmer.solvismax.model.objects.AllChannelDescriptions;
import de.sgollmer.solvismax.model.objects.OfConfigs;

public class DescriptionsPackage extends JsonPackage {

	public DescriptionsPackage(AllChannelDescriptions descriptions, int configurationMask) {
		this.command = Command.DESCRIPTIONS;
		this.data = new Frame();

		for (OfConfigs<de.sgollmer.solvismax.model.objects.ChannelDescription> confDescriptions : descriptions.get()) {
			de.sgollmer.solvismax.model.objects.ChannelDescription description = confDescriptions
					.get(configurationMask);
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

	}

}
