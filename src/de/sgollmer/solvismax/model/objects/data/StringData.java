/************************************************************************
 * 
 * $Id$
 *
 * 
 ************************************************************************/

package de.sgollmer.solvismax.model.objects.data;

import de.sgollmer.solvismax.Constants;
import de.sgollmer.solvismax.helper.Helper;

public class StringData extends SingleData<String> {
	private final String data;

	public StringData(String data, long timeStamp) {
		super(timeStamp);
		this.data = data;
	}

	@Override
	public boolean equals(Object obj) {
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
	public Helper.Boolean getBoolean() {
		return Helper.Boolean.UNDEFINED;
	}

	@Override
	public Integer getInt() {
		return null;
	}

	@Override
	public Long getLong() {
		return null;
	}

	@Override
	public Double getDouble() {
		return null;
	}

	@Override
	public SingleData<String> create(int value, long timeStamp) {
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
	public int compareTo(SingleData<?> o) {
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
	public SingleData<String> create(long timeStamp) {
		return new StringData(this.data, timeStamp);
	}

}
