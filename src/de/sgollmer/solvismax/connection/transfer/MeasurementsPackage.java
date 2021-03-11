/************************************************************************
 * 
 * $Id$
 *
 * 
 ************************************************************************/

package de.sgollmer.solvismax.connection.transfer;

import java.util.ArrayList;
import java.util.Collection;

import de.sgollmer.solvismax.connection.ISendData;
import de.sgollmer.solvismax.connection.mqtt.MqttData;
import de.sgollmer.solvismax.model.objects.data.SolvisData;

public class MeasurementsPackage implements ISendData {

	private final Collection<SolvisData> datas;

	public MeasurementsPackage(Collection<SolvisData> datas) {
		this.datas = datas;
	}

	@Override
	public JsonPackage createJsonPackage() {
		Frame measurements = new Frame();

		for (SolvisData data : this.datas) {
			Element e = Measurement.createMeasurement(data);
			if (e != null) {
				measurements.add(e);
			}
		}

		return new JsonPackage(Command.MEASUREMENTS, measurements);
	}

	@Override
	public Collection<MqttData> createMqttData() {
		Collection<MqttData > mqtt = new ArrayList<>();
		
		for (SolvisData data : this.datas) {
			mqtt.add(data.getMqttData());
		}
		
		return mqtt;
	}

}
