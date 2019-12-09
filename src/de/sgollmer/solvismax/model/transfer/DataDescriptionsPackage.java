package de.sgollmer.solvismax.model.transfer;

import de.sgollmer.solvismax.model.objects.AllDataDescriptions;
import de.sgollmer.solvismax.model.objects.DataDescription;

public class DataDescriptionsPackage extends JsonPackage {
	
	public DataDescriptionsPackage( AllDataDescriptions descriptions ) {
		this.command = Command.DATA_DESCRIPTIONS ;
		this.data = new Frame() ;
		
		for (DataDescription description : descriptions.get()) {
			SolvisDataDescription descr = new SolvisDataDescription(description);
			this.data.add(descr);
		}
		
	}

}
