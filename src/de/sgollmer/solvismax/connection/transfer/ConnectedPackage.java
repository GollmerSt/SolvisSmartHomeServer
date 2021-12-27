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
import de.sgollmer.solvismax.error.PackageException;
import de.sgollmer.solvismax.error.TypeException;
import de.sgollmer.solvismax.model.objects.data.LongValue;
import de.sgollmer.solvismax.model.objects.data.SingleData;

public class ConnectedPackage extends JsonPackage implements ISendData {

	private long clientId;

	@Override
	public String getClientId() {
		return Long.toBinaryString(this.clientId);
	}

	ConnectedPackage() {
	}

	public ConnectedPackage(final long clientId) {
		this.clientId = clientId;
		this.command = Command.CONNECTED;
		this.data = new Frame();
		Element element = new Element("ClientId", new SingleValue(new LongValue(clientId, -1)));
		this.data.add(element);
		element = new Element("ServerVersion", new SingleValue(Version.getInstance().getVersion()));
		this.data.add(element);
		if (Version.getInstance().getBuildDate() != null) {
			element = new Element("BuildDate", new SingleValue(Version.getInstance().getBuildDate()));
			this.data.add(element);
		}
		element = new Element("FormatVersion", new SingleValue(Version.getInstance().getServerFormatVersion()));
		this.data.add(element);
	}

	@Override
	void finish() throws PackageException, TypeException {
		Frame frame = this.data;
		Element e = frame.get("ClientId");
		SingleData<?> data = e.getValue().getSingleData();
		if (data == null) {
			throw new PackageException("Illegal format of ConnectedPackage");
		}
		Integer clientId = data.getInt();

		if (clientId == null) {
			throw new PackageException("Illegal format ClientId in ConnectedPackage");
		}

		this.clientId = clientId;
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
