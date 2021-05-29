/************************************************************************
 * 
 * $Id$
 *
 * 
 ************************************************************************/

package de.sgollmer.solvismax.connection.transfer;

import de.sgollmer.solvismax.model.objects.data.SolvisData.SmartHomeData;

public class Measurement extends Element {

	private Measurement(final SmartHomeData data) {
		super(data.getName(), data.toSingleValue(data.getData()));
	}

	static Measurement createMeasurement(final SmartHomeData data) {
		synchronized (data) {
			if (data.getData() != null) {
				return new Measurement(data);
			} else {
				return null;
			}
		}
	}
}
