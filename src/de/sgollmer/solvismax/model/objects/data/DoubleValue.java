/************************************************************************
 * 
 * $Id$
 *
 * 
 ************************************************************************/

package de.sgollmer.solvismax.model.objects.data;

import java.util.Locale;

import de.sgollmer.solvismax.Constants;
import de.sgollmer.solvismax.helper.Helper;

public class DoubleValue extends SingleData<Double> {

	private final double value;

	public DoubleValue(final double value, final long timeStamp) {
		super(timeStamp);
		this.value = value;
	}

	@Override
	public Helper.Boolean getBoolean() {
		return Helper.Boolean.UNDEFINED;
	}

	@Override
	public Integer getInt() {
		return (int) Math.round(this.value);
	}

	@Override
	public Long getLong() {
		return Math.round(this.value);
	}

	@Override
	public Double getDouble() {
		return this.value;
	}

	@Override
	public SingleData<Double> create(final int value, final long timeStamp) {
		return new DoubleValue(value, timeStamp);
	}

	@Override
	public String getXmlId() {
		return null;
	}

	@Override
	public String toString() {
		return String.format(Locale.US, "%1.4f", this.value);
	}

	@Override
	public String toJson() {
		return Double.toString(this.value);
	}

	@Override
	public Double get() {
		return this.value;
	}

	@Override
	boolean isFastChange() {
		return false;
	}

	@Override
	public boolean equals(final Object obj) {
		if (!(obj instanceof SingleData)) {
			return false;
		}
		Double cmp = ((SingleData<?>) obj).getDouble();
		if (cmp == null) {
			return false;
		}
		double max = Math.max(Math.abs(this.value), Math.abs(cmp));
		double maxEqualDiff = max * Constants.PRECISION_DOUBLE;
		double diff = this.value - cmp;
		return Math.abs(diff) <= maxEqualDiff;
	}

	@Override
	public int hashCode() {
		return Double.valueOf(this.value).hashCode();
	}

	@Override
	public int compareTo(final SingleData<?> o) {
		if (o instanceof DoubleValue) {
			return Double.compare(this.value, ((DoubleValue) o).value);
		} else if (o != null) {
			return this.getClass().getCanonicalName().compareTo(o.getClass().getCanonicalName());
		} else {
			return 1;
		}
	}

	@Override
	public SingleData<Double> create(final long timeStamp) {
		return new DoubleValue(this.value, timeStamp);
	}

}
