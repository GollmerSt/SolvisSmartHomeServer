package de.sgollmer.solvismax.model.objects.data;

public class BooleanValue implements SingleData {

	boolean value;

	public BooleanValue(boolean value) {
		this.value = value;
	}

	public Boolean get() {
		return value;
	}

	@Override
	public Integer getInt() {
		return this.value ? 1 : 0;
	}

	@Override
	public SingleData create(long divisor, int divident) {
		return new BooleanValue(divisor * 2 >= divident);
	}

}
