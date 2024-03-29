/************************************************************************
 * 
 * $Id$
 *
 * 
 ************************************************************************/

package de.sgollmer.solvismax.model.objects.data;

import de.sgollmer.solvismax.Constants;
import de.sgollmer.solvismax.error.TypeException;
import de.sgollmer.solvismax.helper.Helper;

public class LongValue extends SingleData<Long> {
	private final Long data;
	private final boolean fastChange;

	private LongValue(final Long value, final long timeStamp, final boolean fastChange) {
		super(timeStamp);
		this.data = value;
		this.fastChange = fastChange;
	}

	@Override
	public Helper.Boolean getBoolean() {
		if (this.data != null) {
			return this.data == 0 ? Helper.Boolean.FALSE : Helper.Boolean.TRUE;
		}
		return Helper.Boolean.UNDEFINED;
	}

	public LongValue(final Long value, final long timeStamp) {
		this(value, timeStamp, false);
	}

	@Override
	public boolean equals(final Object obj) {
		if (!(obj instanceof SingleData<?>)) {
			return false;
		}
		SingleData<?> cmp = (SingleData<?>) obj;
		if (cmp == null || this.data == null) {
			return false;
		}

		try {
			return this.data.equals(cmp.getLong());
		} catch (TypeException e) {
			return false;
		}
	}

	@Override
	public int hashCode() {
		return Long.valueOf(this.data).hashCode();
	}

	@Override
	public Integer getInt() {
		if (this.data == null) {
			return null;
		} else {
			return this.data.intValue();
		}
	}

	@Override
	public Long getLong() {
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
	public SingleData<Long> create(final int value, final long timeStamp) {
		return new LongValue((long) value, timeStamp);
	}

	@Override
	public String toString() {
		return Long.toString(this.data);
	}

	@Override
	public String getXmlId() {
		return Constants.XmlStrings.XML_MEASUREMENT_INTEGER;
	}

	@Override
	public String toJson() {
		return Long.toString(this.data);
	}

	@Override
	public Long get() {
		return this.data;
	}

	@Override
	public boolean isFastChange() {
		return this.fastChange;
	}

	@Override
	public int compareTo(final SingleData<?> o) {
		if (o instanceof LongValue || o instanceof IntegerValue) {
			Long cmp;
			try {
				cmp = ((SingleData<?>) o).getLong();
			} catch (TypeException e) {
				return -1;
			}
			if (this.data == null) {
				return cmp == null ? 0 : -1;
			} else {
				return Long.compare(this.data, cmp);
			}
		} else if (o != null) {
			return this.getClass().getCanonicalName().compareTo(o.getClass().getCanonicalName());
		} else {
			return 1;
		}
	}

	@Override
	public SingleData<Long> clone(final long timeStamp) {
		return new LongValue(this.data, timeStamp);
	}

	@Override
	public SingleData<Long> add(SingleData<?> data) throws TypeException {
		Long l1 = this.getLong();
		Long l2 = data.getLong();
		return new LongValue((l1 == null || l2 == null) ? null : l1 + l2, this.getTimeStamp());
	}

	@Override
	public SingleData<Long> mult(SingleData<?> data) throws TypeException {
		Long l1 = this.getLong();
		Long l2 = data.getLong();
		return new LongValue((l1 == null || l2 == null) ? null : l1 * l2, this.getTimeStamp());
	}

	@Override
	public boolean isNumeric() {
		return true;
	}

}
