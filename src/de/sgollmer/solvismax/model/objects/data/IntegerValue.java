/************************************************************************
 * 
 * $Id$
 *
 * 
 ************************************************************************/

package de.sgollmer.solvismax.model.objects.data;

import de.sgollmer.solvismax.Constants;

public class IntegerValue extends SingleData<Integer> {
	private final Integer data;
	private final boolean fastChange;

	private IntegerValue(Integer value, long timeStamp, boolean fastChange) {
		super(timeStamp);
		this.data = value;
		this.fastChange = fastChange;
	}


	@Override
	public Boolean getBoolean() {
		if ( this.data != null ) {
			return this.data != 0;
		}
		return null;
	}

	public IntegerValue(Integer value, long timeStamp) {
		this(value, timeStamp, false);
	}

	@Override
	public boolean equals(Object obj) {
		if (this.data != null && obj instanceof IntegerValue) {
			return this.data.equals(((IntegerValue) obj).data);
		} else if (obj instanceof DoubleValue) {
			return this.data.equals(((DoubleValue) obj).getInt());
		} else {
			return false;
		}
	}

	@Override
	public int hashCode() {
		return Integer.valueOf(this.data).hashCode();
	}

	@Override
	public Integer getInt() {
		return this.data;
	}

	@Override
	public SingleData<Integer> create(int value, long timeStamp, boolean fastChange) {
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
		return Constants.XmlStrings.XML_MEASUREMENT_INTEGER;
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
