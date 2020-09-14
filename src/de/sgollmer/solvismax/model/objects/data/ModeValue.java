/************************************************************************
 * 
 * $Id$
 *
 * 
 ************************************************************************/

package de.sgollmer.solvismax.model.objects.data;

import de.sgollmer.solvismax.Constants;

public class ModeValue<M extends IMode> extends SingleData<M> {

	private final M mode;

	public ModeValue(M mode, long timeStamp) {
		super(timeStamp);
		this.mode = mode;
	}

	@Override
	public M get() {
		return this.mode;
	}

	@Override
	public Boolean getBoolean() {
		return null;
	}

	@Override
	public Integer getInt() {
		return null;
	}

	@Override
	public SingleData<M> create(int value, long timeStamp) {
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
		return Constants.XmlStrings.XML_MEASUREMENT_MODE;
	}

	@Override
	public String toJson() {
		return "\"" + this.toString() + "\"";
	}

}
