/************************************************************************
 * 
 * $Id$
 *
 * 
 ************************************************************************/

package de.sgollmer.solvismax.connection.transfer;

import de.sgollmer.solvismax.connection.IReceivedData;
import de.sgollmer.solvismax.connection.ServerCommand;
import de.sgollmer.solvismax.error.PackageException;
import de.sgollmer.solvismax.model.objects.data.SingleData;
import de.sgollmer.solvismax.model.objects.data.StringData;

public class ServerCommandPackage extends JsonPackage implements IReceivedData {

	private ServerCommand serverCommand;

	ServerCommandPackage() {
		super.command = Command.SERVER_COMMAND;
	}

	public ServerCommandPackage(final ServerCommand command) {
		this.command = Command.SERVER_COMMAND;
		this.data = new Frame();
		Element element = new Element("Command", new SingleValue(command.name()));
		this.data.add(element);
	}

	@Override
	void finish() throws PackageException {
		Frame f = this.data;
		Element e = f.get("Command");
		this.serverCommand = ServerCommand.valueOf(e.getValue().getSingleData().toString());
		this.data = null;

	}

	@Override
	public SingleData<?> getSingleData() {
		return new StringData(this.serverCommand.name(), -1L);
	}

}
