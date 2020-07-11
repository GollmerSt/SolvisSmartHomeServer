/************************************************************************
 * 
 * $Id$
 *
 * 
 ************************************************************************/

package de.sgollmer.solvismax.model.objects.data;

import de.sgollmer.solvismax.Constants;

public class FloatValue extends SingleData<Double> {

	private final double value;

	public FloatValue(double value, long timeStamp) {
		super(timeStamp);
		this.value = value;
	}

	@Override
	public Integer getInt() {
		return (int) Math.round(this.value);
	}

	@Override
	public SingleData<Double> create(int value, long timeStamp) {
		return new FloatValue(value, timeStamp);
	}

	@Override
	public String getXmlId() {
		return null;
	}

	@Override
	public String toString() {
		return Double.toString(this.value);
	}

	@Override
	public String toJson() {
		return this.toString();
	}

	@Override
	public Double get() {
		return this.value;
	}

	@Override
	public boolean isFastChange() {
		return false;
	}

	@Override
	public boolean equals(Object obj) {
		if ( !(obj instanceof SingleData)) {
			return false ;
		}
		double cmp ;
		double maxEqualDiff = Math.abs(this.value) * Constants.PRECISION_DOUBLE ;
		if (obj instanceof FloatValue) {
			cmp = ((FloatValue) obj).value ;
		} else if (obj instanceof IntegerValue ) {
			cmp = ((IntegerValue) obj).get() ;
		} else {
			return false ;
		}
		double diff = this.value - cmp ;
		return Math.abs(diff) < maxEqualDiff;
	}

	@Override
	public int hashCode() {
		return Double.valueOf(this.value).hashCode();
	}

}
