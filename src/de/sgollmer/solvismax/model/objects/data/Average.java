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
	private final int measurementHysteresisFactor ;
	private int average = 0;
	private int absAverage = 0;
	private int absCount;
	private long sum;
	private int size;
	private int lastIdx;
	private final int[] lastMeasureValues;

	public Average(int maxCount, int measurementHysteresisFactor) {
		this.maxCount = maxCount;
		this.measurementHysteresisFactor = measurementHysteresisFactor ;
		this.lastMeasureValues = new int[maxCount];
		this.clear();
	}

	public Average(Average average) {
		this.maxCount = average.maxCount;
		this.measurementHysteresisFactor = average.measurementHysteresisFactor ;
		this.lastMeasureValues = Arrays.copyOf(average.lastMeasureValues, average.maxCount);
		this.average = average.average;
		this.sum = average.sum;
		this.size = average.size;
		this.lastIdx = average.lastIdx;
		this.absAverage = average.absAverage;
		this.absCount = average.absCount;
	}

	public void add(SingleData<?> data) {
		Integer value = data.getInt();
		if (value == null) {
			return;
		}

		value *= 10;

		if (this.size > 0) {
			int last = this.getLast();
			int delta = Math.abs(last - value);
			this.absAverage += 2 * delta; // Annahme: Messfehler sind statistisch gleichmäßig verteilt
			++this.absCount;
			int precision = getPrecision();
			if (precision > 0) {
				int deltaLast = Math.abs(this.average - last);
				int deltaCurrent = Math.abs(this.average - value);
				if (deltaLast > precision && deltaCurrent > precision) {
					int cnt = deltaCurrent / precision;
					while (--cnt >= 0) {
						this.put(value);
					}
				}
			}
		}

		this.put(value);

		int newAverage = (int) ((2 * this.sum + this.size) / (2 * this.size));
		int delta = Math.abs(newAverage - this.average);

		if (this.size > 1) {

			if (delta > 10
					&& delta > (this.measurementHysteresisFactor * this.getPrecision() + this.size - 1) / this.size) {
				average = newAverage;
			}
		} else {
			this.average = newAverage;
		}
	}

	private int getPrecision() {
		return (int) ((this.absAverage + (this.absCount >> 1)) / this.absCount);
	}

	SingleData<?> getAverage(SingleData<?> singleData) {
		if (this.maxCount > this.size) {
			return null;
		} else {
			return singleData.create(average > 0 ? (average + 5) / 10 : (average - 5) / 10);
		}
	}

	private void put(int value) {
		int next = this.lastIdx + 1;
		if (next >= this.maxCount) {
			next = 0;
		}
		if (this.size == this.maxCount) {
			int first = lastMeasureValues[next];
			this.sum -= first;
		} else {
			++this.size;
		}
		this.sum += value;
		lastMeasureValues[next] = value;
		this.lastIdx = next;

	}

	private int getLast() {
		return this.lastMeasureValues[lastIdx];
	}

	public void clear() {
		this.sum = 0;
		this.size = 0;
		this.lastIdx = -1;
	}

}
