package de.sgollmer.solvismax.model.objects;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

import de.sgollmer.solvismax.model.objects.data.SolvisData;

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

	public synchronized SolvisData add(SolvisData data) {
		return this.measurements.put(data.getId(), data);
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
}
