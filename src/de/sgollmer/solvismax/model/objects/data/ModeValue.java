package de.sgollmer.solvismax.model.objects.data;

import de.sgollmer.solvismax.model.objects.backup.Measurement;

public class ModeValue<M extends ModeI> implements SingleData<M> {

	private final M mode;

	public ModeValue(M mode) {
		this.mode = mode;
	}

	@Override
	public M get() {
		return this.mode;
	}

	@Override
	public Integer getInt() {
		return null;
	}

	@Override
	public SingleData<M> create(int value) {
		return null;
	}

	@Override
	public boolean equals(Object obj) {
		if (obj instanceof ModeValue<?>) {
			return this.mode.getName().equals(((ModeValue<?>) obj).mode.getName());
		} else {
			return false;
		}
	}

	@Override
	public int hashCode() {
		return this.mode.hashCode();
	}

	@Override
	public String toString() {
		return this.mode.getName();
	}

	@Override
	public String getXmlId() {
		return Measurement.XML_MEASUREMENT_MODE;
	}

	@Override
	public String toJson() {
		return "\"" + this.toString() + "\"";
	}

}
