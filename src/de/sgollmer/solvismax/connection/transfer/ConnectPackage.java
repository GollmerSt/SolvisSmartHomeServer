/************************************************************************
 * 
 * $Id$
 *
 * 
 ************************************************************************/

package de.sgollmer.solvismax.connection.transfer;

import de.sgollmer.solvismax.connection.IReceivedData;
import de.sgollmer.solvismax.error.PackageException;
import de.sgollmer.solvismax.model.objects.data.SingleData;
import de.sgollmer.solvismax.model.objects.data.StringData;

public class ConnectPackage extends JsonPackage implements IReceivedData {

	private String id = null;

	ConnectPackage() {
		this.command = Command.CONNECT;
	}

	public ConnectPackage(final String id) {
		this.id = id;
		this.command = Command.CONNECT;
		this.data = new Frame();
		Element element = new Element("Id", id == null ? null : new SingleValue(id));
		this.data.add(element);
	}

	@Override
	void finish() throws PackageException {
		Frame frame = this.data;
		Element e = frame.get("Id");
		SingleData<?> data = e.getValue().getSingleData();
		if (data == null) {
			this.id = null;
		} else {
			this.id = data.toString();
		}
		this.data = null;
	}

	private String getId() {
		return this.id;
	}

	@Override
	public SingleData<?> getSingleData() {
		return new StringData(this.getId(), -1L);
	}
}
