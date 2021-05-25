/************************************************************************
 * 
 * $Id$
 *
 * 
 ************************************************************************/

package de.sgollmer.solvismax.model.objects.data;

import java.util.Calendar;

import de.sgollmer.solvismax.Constants.Csv;
import de.sgollmer.solvismax.connection.mqtt.Mqtt;
import de.sgollmer.solvismax.connection.mqtt.MqttData;
import de.sgollmer.solvismax.connection.transfer.SingleValue;
import de.sgollmer.solvismax.connection.transfer.SolvisStatePackage;
import de.sgollmer.solvismax.error.TypeException;
import de.sgollmer.solvismax.helper.Helper;
import de.sgollmer.solvismax.log.LogManager;
import de.sgollmer.solvismax.log.LogManager.ILogger;
import de.sgollmer.solvismax.model.Solvis;
import de.sgollmer.solvismax.model.SolvisStatus;
import de.sgollmer.solvismax.model.objects.AllSolvisData;
import de.sgollmer.solvismax.model.objects.ChannelDescription;
import de.sgollmer.solvismax.model.objects.ChannelInstance;
import de.sgollmer.solvismax.model.objects.IChannelSource.SetResult;
import de.sgollmer.solvismax.model.objects.Observer;
import de.sgollmer.solvismax.model.objects.Observer.IObserver;
import de.sgollmer.solvismax.model.objects.Observer.Observable;
import de.sgollmer.solvismax.model.objects.ResultStatus;

public class SolvisData extends Observer.Observable<SolvisData> implements IObserver<SolvisStatePackage> {

	private static final ILogger logger = LogManager.getInstance().getLogger(SolvisData.class);

	private ChannelInstance channelInstance;
	private final AllSolvisData datas;

	private final Average average;
	private SingleData<?> data = null;
	private SingleData<?> pendingData = null;
	private SingleData<?> sentData = null;
	private final boolean dontSend;
	private SmartHomeData smartHomeData;
	private int executionTime;

	private Observer.Observable<SolvisData> continousObservable = null;

	public SolvisData(ChannelDescription description, AllSolvisData datas, boolean ignore) {
		this.datas = datas;
		this.dontSend = ignore;
		this.smartHomeData = null;
		if (description.isAverage()) {
			this.average = new Average(this.datas.getAverageCount(), datas.getMeasurementHysteresisFactor());
		} else {
			this.average = null;
		}
		this.channelInstance = ChannelInstance.create(description, this.getSolvis());
	}

	public void setAsSmartHomedata() {
		this.smartHomeData = new SmartHomeData(this);
	}

	private SolvisData(SolvisData data) {
		this.channelInstance = data.channelInstance;
		this.datas = data.datas;
		if (data.average != null) {
			this.average = new Average(data.average);
		} else {
			this.average = null;
		}
		this.sentData = data.sentData;
		this.dontSend = data.dontSend;
		this.smartHomeData = null;
	}

	public SolvisData(SingleData<?> data) {
		this.data = data;
		this.channelInstance = null;
		this.average = null;
		this.datas = null;
		this.dontSend = false;
		this.smartHomeData = null;
	}

	/**
	 * This method duplicates the SolvisData without the observers
	 * 
	 * @return duplicate
	 */
	public SolvisData duplicate() {
		return new SolvisData(this);
	}

	@Override
	public boolean equals(Object obj) {
		if (obj instanceof SolvisData) {
			return ((SolvisData) obj).data.equals(this.data);
		} else {
			return false;
		}
	}

	@Override
	public int hashCode() {
		if (this.data == null) {
			return 103;
		} else {
			return this.data.hashCode();
		}
	}

	public String getId() {
		return this.getDescription().getId();
	}

	private void setData(SingleData<?> data) {
		this.setData(data, this, ResultStatus.SUCCESS, -1L);
	}

	private synchronized void setData(SingleData<?> data, Object source, ResultStatus status, long executionStartTime) {

		if (data == null || data.get() == null) {
			this.data = null;
			return;
		}

		if (data instanceof TwoValues) {

			TwoValues twoValues = (TwoValues) data;

			this.setData(twoValues.getFirst(), source, status, executionStartTime);
			this.setData(twoValues.getSecond(), source, status, executionStartTime);
			return;
		}

		boolean fastChange = false;
		if (executionStartTime > 0L && data.getTimeStamp() > 0L) {
			this.executionTime = (int) (data.getTimeStamp() - executionStartTime);
		}

		if (this.getDescription().isAverage()) {
			this.average.add(data);
			Average.Result average = this.average.getAverage(data);
			if (average == null) {
				return;
			}
			data = average.getData();
			fastChange = average.isFastChange();
		}

		if (data == null) {
			return;
		}

		boolean glitchInhibitEnabled = this.getDescription().glitchInhibitScanIntervals() > 0;
		boolean changed = false;

		if (data.getTimeStamp() > 0 && this.pendingData != null) {

			if (data.equals(this.pendingData)) {

				if (data.getTimeStamp() > this.pendingData.getTimeStamp()
						+ this.getDescription().glitchInhibitScanIntervals()
								* this.getDescription().getScanInterval_ms(this.getSolvis())) {

					glitchInhibitEnabled = false;
					this.pendingData = null;
					changed = true;
				}
			} else {

				this.pendingData = data;
			}
		} else if (!data.equals(this.data)) {

			if (glitchInhibitEnabled && data.getTimeStamp() > 0) {

				this.pendingData = data;
			} else {

				changed = true;
			}
		}

		if (!glitchInhibitEnabled) {
			this.data = data;
		}

		if (!this.isEmpty() && (changed || status == ResultStatus.VALUE_VIOLATION)) {

			if (!this.getDescription().isWriteable() || !this.datas.getSolvis().willBeModified(this)
					|| status == ResultStatus.VALUE_VIOLATION) {

				this.notify(this, source);
			}
			logger.debug("Channel: " + this.getId() + ", value: " + data.toString());
		}

		if (this.smartHomeData != null) {
			this.smartHomeData.notify(changed, fastChange, source);
		}
		if (this.continousObservable != null) {

			this.continousObservable.notify(this, source);
		}
	}

	/**
	 * @return the description
	 */
	public ChannelDescription getDescription() {
		return this.channelInstance.getDescription();
	}

	public void setInteger(Integer integer, long timeStamp, Object source) {
		this.setData(new IntegerValue(integer, timeStamp), source, ResultStatus.SUCCESS, -1L);
	}

	public void setInteger(Integer integer, long timeStamp) {
		this.setData(new IntegerValue(integer, timeStamp));
	}

	public Integer getInteger() throws TypeException {
		SingleData<?> data = this.data;
		if (data == null) {
			return null;
		} else if (data instanceof DoubleValue) {
			double f = ((DoubleValue) data).get();
			return (int) Math.round(f);

		} else if ((data instanceof IntegerValue)) {
			return ((IntegerValue) data).get();
		}
		throw new TypeException("TypeException: Type actual: <" + data.getClass() + ">, target: <IntegerValue>");
	}

// TODO
//	public Double getDouble() throws TypeException {
//		SingleData<?> data = this.data;
//		if (data == null) {
//			return null;
//		} else if (data instanceof DoubleValue) {
//			return ((DoubleValue) data).get();
//
//		} else if ((data instanceof IntegerValue)) {
//			Integer i = ((IntegerValue) data).get();
//			if (i == null) {
//				return null;
//			} else
//				return (double) i;
//		} else {
//			throw new TypeException("TypeException: Type actual: <" + data.getClass() + ">, target: <IntegerValue>");
//		}
//	}
//
	public int getInt() throws TypeException {
		Integer value = this.getInteger();
		if (value == null) {
			value = 0;
		}
		return value;
	}

	public void setDate(Calendar calendar, long timeStamp) {
		this.setData(new DateValue(calendar, timeStamp));

	}

	public void setBoolean(Boolean bool, long timeStamp) {
		this.setData(new BooleanValue(bool, timeStamp));

	}

	public Helper.Boolean getBoolean() throws TypeException {
		SingleData<?> data = this.data;
		if (data == null) {
			return Helper.Boolean.UNDEFINED;
		}
		Helper.Boolean bool = this.data.getBoolean();
		if (bool == Helper.Boolean.UNDEFINED) {
			throw new TypeException("TypeException: Type actual: <" + data.getClass() + ">, target: <BooleanValue>");
		}
		return bool;
	}

	public boolean getBool() throws TypeException {
		Helper.Boolean bool = this.getBoolean();
		return bool.result();
	}

	public void setMode(IMode<?> mode, long timeStamp) {
		this.setData(mode.create(timeStamp));
	}

	public ModeValue<?> getMode() {
		SingleData<?> data = this.data;
		if (data instanceof ModeValue<?>) {
			return (ModeValue<?>) data;
		} else {
			return null;
		}
	}

	public void setSingleData(SetResult data) {
		this.setData(data.getData(), this, data.getStatus(), -1L);
	}

	public void setSingleData(SingleData<?> data, long executionStartTime) {
		this.setData(data, this, ResultStatus.SUCCESS, executionStartTime);
	}

	public void setSingleData(SingleData<?> data, Object source) {
		this.setData(data, source, ResultStatus.SUCCESS, -1L);
	}

	public void setSingleData(SingleData<?> data) {
		this.setSingleData(data, this);
	}

	public void registerContinuousObserver(Observer.IObserver<SolvisData> observer) {
		if (this.continousObservable == null) {
			this.continousObservable = new Observable<>();
		}
		this.continousObservable.register(observer);
	}

	public void unregisterContinuousObserver(Observer.IObserver<SolvisData> observer) {
		if (this.continousObservable != null) {
			this.continousObservable.unregister(observer);
		}		
	}
	
	public Observer.IObserver<SolvisData> getContinuousObserver(Observer.IObserver<SolvisData> observer) {
		if (this.continousObservable == null) {
			return null;
		}
		return this.continousObservable.getObserver(observer);
	}

	@Override
	public String toString() {
		return this.data.toString();
	}

	public SingleData<?> getSingleData() {
		return this.data;
	}

	public SingleData<?> normalize() {
		return this.getDescription().normalize(this.data);
	}

	@Override
	public void update(SolvisStatePackage data, Object source) {
		SolvisStatus state = data.getState();
		if (state == SolvisStatus.POWER_OFF && this.average != null) {
			this.average.clear();
		}
	}

	/**
	 * Get the time stamp of the last measurement
	 * 
	 * @return
	 */
	public long getTimeStamp() {
		SingleData<?> data = this.data;
		if (data != null) {
			return data.getTimeStamp();
		} else {
			return -1;
		}
	}

	public synchronized SingleData<?> getSentData() {
		if (this.sentData != null) {
			return this.sentData;
		} else {
			return this.data;
		}
	}

	public boolean isFastChange() {
		return this.data.isFastChange();
	}

	public Solvis getSolvis() {
		return this.datas.getSolvis();
	}

//	public MqttData getMqttData() {
//		if (this.data == null || this.dontSend) {
//			return null;
//		}
//		String value = this.normalize().toString();
//		return new MqttData(this.getSolvis(), Mqtt.formatChannelOutTopic(this.getId()), value, 0, true);
//	}
//
	public boolean isValid() {
		if (this.data == null) {
			return false;
		} else if (this.getDescription().isWriteable()) {
			return this.datas.getLastHumanAccess() < this.data.getTimeStamp();
		} else {
			return true;
		}

	}

	public Calendar getDate() {
		if (!(this.data instanceof DateValue)) {
			return null;
		}
		return ((DateValue) this.data).get();
	}

	public boolean isDontSend() {
		return this.dontSend;
	}

	public static class SmartHomeData {
		private final Solvis solvis;
		private final SolvisData solvisData;
		private SingleData<?> current = null;
		private SingleData<?> lastTransmittedData = null;
		private long transmittedTimeStamp = 0;
		private int forceCnt = 0;

		public SmartHomeData(SolvisData solvisData) {
			this.solvis = solvisData.datas.getSolvis();
			this.solvisData = solvisData;
		}

		private void notify(boolean changed, boolean fastChange, Object source) {

			if (this.solvisData.dontSend) {
				return;
			}

			long currentTimeStamp = 0;

			synchronized (this.solvisData) {

				this.current = this.solvisData.data;
				currentTimeStamp = this.solvisData.getTimeStamp();
			}

			if (this.forceCnt > 0) {
				--this.forceCnt;
			}

			if (changed && fastChange && this.forceCnt == 0) {

				boolean buffered = this.solvisData.getDescription().isBuffered() && this.solvis.getUnit().isBuffered();

				long intervall = buffered ? this.solvis.getUnit().getBufferedInterval_ms()
						: this.solvis.getUnit().getMeasurementsInterval_ms();

				long forceUpdateAfterFastChangingIntervals = this.solvis.getUnit()
						.getForceUpdateAfterFastChangingIntervals();

				if (forceUpdateAfterFastChangingIntervals != 0 && currentTimeStamp
						- this.transmittedTimeStamp > intervall * forceUpdateAfterFastChangingIntervals) {
					this.forceCnt = 2;
					logger.debug("Quick change after a long constant period of channel <"
							+ this.solvisData.getDescription() + "> detected.");
				}
			} else if (this.forceCnt > 0) {
				changed = true;
			}

			if (changed) {
				this.solvis.getAllSolvisData().notifySmartHome(this, source);
			}
		}

		public Solvis getSolvis() {
			return this.solvis;
		}

		public boolean isForce() {
			return this.forceCnt > 0;
		}

		public SingleData<?> getData() {
			if (this.forceCnt > 1) {
				return this.lastTransmittedData;
			} else {
				return this.current;

			}
		}

		public ChannelDescription getDescription() {
			return this.solvisData.getDescription();
		}

		public String getName() {
			return this.solvisData.channelInstance.getName();
		}

		public long getTransmittedTimeStamp() {
			return this.transmittedTimeStamp;
		}

		public void setTransmitted(long transmittedTimeStamp) {
			this.transmittedTimeStamp = transmittedTimeStamp;
			if (this.forceCnt <= 1) {
				this.lastTransmittedData = this.current;
			}
		}

		public MqttData getMqttData() {
			if (this.getData() == null) {
				return null;
			}
			String value = this.getDescription().normalize(this.getData()).toString();
			String name = this.solvisData.channelInstance.getName();
			return new MqttData(this.getSolvis(), Mqtt.formatChannelOutTopic(name), value, 0, true);
		}

		public SingleValue toSingleValue(SingleData<?> data) {
			return new SingleValue(this.getDescription().normalize(data));
		}

		@Override
		public String toString() {
			return "Name: " + this.solvisData.getDescription() + ", Value: " + this.getData();
		}

	}

	public SmartHomeData getSmartHomeData() {
		if (this.dontSend) {
			return null;
		} else {
			return this.smartHomeData;
		}
	}

	public Integer getScanInterval_ms() {
		return this.getDescription().getScanInterval_ms(this.getSolvis());
	}

	public ChannelInstance getChannelInstance() {
		return this.channelInstance;
	}

	public int getExecutionTime() {
		return this.executionTime;
	}

	public String getCsvMeta(String column, boolean semicolon) {
		if (this.smartHomeData == null || this.getChannelInstance() == null) {
			return null;
		}
		String csv = null;
		switch (column) {
			case Csv.NAME:
				csv = this.getChannelInstance().getName();
				break;
			case Csv.ALIAS:
				csv = this.getChannelInstance().getAlias();
				break;
			case Csv.MQTT:
				String name = this.channelInstance.getName();
				csv = Mqtt.formatChannelOut(name);
				break;
		}
		if (csv == null) {
			csv = this.getDescription().getCsvMeta(column, semicolon);
		}
		return csv;
	}

}
