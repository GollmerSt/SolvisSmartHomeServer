package de.sgollmer.solvismax.model.objects.data;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collection;
import java.util.Iterator;

import de.sgollmer.solvismax.error.TypeError;
import de.sgollmer.solvismax.model.objects.AllSolvisData;
import de.sgollmer.solvismax.model.objects.DataDescription;
import de.sgollmer.solvismax.model.objects.Observer;
import de.sgollmer.solvismax.model.objects.Observer.Observable;

public class SolvisData extends Observer.Observable<SolvisData> implements Cloneable {

	private final DataDescription description;
	private final AllSolvisData datas;

	private Collection<SingleData> lastMeasureValues = null;

	private SingleData data;
	
	private Observer.Observable<SolvisData> continousObservable = null ;

	public SolvisData(DataDescription description, AllSolvisData datas) {
		this.description = description;
		this.datas = datas;
	}
	
	@Override
	public SolvisData clone() {
		SolvisData data = new SolvisData(this.description, this.datas ) ;
		data.lastMeasureValues = new ArrayList<>( lastMeasureValues ) ;
		data.data = this.data ;
		return data ;
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
			if (this.lastMeasureValues.size() > this.datas.getAverageCount()) {
				Iterator<SingleData> it = this.lastMeasureValues.iterator();
				it.next();
				it.remove();
			}
			lastMeasureValues.add(data);
			int average = 0;
			for (SingleData d : this.lastMeasureValues) {
				if (!(d instanceof IntegerValue)) {
					throw new TypeError("Illegal type <" + d.getClass() + ">");
				}
				average += ((IntegerValue) d).getData();
			}
			data = new IntegerValue(average);
		}
		
		if ( !data.equals(this.data)) {
			this.data = data ;
			this.notify(this);
		}
		if ( this.continousObservable != null ) {
			this.continousObservable.notify(this);
		}
	}
	
	/**
	 * @return the description
	 */
	public DataDescription getDescription() {
		return this.description;
	}

	public void setInteger( Integer integer ) {
		this.setData( new IntegerValue(integer )) ;
	}

	public Integer getInteger() {
		if (this.data == null) {
			return null;
		} else if (!(this.data instanceof IntegerValue)) {
			throw new TypeError("TypeError: Type actual: <" + this.data.getClass() + ">, target: <IntegerValue>");
		}
		return ((IntegerValue)this.data).getData() ;
	}
	
	public int getInt() {
		Integer value = this.getInteger() ;
		if ( value == null ) {
			value = 0 ;
		}
		return value ;
	}

	public void setDate(Calendar calendar) {
		this.setData( new DateValue(calendar) );
		
	}

	public void setBoolean(Boolean bool) {
		this.setData( new BooleanValue(bool) );
		
	}
	
	public Boolean getBoolean() {
		if (this.data == null) {
			return null;
		} else if (!(this.data instanceof BooleanValue)) {
			throw new TypeError("TypeError: Type actual: <" + this.data.getClass() + ">, target: <BooleanValue>");
		}
		return ((BooleanValue)this.data).get() ;
	}
	
	public boolean getBool() {
		Boolean bool = this.getBoolean() ;
		if ( bool == null ) {
			bool = false ;
		}
		return bool ;
	}
	
	public void setMode( ModeI mode ) {
		this.setData( new ModeValue<>(mode));
	}

	public void setSingleData( SingleData data ) {
		this.setData(data);
	}
	
	public void registerContinuousObserver( Observer.ObserverI<SolvisData > observer ) {
		if ( this.continousObservable == null ) {
			this.continousObservable = new Observable<>() ;
		}
		this.continousObservable.register(observer);
	}

}
