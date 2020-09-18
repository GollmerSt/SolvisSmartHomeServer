/************************************************************************
 * 
 * $Id$
 *
 * 
 ************************************************************************/

package de.sgollmer.solvismax.model.objects.data;

import java.util.Locale;

import de.sgollmer.solvismax.Constants;

public class DoubleValue extends SingleData<Double> {

	private final double value;

	public DoubleValue(double value, long timeStamp) {
		super(timeStamp);
		this.value = value;
	}

	@Override
	public Boolean getBoolean() {
		return null;
	}

	@Override
	public Integer getInt() {
		return (int) Math.round(this.value);
	}

	@Override
	public SingleData<Double> create(int value, long timeStamp) {
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
	public boolean equals(Object obj) {
		if (!(obj instanceof SingleData)) {
			return false;
		}
		double cmp;
		double maxEqualDiff = Math.abs(this.value) * Constants.PRECISION_DOUBLE;
		if (obj instanceof DoubleValue) {
			cmp = ((DoubleValue) obj).value;
		} else if (obj instanceof IntegerValue) {
			cmp = ((IntegerValue) obj).get();
		} else {
			return false;
		}
		double diff = this.value - cmp;
		return Math.abs(diff) < maxEqualDiff;
	}

	@Override
	public int hashCode() {
		return Double.valueOf(this.value).hashCode();
	}

}
