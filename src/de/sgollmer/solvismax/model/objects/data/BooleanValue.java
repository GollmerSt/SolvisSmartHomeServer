/************************************************************************
 * 
 * $Id$
 *
 * 
 ************************************************************************/

package de.sgollmer.solvismax.model.objects.data;

import de.sgollmer.solvismax.Constants;

public class BooleanValue extends SingleData<Boolean> {

	boolean value;

	public BooleanValue(boolean value, long timeStamp) {
		super(timeStamp);
		this.value = value;
	}

	@Override
	public Boolean get() {
		return this.value;
	}

	@Override
	public Integer getInt() {
		return this.value ? 1 : 0;
	}

	@Override
	public SingleData<Boolean> create(int value, long timeStamp) {
		return new BooleanValue(value > 0, timeStamp);
	}

	@Override
	public String toString() {
		return Boolean.toString(this.value);
	}

	@Override
	public boolean equals(Object obj) {
		if (obj instanceof BooleanValue) {
			return this.value == ((BooleanValue) obj).value;
		}
		return false;
	}

	@Override
	public int hashCode() {
		return this.value ? 179 : 72;
	}

	@Override
	public String getXmlId() {
		return Constants.XmlStrings.XML_MEASUREMENT_BOOLEAN;
	}

	@Override
	public String toJson() {
		return Boolean.toString(this.value);
	}

}
