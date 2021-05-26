/************************************************************************
 * 
 * $Id$
 *
 * 
 ************************************************************************/

package de.sgollmer.solvismax.connection.transfer;

import de.sgollmer.solvismax.connection.ServerCommand;
import de.sgollmer.solvismax.model.objects.data.BooleanValue;

public class ServerCommandDescription extends Element {

	ServerCommandDescription(ServerCommand command) {
		super(command.name());
		Frame frame = new Frame();
		this.value = frame;

		Element writeableElement = new Element("Writeable",new SingleValue(new BooleanValue(false, -1) ));
		frame.add(writeableElement);

		Element typeElement = new Element("Type",new SingleValue("ServerCommand"));
		frame.add(typeElement);

	}
}
