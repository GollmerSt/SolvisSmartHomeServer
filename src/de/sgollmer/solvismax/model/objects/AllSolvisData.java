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
import de.sgollmer.solvismax.error.TypeException;
import de.sgollmer.solvismax.log.LogManager;
import de.sgollmer.solvismax.log.LogManager.ILogger;
import de.sgollmer.solvismax.model.Solvis;
import de.sgollmer.solvismax.model.WatchDog.HumanAccess;
import de.sgollmer.solvismax.model.objects.Observer.IObserver;
import de.sgollmer.solvismax.model.objects.Observer.Observable;
import de.sgollmer.solvismax.model.objects.backup.Measurement;
import de.sgollmer.solvismax.model.objects.backup.SystemBackup;
import de.sgollmer.solvismax.model.objects.data.IMode;
import de.sgollmer.solvismax.model.objects.data.ModeValue;
import de.sgollmer.solvismax.model.objects.data.SingleData;
import de.sgollmer.solvismax.model.objects.data.SolvisData;
import de.sgollmer.solvismax.model.objects.data.SolvisData.SmartHomeData;

public class AllSolvisData extends Observable<SmartHomeData> {

	private static final ILogger logger = LogManager.getInstance().getLogger(AllSolvisData.class);

	private final Solvis solvis;

	private final Map<String, SolvisData> solvisDatas = new HashMap<>();
	private final Map<String, SolvisData> solvisDatasByAlias = new HashMap<>();
	private final Map<String, SolvisData> solvisDatasByName = new HashMap<>();
	private int averageCount;
	private int measurementHysteresisFactor;
	private long lastHumanAcess = System.currentTimeMillis();
	private HumanAccess humanAccess = HumanAccess.NONE;

	public AllSolvisData(final Solvis solvis) {
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

	public SolvisData get(final ChannelDescription description) {
		String id = description.getId();
		return this.get(id);
	}

	public SolvisData get(final String id) {
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

	public void add(final SolvisData data) {
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
		String name = data.getName();
		if (this.solvisDatasByName.put(name, data) != null) {
			throw new UnknownError("Unknown error: Name <" + name + "> is not unique!!");
		}
	}

	public SolvisData checkAndGet(final String id) {
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
	public void setAverageCount(final int averageCount) {
		this.averageCount = averageCount;
	}

	public int getMeasurementHysteresisFactor() {
		return this.measurementHysteresisFactor;
	}

	public void setMeasurementHysteresisFactor(final int measurementHysteresisFactor) {
		this.measurementHysteresisFactor = measurementHysteresisFactor;
	}

	/**
	 * @return the solvis
	 */
	public Solvis getSolvis() {
		return this.solvis;
	}

	public synchronized void saveToBackup(final SystemBackup systemMeasurements) {
		systemMeasurements.clear();
		for (SolvisData data : this.solvisDatas.values()) {
			if (data.getDescription().mustBackuped() && !data.isFix()) {
				SingleData<?> sd = data.getSingleData();
				if (sd != null) {
					systemMeasurements.add(new Measurement(data.getId(), data.getSingleData()));
				}
			}
		}
	}

	public synchronized void restoreFromBackup(final SystemBackup backup) {
		for (SystemBackup.IValue value : backup.getValues()) {
			if (value instanceof Measurement) {
				SolvisData data = this.get(value.getId());
				if (data != null) {

					Measurement measurement = (Measurement) value;

					SingleData<?> singleData = measurement.getData();

					if (singleData instanceof ModeValue) {
						String name = ((ModeValue<?>) singleData).get().getName();
						IMode<?> mode = data.getDescription().getMode(name);
						if (mode == null) {
							logger.error("Mode name <" + name + "> not defined. Setting by backup ignored.");
							singleData = null;
						} else {
							singleData = mode.create(backup.getTimeOfLastBackup());
						}
					}

					if (singleData != null) {
						SingleData<?> single = singleData.clone(backup.getTimeOfLastBackup());
						try {
							data.setSingleData(single, backup);
						} catch (TypeException e) {
							logger.error("Type exception of " + data.getName() + ", ignored");
						}
					}
				}
			}
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
			if (smartHomeData != null && !data.isDelayed()) {
				measurements.add(smartHomeData);
			}
		}
		return measurements;
	}

	public void notifySmartHome(final SmartHomeData solvisData, final Object source) {
		this.notify(solvisData, source);

	}

	public void sendMetaToMqtt(final Mqtt mqtt, final boolean deleteRetained) throws MqttException, MqttConnectionLost {
		for (SolvisData data : this.solvisDatas.values()) {
			MqttData mqttData = data.getChannelInstance().getMqttMeta();
			if (deleteRetained) {
				mqtt.unpublish(mqttData);
			} else {
				mqtt.publish(mqttData);
			}
		}

	}

	public SolvisData getByName(final String name) {
		return this.solvisDatasByName.get(name);
	}

	public void debugClear() throws TypeException {
		for (SolvisData data : this.solvisDatas.values()) {
			data.setSingleDataDebug(null);
		}
	}

}
