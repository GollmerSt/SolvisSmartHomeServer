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
import de.sgollmer.solvismax.model.objects.Observer;
import de.sgollmer.solvismax.model.objects.Observer.IObserver;
import de.sgollmer.solvismax.model.objects.Observer.Observable;

public class SolvisData extends Observer.Observable<SolvisData> implements Cloneable, IObserver<SolvisState> {

	private static final ILogger logger = LogManager.getInstance().getLogger(SolvisData.class);

	private final ChannelDescription description;
	private final AllSolvisData datas;

	private final Average average;
	private SingleData<?> data = null;
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
		this.setData(data, this);
	}

	private synchronized void setData(SingleData<?> data, Object source) {

		if (data == null || data.get() == null) {
			this.data = null;
			return;
		}

		if (this.description.isAverage()) {
			this.average.add(data);
			data = this.average.getAverage(data);
		}

		if (data != null && !data.equals(this.data)) {
			this.data = data;
			if (!this.description.isWriteable() || !this.datas.getSolvis().willBeModified(this)) {
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
		this.setData(new IntegerValue(integer, timeStamp), source);
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
		} else if (!(data instanceof BooleanValue)) {
			throw new TypeException("TypeException: Type actual: <" + data.getClass() + ">, target: <BooleanValue>");
		}
		return ((BooleanValue) data).get();
	}

	public boolean getBool() throws TypeException {
		Boolean bool = this.getBoolean();
		if (bool == null) {
			bool = false;
		}
		return bool;
	}

	public void setMode(IMode mode, long timeStamp) {
		this.setData(new ModeValue<>(mode, timeStamp));
	}

	public ModeValue<?> getMode() {
		SingleData<?> data = this.data;
		if (data instanceof ModeValue<?>) {
			return (ModeValue<?>) data;
		} else {
			return null;
		}
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
	public void update(SolvisState data, Object source) {
		if (data.getState() == SolvisState.State.POWER_OFF && this.average != null) {
			this.average.clear();
		}
	}

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

}
