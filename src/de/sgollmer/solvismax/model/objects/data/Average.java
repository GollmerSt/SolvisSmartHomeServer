/************************************************************************
 * 
 * $Id$
 *
 * 
 ************************************************************************/

package de.sgollmer.solvismax.model.objects.data;

import java.util.Arrays;

public class Average implements Cloneable {

	public boolean EDGE_DETECTION = false;

	private final int maxCount;
	private int average = 0;
	private int maxDelta = 0;
	private long sum;
	private int size;
	private int next;
	private final int[] lastMeasureValues;

	public Average(int maxCount) {
		this.maxCount = maxCount;
		this.lastMeasureValues = new int[maxCount];
		this.clear();
	}

	public Average(Average average) {
		this.maxCount = average.maxCount;
		this.lastMeasureValues = Arrays.copyOf(average.lastMeasureValues, average.maxCount);
		this.average = average.average;
		this.maxDelta = average.maxDelta;
		this.sum = average.sum;
		this.size = average.size;
		this.next = average.next;
	}

	public void add(SingleData<?> data) {
		Integer value = data.getInt();
		if (value == null) {
			return;
		}

		if (this.size > 0) {
			int last = this.getLast();
			int delta = Math.abs(last - value);
			delta = delta < 0 ? -delta : delta;
			if (delta > this.maxDelta) {
				this.maxDelta = delta;
			}
			delta = this.average - value;
			delta = delta < 0 ? -delta : delta;
			for (int cnt = 0; delta > this.maxDelta && cnt < 4; ++cnt) {
				this.put(value);
				delta -= this.maxDelta;
			}
		}

		this.put(value);

		int newAverage = (int) ((2 * this.sum + this.size) / (2 * this.size));

		if (this.size > 1) {
			int delta = Math.abs(newAverage - this.average);

			if (delta > 1 && delta > (2 * this.maxDelta + this.size - 1) / this.size) {
				average = newAverage;
			}
		} else {
			this.average = newAverage;
		}
	}

	SingleData<?> getAverage(SingleData<?> singleData) {
		if (this.maxCount > this.size) {
			return null;
		} else {
			return singleData.create(average);
		}
	}

	private void put(int value) {
		if (this.size == this.maxCount) {
			int first = lastMeasureValues[next];
			this.sum -= first;
		} else {
			++this.size;
		}
		this.sum += value;
		lastMeasureValues[next] = value;
		++next;
		if (next >= this.maxCount) {
			next = 0;
		}

	}

	private int getLast() {
		return this.lastMeasureValues[next - 1 < 0 ? size - 1 : next - 1];
	}

	public void clear() {
		this.sum = 0;
		this.size = 0;
		this.next = 0;
	}

}
