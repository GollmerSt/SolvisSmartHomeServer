package de.sgollmer.solvismax.model.objects.data;

import java.util.Collection;

public class ModeValue< M extends ModeI > implements SingleData {
	
	private final M mode ;
	
	public ModeValue( M mode) {
		this.mode = mode ;
	}

	@Override
	public SingleData average(Collection<SingleData> values) {
		return null;
	}
	
	public M get() {
		return this.mode ;
	}

}
