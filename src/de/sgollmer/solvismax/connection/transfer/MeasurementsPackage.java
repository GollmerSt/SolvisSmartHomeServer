/************************************************************************
 * 
 * $Id$
 *
 * 
 ************************************************************************/

package de.sgollmer.solvismax.connection.transfer;

import java.util.Collection;

import de.sgollmer.solvismax.connection.ISendData;
import de.sgollmer.solvismax.model.objects.data.SolvisData;

public class MeasurementsPackage extends JsonPackage implements ISendData {

	public MeasurementsPackage(Collection<SolvisData> datas) {
		this.command = Command.MEASUREMENTS;

		Frame measurements = new Frame();
		this.data = measurements;

		for (SolvisData data : datas) {
			Element e = Measurement.createMeasurement(data);
			if (e != null) {
				measurements.add(e);
			}
		}
	}

	@Override
	public JsonPackage createJsonPackage() {
		return this;
	}

}
