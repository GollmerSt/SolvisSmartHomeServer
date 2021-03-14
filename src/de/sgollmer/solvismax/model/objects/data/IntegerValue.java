/************************************************************************
 * 
 * $Id$
 *
 * 
 ************************************************************************/

package de.sgollmer.solvismax.model.objects.data;

import de.sgollmer.solvismax.Constants;
import de.sgollmer.solvismax.helper.Helper;

public class IntegerValue extends SingleData<Integer> {
	private final Integer data;
	private final boolean fastChange;

	private IntegerValue(Integer value, long timeStamp, boolean fastChange) {
		super(timeStamp);
		this.data = value;
		this.fastChange = fastChange;
	}

	@Override
	public Helper.Boolean getBoolean() {
		if (this.data != null) {
			return this.data == 0?Helper.Boolean.FALSE:Helper.Boolean.TRUE;
		}
		return Helper.Boolean.UNDEFINED;
	}

	public IntegerValue(Integer value, long timeStamp) {
		this(value, timeStamp, false);
	}

	@Override
	public boolean equals(Object obj) {
		if (!(obj instanceof SingleData<?>)) {
			return false;
		}
		SingleData<?> cmp = (SingleData<?>) obj;
		if (cmp == null || this.data == null) {
			return false;
		}
		return this.data.equals(cmp.getInt());
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
	public Double getDouble() {
		if (this.data != null) {
			return this.data.doubleValue();
		}
		return null;
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

	@Override
	public int compareTo(SingleData<?> o) {
		if (o instanceof IntegerValue) {
			Integer cmp = ((IntegerValue) o).data;
			if (this.data == null) {
				return cmp == null ? 0 : -1;
			} else {
				return Integer.compare(this.data, ((IntegerValue) o).data);
			}
		} else if (o != null) {
			return this.getClass().getCanonicalName().compareTo(o.getClass().getCanonicalName());
		} else {
			return 1;
		}
	}

}
