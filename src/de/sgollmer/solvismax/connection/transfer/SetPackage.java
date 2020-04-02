/************************************************************************
 * 
 * $Id$
 *
 * 
 ************************************************************************/

package de.sgollmer.solvismax.connection.transfer;

import de.sgollmer.solvismax.model.objects.data.SingleData;

public class SetPackage extends JsonPackage {
	public SetPackage() {
		this.command = Command.SET;
	}

	private String id = null;
	private SingleData<?> singleData = null;

	@Override
	public void finish() {
		Frame f = this.data;
		if (f.size() > 0) {
			Element e = f.get(0);
			this.id = e.name;
			if (e.value instanceof SingleValue) {
				this.singleData = ((SingleValue) e.value).getData();
			}
		}
		this.data = null;
	}

	public String getId() {
		return this.id;
	}

	public SingleData<?> getSingleData() {
		return this.singleData;
	}

}
