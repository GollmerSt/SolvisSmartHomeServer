/************************************************************************
 * 
 * $Id$
 *
 * 
 ************************************************************************/

package de.sgollmer.solvismax.connection.transfer;

import de.sgollmer.solvismax.connection.ITransferedData;
import de.sgollmer.solvismax.model.objects.data.SingleData;

public class SetPackage extends JsonPackage implements ITransferedData {
	SetPackage() {
		this.command = Command.SET;
	}

	private String id = null;
	private SingleData<?> singleData = null;

	@Override
	void finish() {
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

	@Override
	public SingleData<?> getSingleData() {
		return this.singleData;
	}

	@Override
	public String getChannelId() {
		return this.id;
	}

}
