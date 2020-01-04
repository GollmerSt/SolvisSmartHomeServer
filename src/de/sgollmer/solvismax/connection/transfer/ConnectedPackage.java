/************************************************************************
 * 
 * $Id$
 *
 * 
 ************************************************************************/

package de.sgollmer.solvismax.connection.transfer;

import de.sgollmer.solvismax.Version;
import de.sgollmer.solvismax.model.objects.data.IntegerValue;

public class ConnectedPackage extends JsonPackage {

	public ConnectedPackage(int clientId) {
		this.command = Command.CONNECTED;
		this.data = new Frame();
		Element element = new Element();
		this.data.add(element);
		element.name = "ClientId";
		element.value = new SingleValue(new IntegerValue(clientId));
		element = new Element();
		this.data.add(element);
		element.name = "ServerVersion";
		element.value = new SingleValue(Version.getInstance().getVersion());
		element = new Element();
		this.data.add(element);
		element.name = "FormatVersion";
		element.value = new SingleValue(Version.getInstance().getFormatVersion());
	}
}
