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
	public SingleData<M> create(final int value, final long timeStamp) {
		return null;
	}

	@Override
	public boolean equals(final Object obj) {
		if (!(obj instanceof SingleData)) {
			return false;
		}
		String comp = ((SingleData<?>) obj).getModeString();

		if (comp == null) {
			return false;
		} else {
			return comp.equals(this.getModeString());
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
	public SingleData<M> clone(final long timeStamp) {
		return new ModeValue<M>(this.mode, timeStamp);
	}

	@Override
	public SingleData<M> add(SingleData<?> data) throws TypeException {
		throw new TypeException("not supported");
	}

	@Override
	public SingleData<M> mult(SingleData<?> data) throws TypeException {
		throw new TypeException("not supported");
	}

	@Override
	public String getModeString() {
		return this.mode.getName();
	}

	@Override
	public boolean isNumeric() {
		return false;
	}

}
