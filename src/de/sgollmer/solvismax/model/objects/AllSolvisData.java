/************************************************************************
 * 
 * $Id$
 *
 * 
 ************************************************************************/

package de.sgollmer.solvismax.model.objects;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

import de.sgollmer.solvismax.model.Solvis;
import de.sgollmer.solvismax.model.objects.Observer.IObserver;
import de.sgollmer.solvismax.model.objects.backup.Measurement;
import de.sgollmer.solvismax.model.objects.backup.SystemMeasurements;
import de.sgollmer.solvismax.model.objects.data.SingleData;
import de.sgollmer.solvismax.model.objects.data.SolvisData;

public class AllSolvisData {

	private final Solvis solvis;

	private final Map<String, SolvisData> solvisDatas = new HashMap<>();
	private int averageCount;
	private int measurementHysteresisFactor;

	public AllSolvisData(Solvis solvis) {
		this.solvis = solvis;
	}

	public SolvisData get(ChannelDescription description) {
		String id = description.getId();
		SolvisData data = null;
		synchronized (this) {
			data = this.solvisDatas.get(id);
			if (data == null) {
				data = new SolvisData(description, this);
				this.solvisDatas.put(id, data);
			}
		}
		return data;
	}

	public SolvisData get(String id) {
		SolvisData data = null;
		synchronized (this) {
			data = this.solvisDatas.get(id);
			if (data == null) {
				ChannelDescription description = this.solvis.getChannelDescription(id);
				if (description != null) {
					data = new SolvisData(description, this);
					this.solvisDatas.put(id, data);
				}
			}
		}
		return data;
	}

	public SolvisData checkAndGet(String id) {
		ChannelDescription description = this.solvis.getChannelDescription(id);
		if (description == null) {
			throw new UnknownError("Unknown error: <" + id + "> is unknown");
		}
		return this.get(description);
	}

	/**
	 * @return the averageCount
	 */
	public int getAverageCount() {
		return this.averageCount;
	}

	/**
	 * @param averageCount the averageCount to set
	 */
	public void setAverageCount(int averageCount) {
		this.averageCount = averageCount;
	}

	public int getMeasurementHysteresisFactor() {
		return this.measurementHysteresisFactor;
	}

	public void setMeasurementHysteresisFactor(int measurementHysteresisFactor) {
		this.measurementHysteresisFactor = measurementHysteresisFactor;
	}

	/**
	 * @return the solvis
	 */
	public Solvis getSolvis() {
		return this.solvis;
	}

	public synchronized void backupSpecialMeasurements(SystemMeasurements systemMeasurements) {
		systemMeasurements.clear();
		for (SolvisData data : this.solvisDatas.values()) {
			if (data.getDescription().getType() == IChannelSource.Type.CALCULATION) {
				SingleData<?> sd = data.getSingleData();
				if (sd != null) {
					systemMeasurements.add(new Measurement(data.getId(), data.getSingleData()));
				}
			}
		}
	}

	public synchronized void restoreSpecialMeasurements(SystemMeasurements backup) {
		for (Measurement measurement : backup.getMeasurements()) {
			SolvisData data = this.get(measurement.getId());
			if (data != null) {
				data.setSingleData(measurement.getData());
			}
		}
	}

	public synchronized Collection<SolvisData> getMeasurements() {
		return this.solvisDatas.values();
	}

	public void registerObserver(IObserver<SolvisData> observer) {
		for (SolvisData data : this.solvisDatas.values()) {
			data.register(observer);
		}
	}

}
