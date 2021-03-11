package de.sgollmer.solvismax.model.objects;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

import de.sgollmer.solvismax.connection.ISendData;
import de.sgollmer.solvismax.connection.transfer.MeasurementsPackage;
import de.sgollmer.solvismax.model.objects.Observer.IObserver;
import de.sgollmer.solvismax.model.objects.data.SolvisData;

/**
 * 
 * @author stefa_000
 *
 *         A collection of measurements, which should be send
 */
public class Measurements {
	private Map<String, SolvisData> measurements = new HashMap<>();
	private final boolean clear;

	public Measurements(Map<String, SolvisData> measurements) {
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
	public synchronized SolvisData add(SolvisData data) {
		if (!data.isDontSend()) {
			return this.measurements.put(data.getId(), data);
		} else {
			return null;
		}
	}

	public void remove(SolvisData data) {
		this.measurements.remove(data.getId());
	}

	public synchronized Collection<SolvisData> cloneAndClear() {
		Collection<SolvisData> collection = new ArrayList<SolvisData>(this.measurements.values());
		if (this.clear) {
			this.measurements.clear();
		}
		return collection;
	}

	public void sent(IObserver<ISendData> observer) {
		MeasurementsPackage sendPackage = new MeasurementsPackage(this.measurements.values());
		observer.update(sendPackage, this);
	}
}
