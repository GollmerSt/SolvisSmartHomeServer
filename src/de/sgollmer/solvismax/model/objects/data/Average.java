package de.sgollmer.solvismax.model.objects.data;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;

public class Average implements Cloneable {

	private final int maxCount;
	private long sum = 0;
	private final Collection<Integer> lastMeasureValues ;

	public Average(int maxCount) {
		this.maxCount = maxCount;
		this.lastMeasureValues = new ArrayList<>(maxCount);
	}
	
	public Average( Average average)  {
		this.maxCount = average.maxCount;
		this.lastMeasureValues = new ArrayList<>( average.lastMeasureValues );
	}


	public void add(SingleData data) {
		Integer i = data.getInt();
		if (i != null) {
			sum += i;
		}
		lastMeasureValues.add(i);
		if (lastMeasureValues.size() > this.maxCount) {
			Iterator<Integer> it = this.lastMeasureValues.iterator();
			sum -= it.next();
			it.remove();
		}
	}

	SingleData getAverage(SingleData singleData) {

		return singleData.create(this.sum, this.lastMeasureValues.size());
	}

}
