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

public class StringData extends SingleData<String> {
	private final String data;

	public StringData(final String data, final long timeStamp) {
		super(timeStamp);
		this.data = data;
	}

	@Override
	public boolean equals(final Object obj) {
		if (obj instanceof StringData) {
			return this.data.equals(((StringData) obj).data);
		} else if (obj == null) {
			return false;
		} else {
			return obj.equals(this);
		}
	}

	@Override
	public int hashCode() {
		return this.data.hashCode();
	}

	@Override
	public String toString() {
		return this.data;
	}

	@Override
	public Helper.Boolean getBoolean() throws TypeException {
		throw new TypeException(this.getClass().toString() + " can't be converted to Boolean");
	}

	@Override
	public Integer getInt() throws TypeException {
		throw new TypeException(this.getClass().toString() + " can't be converted to Integer");
	}

	@Override
	public Long getLong() throws TypeException {
		throw new TypeException(this.getClass().toString() + " can't be converted to Long");
	}

	@Override
	public Double getDouble() throws TypeException {
		throw new TypeException(this.getClass().toString() + " can't be converted to Double");
	}

	@Override
	public SingleData<String> create(final int value, final long timeStamp) {
		return null;
	}

	@Override
	public String getXmlId() {
		return Constants.XmlStrings.XML_MEASUREMENT_STRING;
	}

	@Override
	public String toJson() {
		return "\"" + this.data + "\"";
	}

	@Override
	public String get() {
		return this.data;
	}

	@Override
	public int compareTo(final SingleData<?> o) {
		if (o instanceof StringData) {
			String cmp = ((StringData) o).data;
			if (this.data == null) {
				return cmp == null ? 0 : -1;
			} else {
				return this.data.compareTo(cmp);
			}
		} else if (o != null) {
			return this.getClass().getCanonicalName().compareTo(o.getClass().getCanonicalName());
		} else {
			return 1;
		}
	}

	@Override
	public SingleData<String> clone(final long timeStamp) {
		return new StringData(this.data, timeStamp);
	}

	@Override
	public SingleData<String> add(SingleData<?> data) throws TypeException {
		throw new TypeException("not supported");
	}

	@Override
	public SingleData<String> mult(SingleData<?> data) throws TypeException {
		throw new TypeException("not supported");
	}

	@Override
	public String getModeString() {
		return this.data;
	}

	@Override
	public boolean isNumeric() {
		return false;
	}
}
