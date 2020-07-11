/************************************************************************
 * 
 * $Id$
 *
 * 
 ************************************************************************/

package de.sgollmer.solvismax.connection.transfer;

import de.sgollmer.solvismax.connection.ITransferedData;
import de.sgollmer.solvismax.connection.ServerCommand;
import de.sgollmer.solvismax.model.objects.data.SingleData;
import de.sgollmer.solvismax.model.objects.data.StringData;

public class ServerCommandPackage extends JsonPackage implements ITransferedData {

	private ServerCommand serverCommand;

	public ServerCommandPackage() {
		super.command = Command.SERVER_COMMAND;
	}

	public ServerCommandPackage(ServerCommand command) {
		this.command = Command.SERVER_COMMAND;
		this.data = new Frame();
		Element element = new Element();
		this.data.add(element);
		element.name = "Command";
		element.value = new SingleValue(command.name());
	}


	@Override
	public void finish() {
		Frame f = this.data;
		if (f.size() > 0) {
			Element e = f.get(0);
			if (e.getName().equals("Command")) {
				if (e.value instanceof SingleValue) {
					this.serverCommand = ServerCommand.valueOf(((SingleValue) e.getValue()).getData().toString());
				}
			}
			this.data = null;
		}

	}

	public ServerCommand getServerCommand() {
		return this.serverCommand;
	}

	@Override
	public SingleData<?> getSingleData() {
		return new StringData(this.serverCommand.name(), 0);
	}

}
