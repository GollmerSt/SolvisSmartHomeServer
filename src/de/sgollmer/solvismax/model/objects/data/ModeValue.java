/************************************************************************
 * 
 * $Id$
 *
 * 
 ************************************************************************/

package de.sgollmer.solvismax.model.objects.data;

import de.sgollmer.solvismax.Constants;
import de.sgollmer.solvismax.helper.Helper;

public class ModeValue<M extends IMode<M>> extends SingleData<M> {

	private final M mode;

	public ModeValue(final M mode, final long timeStamp) {
		super(timeStamp);
		this.mode = mode;
	}

	@Override
	public M get() {
		return this.mode;
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
	public SingleData<M> create(final int value, final long timeStamp) {
		return null;
	}

	@Override
	public boolean equals(final Object obj) {
		if (!(obj instanceof SingleData)) {
			return false;
		} else if (obj instanceof ModeValue<?>) {
			return this.mode.getName().equals(((ModeValue<?>) obj).mode.getName());
		} else if (obj instanceof StringData) {
			return (this.mode.getName().equals(((StringData) obj).get()));
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

	@Override
	public int compareTo(final SingleData<?> o) {
		if (o instanceof ModeValue) {
			@SuppressWarnings("unchecked")
			M cmp = (M) ((ModeValue<?>) o).mode;
			if (this.mode == null) {
				return cmp == null ? 0 : -1;
			} else {
				return this.mode.compareTo(cmp);
			}
		} else if (o != null) {
			return this.getClass().getCanonicalName().compareTo(o.getClass().getCanonicalName());
		} else {
			return 1;
		}
	}

	@Override
	public SingleData<M> create(final long timeStamp) {
		return new ModeValue<M>(this.mode, timeStamp);
	}

}
