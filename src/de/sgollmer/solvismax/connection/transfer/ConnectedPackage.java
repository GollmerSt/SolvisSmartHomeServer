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

	private int clientId;

	public int getClientId() {
		return this.clientId;
	}
	
	public ConnectedPackage() {
	}


	public ConnectedPackage(int clientId) {
		this.clientId = clientId;
		this.command = Command.CONNECTED;
		this.data = new Frame();
		Element element = new Element();
		this.data.add(element);
		element.name = "ClientId";
		element.value = new SingleValue(new IntegerValue(clientId, -1));
		element = new Element();
		this.data.add(element);
		element.name = "ServerVersion";
		element.value = new SingleValue(Version.getInstance().getVersion());
		element = new Element();
		this.data.add(element);
		element.name = "FormatVersion";
		element.value = new SingleValue(Version.getInstance().getFormatVersion());
	}

	@Override
	public void finish() {
		Frame frame = this.data;
		for (Element e : frame.elements) {
			String id = e.name;
			if (id.equals("ClientId")) {
				if (e.value instanceof SingleValue) {
					SingleValue sv = (SingleValue) e.value;
					this.clientId = sv.getData().getInt();
				}
			}
		}
		this.data = null;
	}

}
