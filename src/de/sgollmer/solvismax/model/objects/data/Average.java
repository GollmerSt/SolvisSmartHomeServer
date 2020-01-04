/************************************************************************
 * 
 * $Id: 73_SolvisClient.pm 78 2020-01-03 17:50:08Z stefa $
 *
 * 
 ************************************************************************/

package de.sgollmer.solvismax.model.objects.data;

import java.util.Arrays;

public class Average implements Cloneable {

	public boolean EDGE_DETECTION = false;

	private final int maxCount;
	private int average;
	private int maxDelta = 0;
	private long sum = 0;
	private int size = 0;
	private int next = 0;
	private final int[] lastMeasureValues;

	public Average(int maxCount) {
		this.maxCount = maxCount;
		this.lastMeasureValues = new int[maxCount];
	}

	public Average(Average average) {
		this.maxCount = average.maxCount;
		this.lastMeasureValues = Arrays.copyOf(average.lastMeasureValues, average.maxCount);
		this.size = average.size;
	}

	public void add(SingleData<?> data) {
		Integer i = data.getInt();
		if (i == null) {
			return;
		}

		int ix = next - 1 < 0 ? size - 1 : next - 1;
		if (ix >= 0) {
			int last = lastMeasureValues[next - 1 < 0 ? size - 1 : next - 1];
			int delta = last - i;
			delta = delta < 0 ? -delta : delta;
			if (delta > this.maxDelta) {
				this.maxDelta = delta;
			}
			delta = this.average - i;
			delta = delta < 0 ? -delta : delta;
			for ( int cnt = 0 ; delta > this.maxDelta && cnt < 4 ; ++cnt ) {
				this.put(i);
				delta -= this.maxDelta ;
			}
		}

		this.put(i);

		int newAverage = (int) ((2 * this.sum + this.size) / (2 * this.size));
		
		int delta = newAverage - this.average;
		delta = delta < 0 ? -delta : delta;
		
		if ( delta > 1 && delta > 2 * this.maxDelta / this.size ) {
			average = newAverage ;
		}

	}

	SingleData<?> getAverage(SingleData<?> singleData) {

		return singleData.create(average);
	}

	private void put(int value) {
		int first = lastMeasureValues[next];
		this.sum -= first;
		this.sum += value;
		lastMeasureValues[next] = value;
		++next;
		if (next >= this.maxCount) {
			next = 0;
		}
		this.size = this.size + 1 > this.maxCount ? this.size : size + 1;

	}

}
