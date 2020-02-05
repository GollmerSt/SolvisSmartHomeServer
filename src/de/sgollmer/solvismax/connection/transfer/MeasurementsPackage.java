/************************************************************************
 * 
 * $Id$
 *
 * 
 ************************************************************************/

package de.sgollmer.solvismax.connection.transfer;

import java.util.Collection;

import de.sgollmer.solvismax.model.objects.data.SolvisData;

public class MeasurementsPackage extends JsonPackage {

	public MeasurementsPackage(Collection<SolvisData> datas) {
		this.command = Command.MEASUREMENTS;

		Frame measurements = new Frame();
		this.data = measurements;

		for (SolvisData data : datas) {
			if (data.getSentData() != null) {
				
				Element e = new Measurement(data);
				measurements.add(e);
			}
		}

	}

}
