package de.sgollmer.solvismax.model.objects.control;

import javax.xml.namespace.QName;

import de.sgollmer.solvismax.error.TypeError;
import de.sgollmer.solvismax.error.XmlError;
import de.sgollmer.solvismax.model.Solvis;
import de.sgollmer.solvismax.model.objects.SolvisDescription;
import de.sgollmer.solvismax.model.objects.TouchPoint;
import de.sgollmer.solvismax.model.objects.data.IntegerValue;
import de.sgollmer.solvismax.model.objects.data.SingleData;
import de.sgollmer.solvismax.model.objects.data.SolvisData;
import de.sgollmer.solvismax.objects.Rectangle;
import de.sgollmer.solvismax.xml.CreatorByXML;
import de.sgollmer.solvismax.xml.BaseCreator;

public class StrategyValue extends StrategyRead {
	private final int increment;
	private final int least;
	private final int most;
	private final boolean wrapAround;
	private final TouchPoint upper;
	private final TouchPoint lower;

	public StrategyValue(int increment, String format, int divisor, String unit, int least, int most,
			boolean wrapAround, TouchPoint upper, TouchPoint lower) {
		super(format, divisor, unit);
		this.increment = increment;
		this.least = least;
		this.most = most;
		this.wrapAround = wrapAround;
		this.upper = upper;
		this.lower = lower;
	}

	@Override
	public boolean isWriteable() {
		return true;
	}

	@Override
	public Boolean setValue(Solvis solvis, Rectangle rectangle, SolvisData setValue) {
		Integer goal = setValue.getInteger();

		SingleData data = this.getValue(solvis.getCurrentImage(), rectangle);
		if (data == null) {
			return null;
		}
		if ((data instanceof IntegerValue)) {
			throw new TypeError("TypeError: Type actual: <" + data.getClass() + ">, target: <IntegerValue>");
		}
		int current = ((IntegerValue) data).getData();
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

	public static class Creator extends CreatorByXML<StrategyValue> {

		private String format;
		private int divisor = 1;
		private String unit;
		private int increment;
		private int least;
		private int most;
		private boolean wrapAround = false ;
		private TouchPoint upper;
		private TouchPoint lower;

		public Creator(String id, BaseCreator<?> creator) {
			super(id, creator);
		}

		@Override
		public void setAttribute(QName name, String value) {
			switch (name.getLocalPart()) {
				case "format":
					this.format = value;
					break;
				case "unit":
					this.unit = value;
					break;
				case "divisor":
					this.divisor = Integer.parseInt(value);
					break;
				case "increment":
					this.increment = Integer.parseInt(value);
					break;
				case "least":
					this.least = Integer.parseInt(value);
					break;
				case "most":
					this.most = Integer.parseInt(value);
					break;
			}

		}

		@Override
		public StrategyValue create() throws XmlError {
			return new StrategyValue(increment, format, divisor, unit, least, most, wrapAround, upper, lower);
		}

		@Override
		public CreatorByXML<?> getCreator(QName name) {
			String id = name.getLocalPart();
			switch (id) {
				case "Upper":
				case "Lower":
					return new TouchPoint.Creator(id, this.getBaseCreator());
			}
			return null;
		}

		@Override
		public void created(CreatorByXML<?> creator, Object created) {
			switch (creator.getId()) {
				case "Upper":
					this.upper = (TouchPoint) created;
					break;
				case "Lower":
					this.lower = (TouchPoint) created;
					break;
			}
			// TODO Auto-generated method stub

		}

	}

	@Override
	public void assign(SolvisDescription description) {
		this.upper.assign(description);
		this.lower.assign(description);
	}

}
