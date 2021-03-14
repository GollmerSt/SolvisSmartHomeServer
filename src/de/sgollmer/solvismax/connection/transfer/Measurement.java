/************************************************************************
 * 
 * $Id$
 *
 * 
 ************************************************************************/

package de.sgollmer.solvismax.connection.transfer;

import de.sgollmer.solvismax.model.objects.data.SolvisData.SmartHomeData;

public class Measurement extends Element {

	private Measurement(SmartHomeData data) {
		this.name = data.getDescription().getId();
		this.value = data.toSingleValue(data.getData());
	}

	static Measurement createMeasurement(SmartHomeData data) {
		synchronized (data) {
			if (data.getData() != null) {
				return new Measurement(data);
			} else {
				return null;
			}
		}
	}
}
