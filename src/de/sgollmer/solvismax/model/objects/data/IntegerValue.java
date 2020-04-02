/************************************************************************
 * 
 * $Id$
 *
 * 
 ************************************************************************/

package de.sgollmer.solvismax.model.objects.data;

import de.sgollmer.solvismax.model.objects.backup.Measurement;

public class IntegerValue extends SingleData<Integer> {
	private final Integer data;
	private final boolean fastChange;

	public IntegerValue(Integer value, long timeStamp, boolean fastChange) {
		super(timeStamp);
		this.data = value;
		this.fastChange = fastChange;
	}
	
	public  IntegerValue(Integer value, long timeStamp) {
		this(value, timeStamp, false) ;
	}

	@Override
	public boolean equals(Object obj) {
		if (obj instanceof IntegerValue) {
			return this.data.equals(((IntegerValue) obj).data);
		} else {
			return false;
		}
	}

	@Override
	public int hashCode() {
		return this.data.hashCode();
	}

	@Override
	public Integer getInt() {
		return this.data;
	}

	@Override
	public SingleData<Integer> create(int value, long timeStamp,boolean fastChange) {
		return new IntegerValue(value, timeStamp, fastChange);
	}

	@Override
	public SingleData<Integer> create(int value, long timeStamp) {
		return new IntegerValue(value, timeStamp);
	}

	@Override
	public String toString() {
		return Integer.toString(this.data);
	}

	@Override
	public String getXmlId() {
		return Measurement.XML_MEASUREMENT_INTEGER;
	}

	@Override
	public String toJson() {
		return Integer.toString(this.data);
	}

	@Override
	public Integer get() {
		return this.data;
	}

	@Override
	public boolean isFastChange() {
		return this.fastChange;
	}


}
