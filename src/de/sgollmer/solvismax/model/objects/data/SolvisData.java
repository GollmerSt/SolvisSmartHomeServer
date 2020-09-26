/************************************************************************
 * 
 * $Id$
 *
 * 
 ************************************************************************/

package de.sgollmer.solvismax.model.objects.data;

import java.util.Calendar;

import de.sgollmer.solvismax.connection.mqtt.Mqtt;
import de.sgollmer.solvismax.connection.mqtt.MqttData;
import de.sgollmer.solvismax.connection.transfer.SingleValue;
import de.sgollmer.solvismax.error.TypeException;
import de.sgollmer.solvismax.log.LogManager;
import de.sgollmer.solvismax.log.LogManager.ILogger;
import de.sgollmer.solvismax.model.Solvis;
import de.sgollmer.solvismax.model.SolvisState;
import de.sgollmer.solvismax.model.objects.AllSolvisData;
import de.sgollmer.solvismax.model.objects.ChannelDescription;
import de.sgollmer.solvismax.model.objects.IChannelSource.SetResult;
import de.sgollmer.solvismax.model.objects.Observer;
import de.sgollmer.solvismax.model.objects.Observer.IObserver;
import de.sgollmer.solvismax.model.objects.Observer.Observable;
import de.sgollmer.solvismax.model.objects.ResultStatus;

public class SolvisData extends Observer.Observable<SolvisData> implements Cloneable, IObserver<SolvisState.State> {

	private static final ILogger logger = LogManager.getInstance().getLogger(SolvisData.class);

	private final ChannelDescription description;
	private final AllSolvisData datas;

	private final Average average;
	private SingleData<?> data = null;
	private SingleData<?> pendingData = null;
	private long sentTimeStamp = -1;
	private SingleData<?> sentData = null;

	private Observer.Observable<SolvisData> continousObservable = null;

	public SolvisData(ChannelDescription description, AllSolvisData datas) {
		this.description = description;
		this.datas = datas;
		if (this.description.isAverage()) {
			this.average = new Average(datas.getAverageCount(), datas.getMeasurementHysteresisFactor());
		} else {
			this.average = null;
		}
	}

	private SolvisData(SolvisData data) {
		this.description = data.description;
		this.datas = data.datas;
		if (data.average != null) {
			this.average = new Average(data.average);
		} else {
			this.average = null;
		}
		this.sentTimeStamp = data.sentTimeStamp;
		this.sentData = data.sentData;
	}

	public SolvisData(SingleData<?> data) {
		this.data = data;
		this.description = null;
		this.average = null;
		this.datas = null;
	}

	@Override
	public SolvisData clone() {
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
		return this.description.getId();
	}

	private void setData(SingleData<?> data) {
		this.setData(data, this, ResultStatus.SUCCESS);
	}

	private synchronized void setData(SingleData<?> data, Object source, ResultStatus status) {

		if (data == null || data.get() == null) {
			this.data = null;
			return;
		}

		if (this.description.isAverage()) {
			this.average.add(data);
			data = this.average.getAverage(data);
		}

		if (data == null) {
			return;
		}

		boolean glitchInhibitEnabled = this.description.getGlitchInhibitTime_ms() > 0;
		boolean changed = false;

		if (data.getTimeStamp() > 0 && this.pendingData != null) {

			if (data.equals(this.pendingData)) {

				if (data.getTimeStamp() > this.pendingData.getTimeStamp()
						+ this.description.getGlitchInhibitTime_ms()) {

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

		if (changed || status == ResultStatus.VALUE_VIOLATION) {

			if (!this.description.isWriteable() || !this.datas.getSolvis().willBeModified(this)
					|| status == ResultStatus.VALUE_VIOLATION) {

				this.notify(this, source);
			}
			logger.debug("Channel: " + this.getId() + ", value: " + data.toString());
		}

		if (this.continousObservable != null) {

			this.continousObservable.notify(this, source);
		}
	}

	/**
	 * @return the description
	 */
	public ChannelDescription getDescription() {
		return this.description;
	}

	public void setInteger(Integer integer, long timeStamp, Object source) {
		this.setData(new IntegerValue(integer, timeStamp), source, ResultStatus.SUCCESS);
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

	public Double getDouble() throws TypeException {
		SingleData<?> data = this.data;
		if (data == null) {
			return null;
		} else if (data instanceof DoubleValue) {
			return ((DoubleValue) data).get();

		} else if ((data instanceof IntegerValue)) {
			Integer i = ((IntegerValue) data).get();
			if (i == null) {
				return null;
			} else
				return (double) i;
		} else {
			throw new TypeException("TypeException: Type actual: <" + data.getClass() + ">, target: <IntegerValue>");
		}
	}

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

	public Boolean getBoolean() throws TypeException {
		SingleData<?> data = this.data;
		if (data == null) {
			return null;
		}
		Boolean bool = this.data.getBoolean();
		if (bool == null) {
			throw new TypeException("TypeException: Type actual: <" + data.getClass() + ">, target: <BooleanValue>");
		}
		return bool;
	}

	public boolean getBool() throws TypeException {
		Boolean bool = this.getBoolean();
		if (bool == null) {
			bool = false;
		}
		return bool;
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
		this.setData(data.getData(), this, data.getStatus());
		;
	}

	public void setSingleData(SingleData<?> data) {
		this.setData(data);
	}

	public void registerContinuousObserver(Observer.IObserver<SolvisData> observer) {
		if (this.continousObservable == null) {
			this.continousObservable = new Observable<>();
		}
		this.continousObservable.register(observer);
	}

	@Override
	public String toString() {
		return this.data.toString();
	}

	public SingleData<?> getSingleData() {
		return this.data;
	}

//	private SingleValue toSingleValue() {
//		return this.toSingleValue(this.data);
//	}

	public SingleValue toSingleValue(SingleData<?> data) {
		if (data.get() == null) {
			return null;
		}
		if (data instanceof IntegerValue && this.getDescription().getDivisor() != 1) {
			return new SingleValue(
					new DoubleValue((double) data.getInt() / this.getDescription().getDivisor(), data.getTimeStamp()));
		} else {
			return new SingleValue(data);
		}
	}

	@Override
	public void update(SolvisState.State data, Object source) {
		if (data == SolvisState.State.POWER_OFF && this.average != null) {
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

	public synchronized SingleData<?> setSentData(long sentTimeStamp) {
		this.sentTimeStamp = sentTimeStamp;
		this.sentData = this.data;
		return this.sentData;
	}

	public long getSentTimeStamp() {
		return this.sentTimeStamp;
	}

	public boolean isFastChange() {
		return this.data.isFastChange();
	}

	public Solvis getSolvis() {
		return this.datas.getSolvis();
	}

	public MqttData getMqttData() {
		if (this.data == null) {
			return null;
		}
		String value;
		if (this.data instanceof IntegerValue && this.getDescription().getDivisor() != 1) {
			value = Double.toString((double) this.data.getInt() / this.getDescription().getDivisor());
		} else {
			value = this.data.toString();
		}
		return new MqttData(this.getSolvis(), Mqtt.formatChannelOutTopic(this.getId()), value, 0, true);
	}

	public boolean isValid() {
		if ( this.data == null ) {
			return false ;
		} else if (this.description.isWriteable()){
			return this.datas.getLastHumanAccess() < this.data.getTimeStamp();
		} else {
			return true;
		}
		
	}
}
