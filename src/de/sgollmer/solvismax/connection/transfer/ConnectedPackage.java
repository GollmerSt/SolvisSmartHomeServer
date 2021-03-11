/************************************************************************
 * 
 * $Id$
 *
 * 
 ************************************************************************/

package de.sgollmer.solvismax.connection.transfer;

import java.util.Collection;

import de.sgollmer.solvismax.Version;
import de.sgollmer.solvismax.connection.ISendData;
import de.sgollmer.solvismax.connection.mqtt.MqttData;
import de.sgollmer.solvismax.model.objects.data.IntegerValue;

public class ConnectedPackage extends JsonPackage implements ISendData {

	private int clientId;

	@Override
	public String getClientId() {
		return Integer.toBinaryString(this.clientId);
	}

	ConnectedPackage() {
	}

	public ConnectedPackage(int clientId) {
		this.clientId = clientId;
		this.command = Command.CONNECTED;
		this.data = new Frame();
		Element element = new Element();
		element.name = "ClientId";
		element.value = new SingleValue(new IntegerValue(clientId, -1));
		this.data.add(element);
		element = new Element();
		element.name = "ServerVersion";
		element.value = new SingleValue(Version.getInstance().getVersion());
		this.data.add(element);
		if (Version.getInstance().getBuildDate() != null) {
			element = new Element();
			element.name = "BuildDate";
			element.value = new SingleValue(Version.getInstance().getBuildDate());
			this.data.add(element);
		}
		element = new Element();
		element.name = "FormatVersion";
		element.value = new SingleValue(Version.getInstance().getServerFormatVersion());
		this.data.add(element);
	}

	@Override
	void finish() {
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

	@Override
	public JsonPackage createJsonPackage() {
		return this;
	}

	@Override
	public Collection<MqttData> createMqttData() {
		return null;
	}

}
