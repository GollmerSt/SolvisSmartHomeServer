/************************************************************************
 * 
 * $Id$
 *
 * 
 ************************************************************************/

package de.sgollmer.solvismax.connection.transfer;

import de.sgollmer.solvismax.connection.IReceivedData;
import de.sgollmer.solvismax.model.objects.data.SingleData;
import de.sgollmer.solvismax.model.objects.data.StringData;

public class ConnectPackage extends JsonPackage implements IReceivedData {

	private String id = null;

	ConnectPackage() {
		this.command = Command.CONNECT;
	}

	public ConnectPackage(String id) {
		this.id = id;
		this.command = Command.CONNECT;
		this.data = new Frame();
		Element element = new Element("Id", id == null ? null : new SingleValue(id));
		this.data.add(element);
	}

	@Override
	void finish() {
		Frame frame = this.data;
		for (Element e : frame.elements) {
			String id = e.name;
			if (e.value instanceof SingleValue) {
				SingleValue sv = (SingleValue) e.value;
				String value;
				if (sv.getData() == null) {
					value = null;
				} else {
					value = sv.getData().toString();
				}
				switch (id) {
					case "Id":
						this.id = value;
						break;
				}
			}
		}
		this.data = null;
	}

	private String getId() {
		return this.id;
	}

	@Override
	public SingleData<?> getSingleData() {
		return new StringData(this.getId(), 0);
	}
}
