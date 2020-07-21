/************************************************************************
 * 
 * $Id$
 *
 * 
 ************************************************************************/

package de.sgollmer.solvismax.connection.transfer;

import de.sgollmer.solvismax.model.objects.data.SolvisData;

public class Measurement extends Element {

	private Measurement(SolvisData data) {
		this.name = data.getId();
		this.value = data.toSingleValue(data.getSentData());
	}

	static Measurement createMeasurement(SolvisData data) {
		synchronized (data) {
			if (data.getSentData() != null) {
				return new Measurement(data);
			} else {
				return null;
			}
		}
	}
}
