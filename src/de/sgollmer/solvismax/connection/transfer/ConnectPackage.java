/************************************************************************
 * 
 * $Id$
 *
 * 
 ************************************************************************/

package de.sgollmer.solvismax.connection.transfer;

import de.sgollmer.solvismax.connection.ITransferedData;
import de.sgollmer.solvismax.model.objects.data.SingleData;
import de.sgollmer.solvismax.model.objects.data.StringData;

public class ConnectPackage extends JsonPackage implements ITransferedData {

	private String id = null;

	public ConnectPackage() {
		this.command = Command.CONNECT;
	}

	public ConnectPackage(String id) {
		this.id = id;
		this.command = Command.CONNECT;
		this.data = new Frame();
		Element element = new Element();
		this.data.add(element);
		element.name = "Id";
		if (id == null) {
			element.value = null;
		} else {
			element.value = new SingleValue(id);
		}
	}

	@Override
	public void finish() {
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

	public String getId() {
		return this.id;
	}

	@Override
	public SingleData<?> getSingleData() {
		return new StringData(this.getId(), 0);
	}
}
