package de.sgollmer.solvismax.model.objects;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

import de.sgollmer.solvismax.connection.ISendData;
import de.sgollmer.solvismax.connection.transfer.MeasurementsPackage;
import de.sgollmer.solvismax.model.objects.Observer.IObserver;
import de.sgollmer.solvismax.model.objects.data.SolvisData.SmartHomeData;

/**
 * 
 * @author stefa_000
 *
 *         A collection of measurements, which should be send
 */
public class Measurements {
	private Map<String, SmartHomeData> measurements = new HashMap<>();
	private final boolean clear;

	public Measurements(final Map<String, SmartHomeData> measurements) {
		this.clear = false;
		this.measurements = measurements;
	}

	public Measurements() {
		this.clear = true;
		this.measurements = new HashMap<>();
	}

	/**
	 * 
	 * @param data to add
	 * @return replaced value
	 */
	public synchronized SmartHomeData add(final SmartHomeData data) {
		return this.measurements.put(data.getDescription().getId(), data);
	}

	public void remove(final SmartHomeData data) {
		this.measurements.remove(data.getDescription().getId());
	}

	public synchronized Collection<SmartHomeData> cloneAndClear() {
		Collection<SmartHomeData> collection = new ArrayList<>(this.measurements.values());
		if (this.clear) {
			this.measurements.clear();
		}
		return collection;
	}

	public void sent(final IObserver<ISendData> observer) {
		MeasurementsPackage sendPackage = new MeasurementsPackage(this.measurements.values());
		observer.update(sendPackage, this);
	}
}
