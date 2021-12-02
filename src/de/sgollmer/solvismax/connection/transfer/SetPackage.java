/************************************************************************
 * 
 * $Id$
 *
 * 
 ************************************************************************/

package de.sgollmer.solvismax.connection.transfer;

import de.sgollmer.solvismax.connection.IReceivedData;
import de.sgollmer.solvismax.log.LogManager;
import de.sgollmer.solvismax.log.LogManager.ILogger;
import de.sgollmer.solvismax.model.objects.data.SingleData;

public class SetPackage extends JsonPackage implements IReceivedData {

	private static final ILogger logger = LogManager.getInstance().getLogger(SetPackage.class);

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
			} else {
				logger.warn("Data parameter can't be interpreted. Package: " + this.getReceivedString());
			}
		} else {
			logger.warn("Data parameter is empty. Package: " + this.getReceivedString());
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
