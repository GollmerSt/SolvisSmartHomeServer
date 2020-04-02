/************************************************************************
 * 
 * $Id$
 *
 * 
 ************************************************************************/

package de.sgollmer.solvismax.model.objects.data;

public class FloatValue extends SingleData<Float> {

	private final float value;
	private final boolean fastChange;

	public FloatValue(float value, long timeStamp, boolean fastChange) {
		super(timeStamp);
		this.value = value;
		this.fastChange = fastChange;
	}

	public FloatValue(float value, long timeStamp) {
		this(value, timeStamp, false);
	}

	@Override
	public Integer getInt() {
		return Math.round(this.value);
	}

	@Override
	public SingleData<Float> create(int value, long timeStamp) {
		return new FloatValue(value, timeStamp);
	}

	@Override
	public String getXmlId() {
		return null;
	}

	@Override
	public String toString() {
		return Float.toString(this.value);
	}

	@Override
	public String toJson() {
		return this.toString();
	}

	@Override
	public Float get() {
		return this.value;
	}

	@Override
	public boolean isFastChange() {
		return this.fastChange;
	}

}
