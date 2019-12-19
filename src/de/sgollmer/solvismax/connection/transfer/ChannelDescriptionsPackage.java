package de.sgollmer.solvismax.connection.transfer;

import de.sgollmer.solvismax.model.objects.AllChannelDescriptions;
import de.sgollmer.solvismax.model.objects.ChannelDescription;

public class ChannelDescriptionsPackage extends JsonPackage {
	
	public ChannelDescriptionsPackage( AllChannelDescriptions descriptions ) {
		this.command = Command.CHANNEL_DESCRIPTIONS ;
		this.data = new Frame() ;
		
		for (ChannelDescription description : descriptions.get()) {
			JsonChannelDescription descr = new JsonChannelDescription(description);
			this.data.add(descr);
		}
		
	}

}
