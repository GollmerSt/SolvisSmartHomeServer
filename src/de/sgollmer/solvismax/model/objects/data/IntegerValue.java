package de.sgollmer.solvismax.model.objects.data;

import de.sgollmer.solvismax.model.objects.backup.Measurement;

public class IntegerValue implements SingleData {
	private final Integer data;

	public IntegerValue(Integer value) {
		this.data = value;
	}

	@Override
	public boolean equals(Object obj) {
		if (obj instanceof IntegerValue) {
			return this.data.equals(((IntegerValue) obj).data);
		} else {
			return false;
		}
	}

	@Override
	public int hashCode() {
		return this.data.hashCode();
	}

	/**
	 * @return the data
	 */
	public Integer getData() {
		return data;
	}

	@Override
	public Integer getInt() {
		return data;
	}

	@Override
	public SingleData create(int value) {
		return new IntegerValue(value);
	}

	@Override
	public String toString() {
		return Integer.toString(data);
	}

	@Override
	public String getXmlId() {
		return Measurement.XML_MEASUREMENT_INTEGER;
	}

}
