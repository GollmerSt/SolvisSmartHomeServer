package de.sgollmer.solvismax.model.objects.data;

import java.util.Calendar;

import de.sgollmer.solvismax.error.TypeError;
import de.sgollmer.solvismax.model.objects.AllSolvisData;
import de.sgollmer.solvismax.model.objects.DataDescription;
import de.sgollmer.solvismax.model.objects.Observer;
import de.sgollmer.solvismax.model.objects.Observer.Observable;

public class SolvisData extends Observer.Observable<SolvisData> implements Cloneable {

	private final DataDescription description;
	private final AllSolvisData datas;

	private final Average average;

	private SingleData data;

	private Observer.Observable<SolvisData> continousObservable = null;

	public SolvisData(DataDescription description, AllSolvisData datas) {
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

	public SolvisData(SingleData data) {
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

	private void setData(SingleData data) {
		if (this.description.isAverage()) {
			this.average.add(data);
			data = this.average.getAverage(data);
		}

		if (!data.equals(this.data)) {
			this.data = data;
			this.notify(this);
			System.out.println( this.getId() + ": " + data ) ;
		}
		if (this.continousObservable != null) {
			this.continousObservable.notify(this);
		}
	}

	/**
	 * @return the description
	 */
	public DataDescription getDescription() {
		return this.description;
	}

	public void setInteger(Integer integer) {
		this.setData(new IntegerValue(integer));
	}

	public Integer getInteger() {
		if (this.data == null) {
			return null;
		} else if (!(this.data instanceof IntegerValue)) {
			throw new TypeError("TypeError: Type actual: <" + this.data.getClass() + ">, target: <IntegerValue>");
		}
		return ((IntegerValue) this.data).getData();
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

	public void setSingleData(SingleData data) {
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
		return this.data.toString() ;
	}

}
