package de.sgollmer.solvismax.model.objects.data;

public class ModeValue< M extends ModeI > implements SingleData {
	
	private final M mode ;
	
	public ModeValue( M mode) {
		this.mode = mode ;
	}

	public M get() {
		return this.mode ;
	}

	@Override
	public Integer getInt() {
		return null;
	}

	@Override
	public SingleData create(long divisor, int divident) {
		return null;
	}

}
