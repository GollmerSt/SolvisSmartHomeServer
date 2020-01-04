/************************************************************************
 * 
 * $Id$
 *
 * 
 ************************************************************************/

package de.sgollmer.solvismax.connection.transfer;

public class ConnectPackage extends JsonPackage {

	public ConnectPackage() {
		this.command = Command.CONNECT;
	}

	private String id = null;

	@Override
	public void finish() {
		Frame frame = this.data;
		for (Element e : frame.elements) {
			String id = e.name;
			if (e.value instanceof SingleValue) {
				SingleValue sv = (SingleValue) e.value;
				String value = sv.getData().toString();
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
		return id;
	}
}
