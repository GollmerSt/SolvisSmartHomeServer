package de.sgollmer.solvismax.connection.transfer;

import de.sgollmer.solvismax.model.objects.AllChannelDescriptions;

public class ChannelDescriptionsPackage extends JsonPackage {
	
	public ChannelDescriptionsPackage( AllChannelDescriptions descriptions ) {
		this.command = Command.CHANNEL_DESCRIPTIONS ;
		this.data = new Frame() ;
		
		for (de.sgollmer.solvismax.model.objects.ChannelDescription description : descriptions.get()) {
			ChannelDescription descr = new ChannelDescription(description);
			this.data.add(descr);
		}
		
	}

}
