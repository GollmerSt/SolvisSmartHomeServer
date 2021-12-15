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

public class SetPackage extends JsonPackage implements IReceivedData {

	SetPackage() {
		this.command = Command.SET;
	}

	private String id = null;
	private SingleData<?> singleData = null;

	@Override
	void finish() throws PackageException {
		Frame f = this.data;
		if (f.size() == 1) {
			Element e = f.get(0);
			this.id = e.name;
			IValue value = e.getValue();
			if (value == null) {
				throw new PackageException("Set value is empty.");
			}
			this.singleData = value.getSingleData();
			if (this.singleData == null || this.singleData.get() == null) {
				throw new PackageException("Set value is <null>.");
			}
		} else if (f.size() == 0) {
			throw new PackageException("Data parameter is empty.");
		} else {
			throw new PackageException("More than one data parameter is given.");
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
