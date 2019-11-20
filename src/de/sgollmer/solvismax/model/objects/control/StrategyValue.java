package de.sgollmer.solvismax.model.objects.control;

import de.sgollmer.solvismax.error.TypeError;
import de.sgollmer.solvismax.model.Solvis;
import de.sgollmer.solvismax.model.objects.TouchPoint;
import de.sgollmer.solvismax.model.objects.data.IntegerValue;
import de.sgollmer.solvismax.model.objects.data.SingleData;
import de.sgollmer.solvismax.model.objects.data.SolvisData;
import de.sgollmer.solvismax.objects.Rectangle;

public class StrategyValue extends StrategyRead {
	private final int increment;
	private final int least;
	private final int most;
	private final boolean wrapAround;
	private final TouchPoint upper;
	private final TouchPoint lower;

	public StrategyValue(int increment, String format, int divisor, String unit, int least, int most,
			boolean warpAround, TouchPoint upper, TouchPoint lower) {
		super(format, divisor, unit);
		this.increment = increment;
		this.least = least;
		this.most = most;
		this.wrapAround = warpAround;
		this.upper = upper;
		this.lower = lower;
	}

	@Override
	public boolean isWriteable() {
		return true;
	}

	@Override
	public Boolean setValue(Solvis solvis, Rectangle rectangle, SolvisData setValue) {
		Integer goal = setValue.getInteger() ;

		SingleData data = this.getValue( solvis.getCurrentImage(), rectangle);
		if ( data == null ) {
			return null;
		}
		if ( ( data instanceof IntegerValue ) ) {
			throw new TypeError("TypeError: Type actual: <" + data.getClass() + ">, target: <IntegerValue>");
		}
		int current = ((IntegerValue)data).getData() ;
		int value = (2 * this.increment * (goal + this.increment)) / (2 * this.increment);

		if (current == value) {
			return true;
		}

		int[] dist = new int[3];

		dist[0] = value - current; // no wrap around
		dist[1] = value + this.most - current - this.least + this.increment; // wrap
																				// upper
		dist[2] = value - this.most + this.least - current + this.increment; // wrap
																				// lower
		int minDist = dist[0];

		if (this.wrapAround) {
			for (int i = 1; i < dist.length; ++i) {
				if (Math.abs(minDist) > dist[i]) {
					minDist = dist[i];
				}
			}
		}
		TouchPoint point;
		if (minDist < 0) {
			point = this.lower;
		} else {
			point = this.upper;
		}

		for (int c = 0; c < Math.abs(minDist) / this.increment; ++c) {
			solvis.send(point);
		}

		return false;
	}

}
