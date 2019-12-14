package de.sgollmer.solvismax.connection.transfer;

import de.sgollmer.solvismax.model.objects.data.IntegerValue;

public class ConnectedPackage extends JsonPackage {

	public ConnectedPackage(int clientId) {
		this.command = Command.CONNECTED;
		this.data = new Frame();
		Element element = new Element();
		this.data.add(element);
		element.name = "ClientId";
		element.value = new SingleValue(new IntegerValue(clientId));
	}
}
