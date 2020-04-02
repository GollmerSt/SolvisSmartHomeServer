/************************************************************************
 * 
 * $Id$
 *
 * 
 ************************************************************************/

package de.sgollmer.solvismax.connection.transfer;

public class ConnectPackage extends JsonPackage {

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
}
