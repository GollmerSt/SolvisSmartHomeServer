package de.sgollmer.solvismax.model.objects.data;

public class FloatValue implements SingleData<Float> {
	
	private final float value ;
	
	public FloatValue(float value ) {
		this.value = value ;
	}

	@Override
	public Integer getInt() {
		return Math.round( value ) ;
	}

	@Override
	public SingleData<Float> create(int value) {
		return new FloatValue(value);
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
		return this.value ;
	}

}
