/************************************************************************
 * 
 * $Id$
 *
 * 
 ************************************************************************/

package de.sgollmer.solvismax.model.objects.data;

import de.sgollmer.solvismax.error.TypeException;
import de.sgollmer.solvismax.helper.Helper.AverageInt;

public class Average implements Cloneable {

	private static class InternalResult {
		private final int value10;
		private final boolean fastChange;

		private InternalResult(final int value10, final boolean fastChange) {
			this.value10 = value10;
			this.fastChange = fastChange;
		}
	}

	private final int measurementHysteresisFactor;
	private InternalResult average = new InternalResult(0, false);
	private int absAverage = 0;
	private int absCount;
	private AverageInt averageInt;

	Average(final int maxCount, final int measurementHysteresisFactor) {
		this.averageInt = new AverageInt(maxCount);
		this.measurementHysteresisFactor = measurementHysteresisFactor;
	}

	Average(final Average average) {
		this.measurementHysteresisFactor = average.measurementHysteresisFactor;
		this.average = average.average;
		this.absAverage = average.absAverage;
		this.absCount = average.absCount;
	}

	synchronized void add(final SingleData<?> data) throws TypeException {
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
				this.average = new InternalResult(newAverage, fastChange);
			}
		} else {
			this.average = new InternalResult(newAverage, fastChange);
			;
		}
	}

	private int getPrecision() {
		return (int) ((this.absAverage + (this.absCount >> 1)) / this.absCount);
	}

	public static class Result {
		private final SingleData<?> data;
		private final boolean fastChange;

		public Result(final SingleData<?> data, final boolean fastChange) {
			this.data = data;
			this.fastChange = fastChange;
		}

		public SingleData<?> getData() {
			return this.data;
		}

		public boolean isFastChange() {
			return this.fastChange;
		}
	}

	Result getAverage(final SingleData<?> singleData) {
		if (!this.averageInt.isFilled()) {
			return null;
		} else {
			InternalResult average = this.average;
			SingleData<?> data = singleData.create(
					average.value10 > 0 ? (average.value10 + 5) / 10 : (average.value10 - 5) / 10,
					singleData.getTimeStamp());
			return new Result(data, average.fastChange);
		}
	}

	void clear() {
		this.averageInt.clear();
	}
}
