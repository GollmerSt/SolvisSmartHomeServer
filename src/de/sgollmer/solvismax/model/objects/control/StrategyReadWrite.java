/************************************************************************
 * 
 * $Id$
 *
 * 
 ************************************************************************/

package de.sgollmer.solvismax.model.objects.control;

import java.io.IOException;

import javax.xml.namespace.QName;

import de.sgollmer.solvismax.Constants.Csv;
import de.sgollmer.solvismax.error.TerminationException;
import de.sgollmer.solvismax.error.TypeException;
import de.sgollmer.solvismax.model.Solvis;
import de.sgollmer.solvismax.model.objects.IChannelSource.SetResult;
import de.sgollmer.solvismax.model.objects.IChannelSource.UpperLowerStep;
import de.sgollmer.solvismax.model.objects.ResultStatus;
import de.sgollmer.solvismax.model.objects.TouchPoint;
import de.sgollmer.solvismax.model.objects.data.IntegerValue;
import de.sgollmer.solvismax.model.objects.data.SingleData;
import de.sgollmer.solvismax.model.objects.data.SolvisData;
import de.sgollmer.xmllibrary.BaseCreator;
import de.sgollmer.xmllibrary.CreatorByXML;
import de.sgollmer.xmllibrary.XmlException;

public class StrategyReadWrite extends StrategyRead {

	private static final String XML_GUI_MODIFICATION = "GuiModification";
	private static final String XML_UPPER = "Upper";
	private static final String XML_LOWER = "Lower";

	private final int increment;
	private final int least;
	private final int most;
	private final Integer incrementChange;
	private final Integer changedIncrement;
	private final Integer maxExceeding;
	private final GuiModification guiModification;

	private StrategyReadWrite(final int increment, final int divisor, final int least, final int most,
			final Integer incrementChange, final Integer changedIncrement, final Integer maxExceeding,
			final GuiModification guiModification) {
		super(divisor, guiModification);
		this.increment = increment;
		this.least = least;
		this.most = most;
		this.incrementChange = incrementChange;
		this.changedIncrement = changedIncrement;
		this.maxExceeding = maxExceeding;
		this.guiModification = guiModification;
	}

	@Override
	public boolean isWriteable() {
		return true;
	}

	@Override
	public SetResult setValue(final Solvis solvis, final SolvisData setValue)
			throws IOException, TerminationException, TypeException {

		Integer target = setValue.getInteger();
		IntegerValue data = this.getValue(solvis.getCurrentScreen(), solvis, false);
		if (data == null) {
			return null;
		} else if (data.get() == null) {
			return new SetResult(ResultStatus.NO_SUCCESS, data, true);
		}
		int current = data.get();

		int value = getRealValue(target);

		if (current == value) {
			return new SetResult(target == current ? ResultStatus.SUCCESS : ResultStatus.VALUE_VIOLATION, data, true);
		}

		int[] dist = new int[3];

		dist[0] = this.steps(value, false) - this.steps(current, false); // no wrap around

		dist[1] = -this.steps(current, false) + this.steps(value, true); // wrap lower
		dist[2] = this.steps(value, false) - this.steps(current, true); // wrap upper

		int minDist = dist[0];

		if (this.guiModification.wrapAround && //
				(this.maxExceeding == null //
						|| this.most - current <= this.maxExceeding //
						|| this.most - value <= this.maxExceeding //
				)) {
			for (int i = 1; i < dist.length; ++i) {
				if (Math.abs(minDist) > Math.abs(dist[i])) {
					minDist = dist[i];
				}
			}
		}

		TouchPoint point;
		int touches;
		if (minDist < 0) {
			point = this.guiModification.lower;
			touches = -minDist;
		} else {
			point = this.guiModification.upper;
			touches = minDist;
		}

		boolean interrupt = false;

		if (touches > de.sgollmer.solvismax.Constants.Solvis.INTERRUPT_AFTER_N_TOUCHES) {
			touches = de.sgollmer.solvismax.Constants.Solvis.INTERRUPT_AFTER_N_TOUCHES;
			interrupt = true;
		}

		for (int c = 0; c < touches; ++c) {
			solvis.send(point);
		}
		if (interrupt) {
			return new SetResult(ResultStatus.INTERRUPTED, data, false);
		}
		return null;
	}

	private int getRealValue(final int target) {
		int goal = Math.max(target, this.least);
		goal = Math.min(goal, this.most);

		int value;

		if (this.incrementChange != null && goal >= this.incrementChange) {
			goal -= this.incrementChange;
			value = (2 * goal + this.changedIncrement) / (2 * this.changedIncrement) * this.changedIncrement;
			value += this.incrementChange;
		} else {
			goal -= this.least;
			value = (2 * goal + this.increment) / (2 * this.increment) * this.increment;
			value += this.least;
		}
		return value;

	}

	/**
	 * Calculation of the number of steps to need to go FROM the given limit
	 * 
	 * @param value Reach value
	 * @param upper True: Go FROM upper limit, false FROM lower
	 * 
	 * @return Number of steps, positiv increments to reach the value from the
	 *         limits
	 */

	private int steps(final int value, final boolean upper) {
		int result;

		if (this.incrementChange == null) {
			int limit = upper ? this.most + this.increment : this.least;
			result = (value - limit) / this.increment;
		} else {
			if (upper) {
				int limit = this.most + this.changedIncrement;

				if (value >= this.incrementChange) {
					result = (value - limit) / this.changedIncrement;
				} else {
					result = (this.incrementChange - limit) / this.changedIncrement;
					result += (value - this.incrementChange) / this.increment;
				}
			} else {
				int limit = this.least;
				if (value <= this.incrementChange) {
					result = (value - limit) / this.increment;
				} else {
					result = (this.incrementChange - limit) / this.increment;
					result += (value - this.incrementChange) / this.changedIncrement;
				}
			}
		}

		return result;
	}

	static class Creator extends CreatorByXML<StrategyReadWrite> {

		private int divisor = 1;
		private int increment;
		private int least;
		private int most;
		private Integer incrementChange = null;
		private Integer changedIncrement = null;
		private Integer maxExceeding = null;
		private GuiModification guiModification = null;

		Creator(String id, BaseCreator<?> creator) {
			super(id, creator);
		}

		@Override
		public void setAttribute(final QName name, final String value) {
			switch (name.getLocalPart()) {
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
				case "incrementChange":
					this.incrementChange = Integer.parseInt(value);
					break;
				case "changedIncrement":
					this.changedIncrement = Integer.parseInt(value);
					break;
				case "maxExceeding":
					this.maxExceeding = Integer.parseInt(value);
					break;
			}

		}

		@Override
		public StrategyReadWrite create() throws XmlException {
			return new StrategyReadWrite(this.increment, this.divisor, this.least, this.most, this.incrementChange,
					this.changedIncrement, this.maxExceeding, this.guiModification);
		}

		@Override
		public CreatorByXML<?> getCreator(final QName name) {
			String id = name.getLocalPart();
			switch (id) {
				case XML_GUI_MODIFICATION:
					return new GuiModification.Creator(id, this.getBaseCreator());
			}
			return null;
		}

		@Override
		public void created(final CreatorByXML<?> creator, final Object created) {
			switch (creator.getId()) {
				case XML_GUI_MODIFICATION:
					this.guiModification = (GuiModification) created;
					break;
			}

		}

	}

	@Override
	public UpperLowerStep getUpperLowerStep() {
		Double incrementChange = null;
		Double changedIncrement = null;

		if (this.incrementChange != null && this.changedIncrement != null) {
			incrementChange = (double) this.incrementChange / this.getDivisor();
			changedIncrement = (double) this.changedIncrement / this.getDivisor();
		}

		return new UpperLowerStep( //
				(double) this.most / this.getDivisor(), //
				(double) this.least / this.getDivisor(), //
				(double) this.increment / this.getDivisor(), //
				incrementChange, //
				changedIncrement//
		);
	}

	private static class GuiModification extends GuiRead {
		private final boolean wrapAround;
		private final TouchPoint upper;
		private final TouchPoint lower;

		private GuiModification(final String format, final boolean wrapAround, final TouchPoint upper,
				final TouchPoint lower) {
			super(format);
			this.wrapAround = wrapAround;
			this.upper = upper;
			this.lower = lower;
		}

		private static class Creator extends CreatorByXML<GuiModification> {

			private String format;
			private boolean wrapAround = false;
			private TouchPoint upper;
			private TouchPoint lower;

			private Creator(final String id, final BaseCreator<?> creator) {
				super(id, creator);
			}

			@Override
			public void setAttribute(final QName name, final String value) {
				switch (name.getLocalPart()) {
					case "format":
						this.format = value;
						break;
					case "wrapAround":
						this.wrapAround = Boolean.parseBoolean(value);
				}
			}

			@Override
			public GuiModification create() throws XmlException, IOException {
				return new GuiModification(this.format, this.wrapAround, this.upper, this.lower);
			}

			@Override
			public CreatorByXML<?> getCreator(final QName name) {
				String id = name.getLocalPart();
				switch (id) {
					case XML_UPPER:
					case XML_LOWER:
						return new TouchPoint.Creator(id, this.getBaseCreator());
				}
				return null;
			}

			@Override
			public void created(final CreatorByXML<?> creator, final Object created) {
				switch (creator.getId()) {
					case XML_UPPER:
						this.upper = (TouchPoint) created;
						break;
					case XML_LOWER:
						this.lower = (TouchPoint) created;
						break;
				}
			}

		}
	}

	@Override
	public boolean isXmlValid() {
		return this.guiModification != null;
	}

	@Override
	public String getCsvMeta(final String column, final boolean semicolon) {

		String csv = null;
		switch (column) {
			case Csv.INCREMENT:
				csv = Integer.toString(this.increment);
				break;
			case Csv.LEAST:
				csv = Integer.toString(this.least);
				break;
			case Csv.MOST:
				csv = Integer.toString(this.most);
				break;
			case Csv.INCREMENT_CHANGE:
				if (this.incrementChange != null) {
					csv = Integer.toString(this.incrementChange);
				}
				break;
			case Csv.CHANGED_INCREMENT:
				if (this.changedIncrement != null) {
					csv = Integer.toString(this.changedIncrement);
				}
				break;
			case Csv.WRITE:
				csv = "true";
		}
		if (csv == null) {
			return super.getCsvMeta(column, semicolon);
		} else {
			return csv;
		}
	}

	@Override
	public SetResult setDebugValue(Solvis solvis, SingleData<?> value) throws TypeException {

		Integer target = value.getInt();

		if (target == null) {
			throw new TypeException("Wrong type of debug value");
		}

		return new SetResult(ResultStatus.SUCCESS, new IntegerValue(this.getRealValue(target), -1L), false);
	}
}
