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
import de.sgollmer.solvismax.error.TypeException;
import de.sgollmer.solvismax.log.LogManager;
import de.sgollmer.solvismax.log.LogManager.ILogger;
import de.sgollmer.solvismax.model.objects.data.SolvisData.SmartHomeData;

public class MeasurementsPackage implements ISendData {

	private static final ILogger logger = LogManager.getInstance().getLogger(MeasurementsPackage.class);

	private final Collection<SmartHomeData> datas;

	public MeasurementsPackage(final Collection<SmartHomeData> datas) {
		this.datas = datas;
	}

	@Override
	public JsonPackage createJsonPackage() {
		Frame measurements = new Frame();

		for (SmartHomeData data : this.datas) {
			Element e;
			try {
				e = Measurement.createMeasurement(data);
			} catch (TypeException e1) {
				logger.error("Type exception of " + data.getName() + ", ignored");
				e = null;
			}
			if (e != null) {
				measurements.add(e);
			}
		}

		return new JsonPackage(Command.MEASUREMENTS, measurements);
	}

	@Override
	public Collection<MqttData> createMqttData() {
		Collection<MqttData> mqtt = new ArrayList<>();

		for (SmartHomeData data : this.datas) {
			try {
				mqtt.add(data.getMqttData());
			} catch (TypeException e) {
				logger.error("Type exception of " + data.getName() + ", ignored");
			}
		}

		return mqtt;
	}

}
