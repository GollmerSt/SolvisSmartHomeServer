/************************************************************************
 * 
 * $Id$
 *
 * 
 ************************************************************************/

package de.sgollmer.solvismax.model.objects.data;

import de.sgollmer.solvismax.helper.Helper.AverageInt;

public class Average implements Cloneable {

	public boolean EDGE_DETECTION = false;

	private final int measurementHysteresisFactor;
	private int average = 0;
	private int absAverage = 0;
	private int absCount;
	private AverageInt averageInt;

	public Average(int maxCount, int measurementHysteresisFactor) {
		this.averageInt = new AverageInt(maxCount);
		this.measurementHysteresisFactor = measurementHysteresisFactor;
	}

	public Average(Average average) {
		this.measurementHysteresisFactor = average.measurementHysteresisFactor;
		this.average = average.average;
		this.absAverage = average.absAverage;
		this.absCount = average.absCount;
	}

	public void add(SingleData<?> data) {
		Integer value = data.getInt();
		if (value == null) {
			return;
		}

		value *= 10;

		if (this.averageInt.size() > 0) {
			int last = this.averageInt.getLast();
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
						this.averageInt.put(value);
					}
				}
			}
		}

		this.averageInt.put(value);

		int size = this.averageInt.size();

		int newAverage = this.averageInt.get();
		int delta = Math.abs(newAverage - this.average);

		if (size > 1) {

			if (delta > 10 && delta > (this.measurementHysteresisFactor * this.getPrecision() + size - 1) / size) {
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
		if (!this.averageInt.isFilled()) {
			return null;
		} else {
			return singleData.create(average > 0 ? (average + 5) / 10 : (average - 5) / 10);
		}
	}

	public void clear() {
		this.averageInt.clear();
	}
}
