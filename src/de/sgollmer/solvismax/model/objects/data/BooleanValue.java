package de.sgollmer.solvismax.model.objects.data;

import java.util.Collection;

public class BooleanValue implements SingleData {

	boolean value;

	public BooleanValue(boolean value) {
		this.value = value;
	}

	@Override
	public SingleData average(Collection<SingleData> values) {
		return null;
	}
	
	public Boolean get() {
		return value ;
	}

}
