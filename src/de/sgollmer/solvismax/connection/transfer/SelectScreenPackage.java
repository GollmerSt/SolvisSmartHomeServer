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

public class SelectScreenPackage extends JsonPackage implements ITransferedData {

	private String screenId;

	SelectScreenPackage() {
		super.command = Command.SELECT_SCREEN;
	}

	public SelectScreenPackage(String screenId) {
		this.command = Command.SELECT_SCREEN;
		this.data = new Frame();
		Element element = new Element();
		this.data.add(element);
		element.name = "Screen";
		element.value = new SingleValue(screenId);
	}

	@Override
	void finish() {
		Frame f = this.data;
		if (f.size() > 0) {
			Element e = f.get(0);
			if (e.getName().equals("Screen")) {
				if (e.value instanceof SingleValue) {
					this.screenId = ((SingleValue) e.getValue()).getData().toString();
				}
			}
			this.data = null;
		}

	}

	@Override
	public SingleData<?> getSingleData() {
		return new StringData(this.screenId, 0);
	}

}