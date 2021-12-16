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

public class BooleanValue extends SingleData<Boolean> {

	boolean value;

	public BooleanValue(final boolean value, final long timeStamp) {
		super(timeStamp);
		this.value = value;
	}

	@Override
	public Boolean get() {
		return this.value;
	}

	@Override
	public Helper.Boolean getBoolean() {
		return this.value ? Helper.Boolean.TRUE : Helper.Boolean.FALSE;
	}

	@Override
	public Integer getInt() {
		return this.value ? 1 : 0;
	}

	@Override
	public Long getLong() {
		return this.value ? 1L : 0L;
	}

	@Override
	public Double getDouble() {
		return this.value ? 1.0 : 0.0;
	}

	@Override
	public SingleData<Boolean> create(final int value, final long timeStamp) {
		return new BooleanValue(value > 0, timeStamp);
	}

	@Override
	public String toString() {
		return Boolean.toString(this.value);
	}

	@Override
	public boolean equals(final Object obj) {
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

	@Override
	public int compareTo(final SingleData<?> o) {
		if (o instanceof BooleanValue) {
			return Boolean.compare(this.value, ((BooleanValue) o).value);
		} else if (o != null) {
			return this.getClass().getCanonicalName().compareTo(o.getClass().getCanonicalName());
		} else {
			return 1;
		}
	}

	@Override
	public SingleData<Boolean> clone(final long timeStamp) {
		return new BooleanValue(this.value, timeStamp);
	}

	@Override
	public BooleanValue add(SingleData<?> data) throws TypeException{
		if ( !(data instanceof BooleanValue )) {
			throw new TypeException();
		}
		return new BooleanValue(this.get() != data.get(), this.getTimeStamp());
	}

	@Override
	public BooleanValue mult(SingleData<?> data) throws TypeException{
		if ( !(data instanceof BooleanValue )) {
			throw new TypeException();
		}
		return new BooleanValue((boolean)this.get() && (boolean)((BooleanValue)data).get(), this.getTimeStamp());
	}

	@Override
	public boolean isNumeric() {
		return false;
	}

}
