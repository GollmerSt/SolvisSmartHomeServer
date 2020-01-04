/************************************************************************
 * 
 * $Id: 73_SolvisClient.pm 78 2020-01-03 17:50:08Z stefa $
 *
 * 
 ************************************************************************/

package de.sgollmer.solvismax.model.objects.data;

import java.util.Calendar;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import de.sgollmer.solvismax.connection.transfer.SingleValue;
import de.sgollmer.solvismax.error.TypeError;
import de.sgollmer.solvismax.model.objects.AllSolvisData;
import de.sgollmer.solvismax.model.objects.ChannelDescription;
import de.sgollmer.solvismax.model.objects.Observer;
import de.sgollmer.solvismax.model.objects.Observer.Observable;

public class SolvisData extends Observer.Observable<SolvisData> implements Cloneable {

	private static final Logger logger = LogManager.getLogger(SolvisData.class);

	private final ChannelDescription description;
	private final AllSolvisData datas;

	private final Average average;

	private SingleData<?> data;

	private Observer.Observable<SolvisData> continousObservable = null;

	public SolvisData(ChannelDescription description, AllSolvisData datas) {
		this.description = description;
		this.datas = datas;
		if (this.description.isAverage()) {
			this.average = new Average(datas.getAverageCount());
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
			return data.equals(((SolvisData) obj).data);
		} else {
			return false;
		}
	}

	@Override
	public int hashCode() {
		return this.data.hashCode();
	}

	public String getId() {
		return this.description.getId();
	}

	private void setData(SingleData<?> data) {
		this.setData(data, this);
	}

	private void setData(SingleData<?> data, Object source) {

		if (this.description.isAverage()) {
			this.average.add(data);
			data = this.average.getAverage(data);
		}

		if (!data.equals(this.data)) {
			this.data = data;
			this.notify(this, source);
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

	public void setInteger(Integer integer, Object source) {
		this.setData(new IntegerValue(integer), source);
	}

	public void setInteger(Integer integer) {
		this.setData(new IntegerValue(integer));
	}

	public Integer getInteger() {
		if (this.data == null) {
			return null;
		} else if (this.data instanceof FloatValue) {
			float f = ((FloatValue) this.data).get();
			return Math.round(f);

		} else if ((this.data instanceof IntegerValue)) {
			return ((IntegerValue) this.data).get();
		}
		throw new TypeError("TypeError: Type actual: <" + this.data.getClass() + ">, target: <IntegerValue>");
	}

	public int getInt() {
		Integer value = this.getInteger();
		if (value == null) {
			value = 0;
		}
		return value;
	}

	public void setDate(Calendar calendar) {
		this.setData(new DateValue(calendar));

	}

	public void setBoolean(Boolean bool) {
		this.setData(new BooleanValue(bool));

	}

	public Boolean getBoolean() {
		if (this.data == null) {
			return null;
		} else if (!(this.data instanceof BooleanValue)) {
			throw new TypeError("TypeError: Type actual: <" + this.data.getClass() + ">, target: <BooleanValue>");
		}
		return ((BooleanValue) this.data).get();
	}

	public boolean getBool() {
		Boolean bool = this.getBoolean();
		if (bool == null) {
			bool = false;
		}
		return bool;
	}

	public void setMode(ModeI mode) {
		this.setData(new ModeValue<>(mode));
	}

	public ModeValue<?> getMode() {
		if (this.data instanceof ModeValue<?>) {
			return (ModeValue<?>) this.data;
		} else {
			return null;
		}
	}

	public void setSingleData(SingleData<?> data) {
		this.setData(data);
	}

	public void registerContinuousObserver(Observer.ObserverI<SolvisData> observer) {
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

	public SingleValue toSingleValue() {
		if (this.data instanceof IntegerValue) {
			return new SingleValue(new FloatValue((float) this.getInt() / this.getDescription().getDivisor()));
		} else {
			return new SingleValue(data);
		}
	}

}
