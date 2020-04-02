/************************************************************************
 * 
 * $Id$
 *
 * 
 ************************************************************************/

package de.sgollmer.solvismax.model.objects.data;

import de.sgollmer.solvismax.helper.Helper.AverageInt;

public class Average implements Cloneable {

	private static class Result {
		private final int value10;
		private final boolean fastChange;

		public Result(int value10, boolean fastChange) {
			this.value10 = value10;
			this.fastChange = fastChange;
		}
	}

	public boolean EDGE_DETECTION = false;

	private final int measurementHysteresisFactor;
	private Result average = new Result(0, false);
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

	public synchronized void add(SingleData<?> data) {
		Integer value = data.getInt();
		if (value == null) {
			return;
		}

		value *= 10;
		boolean fastChange = false;

		if (this.averageInt.size() > 0) {
			int last = this.averageInt.getLast();
			int delta = Math.abs(last - value);
			this.absAverage += 2 * delta; // Annahme: Messfehler sind statistisch gleichmäßig verteilt
			++this.absCount;
			int precision = getPrecision();
			if (precision > 0) {
				int deltaLast = Math.abs(this.average.value10 - last);
				int deltaCurrent = Math.abs(this.average.value10 - value);
				if (deltaLast > precision && deltaCurrent > precision) {
					int cnt = deltaCurrent / precision;
					fastChange = true;
					while (--cnt >= 0) {
						this.averageInt.put(value);
					}
				}
			}
		}

		this.averageInt.put(value);

		int size = this.averageInt.size();

		int newAverage = this.averageInt.get();
		int delta = Math.abs(newAverage - this.average.value10);

		if (size > 1) {

			if (delta > 10 && delta > (this.measurementHysteresisFactor * this.getPrecision() + size - 1) / size) {
				this.average = new Result(newAverage, fastChange);
			}
		} else {
			this.average = new Result(newAverage, fastChange);
			;
		}
	}

	private int getPrecision() {
		return (int) ((this.absAverage + (this.absCount >> 1)) / this.absCount);
	}

	SingleData<?> getAverage(SingleData<?> singleData) {
		if (!this.averageInt.isFilled()) {
			return null;
		} else {
			Result average = this.average;
			return singleData.create(average.value10 > 0 ? (average.value10 + 5) / 10 : (average.value10 - 5) / 10,
					singleData.getTimeStamp(), average.fastChange);
		}
	}

	public void clear() {
		this.averageInt.clear();
	}
}
