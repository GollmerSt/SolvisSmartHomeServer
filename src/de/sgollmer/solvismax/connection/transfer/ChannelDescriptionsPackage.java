package de.sgollmer.solvismax.connection.transfer;

import de.sgollmer.solvismax.model.objects.AllDataDescriptions;
import de.sgollmer.solvismax.model.objects.DataDescription;

public class ChannelDescriptionsPackage extends JsonPackage {
	
	public ChannelDescriptionsPackage( AllDataDescriptions descriptions ) {
		this.command = Command.DATA_DESCRIPTIONS ;
		this.data = new Frame() ;
		
		for (DataDescription description : descriptions.get()) {
			ChannelDescription descr = new ChannelDescription(description);
			this.data.add(descr);
		}
		
	}

}
