/************************************************************************
 * 
 * $Id$
 *
 * 
 ************************************************************************/

package de.sgollmer.solvismax.connection.transfer;

import de.sgollmer.solvismax.connection.IReceivedData;
import de.sgollmer.solvismax.connection.ServerCommand;
import de.sgollmer.solvismax.model.objects.data.SingleData;
import de.sgollmer.solvismax.model.objects.data.StringData;

public class DebugChannelPackage extends JsonPackage implements IReceivedData {

	private String channelEquation;

	DebugChannelPackage() {
		super.command = Command.DEBUG_CHANNEL;
	}

	public DebugChannelPackage(final ServerCommand command) {
		this.command = Command.DEBUG_CHANNEL;
		this.data = new Frame();
		Element element = new Element("ChannelEquation", new SingleValue(command.name()));
		this.data.add(element);
	}

	@Override
	void finish() {
		Frame f = this.data;
		if (f.size() > 0) {
			Element e = f.get(0);
			if (e.getName().equals("ChannelEquation")) {
				if (e.value instanceof SingleValue) {
					this.channelEquation = ((SingleValue) e.getValue()).getData().toString();
				}
			}
			this.data = null;
		}

	}

	@Override
	public SingleData<?> getSingleData() {
		return new StringData(this.channelEquation, -1L);
	}

}
