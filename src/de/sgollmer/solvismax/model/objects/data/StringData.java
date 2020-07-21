/************************************************************************
 * 
 * $Id$
 *
 * 
 ************************************************************************/

package de.sgollmer.solvismax.model.objects.data;

import de.sgollmer.solvismax.Constants;

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
		} else {
			return false;
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
	public Integer getInt() {
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

}
