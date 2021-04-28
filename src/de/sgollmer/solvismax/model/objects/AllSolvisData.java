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

import org.eclipse.paho.client.mqttv3.MqttException;

import de.sgollmer.solvismax.connection.mqtt.Mqtt;
import de.sgollmer.solvismax.connection.mqtt.MqttData;
import de.sgollmer.solvismax.error.MqttConnectionLost;
import de.sgollmer.solvismax.model.Solvis;
import de.sgollmer.solvismax.model.WatchDog.HumanAccess;
import de.sgollmer.solvismax.model.objects.Observer.IObserver;
import de.sgollmer.solvismax.model.objects.Observer.Observable;
import de.sgollmer.solvismax.model.objects.backup.Measurement;
import de.sgollmer.solvismax.model.objects.backup.SystemBackup;
import de.sgollmer.solvismax.model.objects.data.SingleData;
import de.sgollmer.solvismax.model.objects.data.SolvisData;
import de.sgollmer.solvismax.model.objects.data.SolvisData.SmartHomeData;
import de.sgollmer.solvismax.model.update.Correction;

public class AllSolvisData extends Observable<SmartHomeData> {

	private final Solvis solvis;

	private final Map<String, SolvisData> solvisDatas = new HashMap<>();
	private final Map<String, Correction> corrections = new HashMap<>();
	private final Map<String, SolvisData> solvisDatasByAlias = new HashMap<>();
	private final Map<String, SolvisData> solvisDatasByName = new HashMap<>();
	private int averageCount;
	private int measurementHysteresisFactor;
	private long lastHumanAcess = System.currentTimeMillis();
	private HumanAccess humanAccess = HumanAccess.NONE;

	public AllSolvisData(Solvis solvis) {
		this.solvis = solvis;

		solvis.registerScreenChangedByHumanObserver(new IObserver<HumanAccess>() {

			@Override
			public void update(HumanAccess data, Object source) {
				if (data == HumanAccess.NONE && AllSolvisData.this.humanAccess != HumanAccess.NONE) {
					AllSolvisData.this.lastHumanAcess = System.currentTimeMillis();
				}
				AllSolvisData.this.humanAccess = data;
			}

		});
	}

	public long getLastHumanAccess() {
		return this.lastHumanAcess;
	}

	public SolvisData get(ChannelDescription description) {
		String id = description.getId();
		return this.get(id);
	}

	public SolvisData get(String id) {
		SolvisData data = null;
		synchronized (this) {
			data = this.solvisDatas.get(id);
			if (data == null) {
				data = this.solvisDatasByAlias.get(id);

// TODO
//				ChannelDescription description = this.solvis.getChannelDescription(id);
//				if (description != null) {
//					boolean ignore = this.solvis.getUnit().isChannelIgnored(id);
//					data = new SolvisData(description, this, ignore);
//					data.setAsSmartHomedata();
//					this.solvisDatas.put(id, data);
//				}
			}
		}
		return data;
	}

	public void add(SolvisData data) {
		String id = data.getDescription().getId();
		if (this.solvisDatas.put(id, data) != null) {
			throw new UnknownError("Unknown error: <" + id + "> is not unique!!");
		}
		String alias = data.getChannelInstance().getAlias();
		if (alias != null) {
			if (this.solvisDatasByAlias.put(alias, data) != null) {
				throw new UnknownError("Unknown error: Alias <" + alias + "> is not unique!!");
			}
		}
		String name = data.getChannelInstance().getName();
		if (this.solvisDatasByName.put(name, data) != null) {
			throw new UnknownError("Unknown error: Name <" + name + "> is not unique!!");
		}
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

	public synchronized void saveToBackup(SystemBackup systemMeasurements) {
		systemMeasurements.clear();
		for (SolvisData data : this.solvisDatas.values()) {
			if (data.getDescription().mustBackuped()) {
				SingleData<?> sd = data.getSingleData();
				if (sd != null) {
					systemMeasurements.add(new Measurement(data.getId(), data.getSingleData()));
				}
			}
		}
		for (Map.Entry<String, Correction> entry : this.corrections.entrySet()) {
			systemMeasurements.add(entry.getValue());
		}
	}

	public synchronized void restoreFromBackup(SystemBackup backup) {
		for (SystemBackup.IValue value : backup.getValues()) {
			if (value instanceof Measurement) {
				SolvisData data = this.get(value.getId());
				if (data != null) {

					SingleData<?> single = ((Measurement) value).getData().create(backup.getTimeOfLastBackup());
					data.setSingleData(single, backup);
				}
			} else if (value instanceof Correction) {
				String id = value.getId();
				Correction correction = this.getCorrection(id);
				correction.set((Correction) value);
			}
		}
	}

	public void setDefaultCorrection() {
		for (Correction correction : this.corrections.values()) {
			this.solvis.getUnit().setDefaultCorrection(correction);
		}
	}

	public synchronized Measurements getMeasurements() {
		Measurements measurements = new Measurements();
		for (SolvisData data : this.solvisDatas.values()) {
			SmartHomeData smartHomeData = data.getSmartHomeData();
			if (smartHomeData != null) {
				measurements.add(smartHomeData);
			}
		}
		return measurements;
	}

	public synchronized Collection<SolvisData> getSolvisDatas() {
		return this.solvisDatas.values();
	}

	public synchronized Measurements getMeasurementsForUpdate() {
		Measurements measurements = new Measurements();
		for (SolvisData data : this.solvisDatas.values()) {
			SmartHomeData smartHomeData = data.getSmartHomeData();
			if (smartHomeData != null && !smartHomeData.getDescription().isDelayed(this.solvis)) {
				measurements.add(smartHomeData);
			}
		}
		return measurements;
	}

	public void notifySmartHome(SmartHomeData solvisData, Object source) {
		this.notify(solvisData, source);

	}

	public Correction getCorrection(String id) {
		Correction correction = this.corrections.get(id);
		if (correction == null) {
			correction = new Correction(id);
			this.corrections.put(id, correction);
		}
		return correction;
	}

	public void sendMetaToMqtt(Mqtt mqtt, boolean deleteRetained) throws MqttException, MqttConnectionLost {
		for (SolvisData data : this.solvisDatas.values()) {
			MqttData mqttData = data.getChannelInstance().getMqttMeta();
			if (deleteRetained) {
				mqtt.unpublish(mqttData);
			} else {
				mqtt.publish(mqttData);
			}
		}

	}

	public SolvisData getByName(String name) {
		return this.solvisDatasByName.get(name);
	}

}
