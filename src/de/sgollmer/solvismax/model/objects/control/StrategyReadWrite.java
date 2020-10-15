/************************************************************************
 * 
 * $Id$
 *
 * 
 ************************************************************************/

package de.sgollmer.solvismax.model.objects.control;

import java.io.IOException;

import javax.xml.namespace.QName;

import de.sgollmer.solvismax.Constants;
import de.sgollmer.solvismax.error.AssignmentException;
import de.sgollmer.solvismax.error.TerminationException;
import de.sgollmer.solvismax.error.TypeException;
import de.sgollmer.solvismax.error.XmlException;
import de.sgollmer.solvismax.model.Solvis;
import de.sgollmer.solvismax.model.objects.IAssigner;
import de.sgollmer.solvismax.model.objects.IChannelSource.SetResult;
import de.sgollmer.solvismax.model.objects.IChannelSource.UpperLowerStep;
import de.sgollmer.solvismax.model.objects.SolvisDescription;
import de.sgollmer.solvismax.model.objects.ResultStatus;
import de.sgollmer.solvismax.model.objects.TouchPoint;
import de.sgollmer.solvismax.model.objects.control.Control.GuiAccess;
import de.sgollmer.solvismax.model.objects.data.DoubleValue;
import de.sgollmer.solvismax.model.objects.data.IntegerValue;
import de.sgollmer.solvismax.model.objects.data.SingleData;
import de.sgollmer.solvismax.model.objects.data.SolvisData;
import de.sgollmer.solvismax.model.objects.data.StringData;
import de.sgollmer.solvismax.xml.BaseCreator;
import de.sgollmer.solvismax.xml.CreatorByXML;

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

	private StrategyReadWrite(int increment, int divisor, int least, int most, Integer incrementChange,
			Integer changedIncrement, Integer maxExceeding, GuiModification guiModification) {
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
	public SetResult setValue(Solvis solvis, IControlAccess controlAccess, SolvisData setValue)
			throws IOException, TerminationException, TypeException {
		if (controlAccess instanceof GuiAccess) {
			Integer target = setValue.getInteger();
			IntegerValue data = this.getValue(solvis.getCurrentScreen(), solvis, controlAccess, false);
			if (data == null) {
				return null;
			} else if (data.get() == null) {
				return new SetResult(ResultStatus.NO_SUCCESS, data);
			}
			int current = data.get();

			int goal = Math.max(target, this.least);
			goal = Math.min(goal, this.most);

			int value;

			if (this.incrementChange != null && goal >= this.incrementChange) {
				goal -= this.incrementChange;
				value = (2 * this.changedIncrement * goal + (goal > 0 ? this.changedIncrement : -this.changedIncrement))
						/ (2 * this.changedIncrement);
				value += this.incrementChange;
			} else {
				value = (2 * this.increment * goal + (goal > 0 ? this.increment : -this.increment))
						/ (2 * this.increment);
			}

			if (current == value) {
				return new SetResult(target == current ? ResultStatus.SUCCESS : ResultStatus.VALUE_VIOLATION, data);
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

			if (touches > Constants.INTERRUPT_AFTER_N_TOUCHES) {
				touches = Constants.INTERRUPT_AFTER_N_TOUCHES;
				interrupt = true;
			}

			for (int c = 0; c < touches; ++c) {
				solvis.send(point);
			}
			if (interrupt) {
				return new SetResult(ResultStatus.INTERRUPTED, data);
			}
		}
		return null;
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

	private int steps(int value, boolean upper) {
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
		public void setAttribute(QName name, String value) {
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
		public CreatorByXML<?> getCreator(QName name) {
			String id = name.getLocalPart();
			switch (id) {
				case XML_GUI_MODIFICATION:
					return new GuiModification.Creator(id, this.getBaseCreator());
			}
			return null;
		}

		@Override
		public void created(CreatorByXML<?> creator, Object created) {
			switch (creator.getId()) {
				case XML_GUI_MODIFICATION:
					this.guiModification = (GuiModification) created;
					break;
			}

		}

	}

	@Override
	public void assign(SolvisDescription description) throws AssignmentException {
		if (this.guiModification != null) {
			this.guiModification.assign(description);
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

	@Override
	public SingleData<?> interpretSetData(SingleData<?> singleData) throws TypeException {
		if (singleData instanceof StringData) {
			String string = (String) singleData.get();
			try {
				if (string.contains(".")) {
					return new DoubleValue(Double.parseDouble(string), 0);
				} else {
					return new IntegerValue(Integer.parseInt(string), 0);
				}
			} catch (NumberFormatException e) {
				throw new TypeException(e);
			}
		}
		return singleData;
	}

	private static class GuiModification extends GuiRead implements IAssigner {
		private final boolean wrapAround;
		private final TouchPoint upper;
		private final TouchPoint lower;

		private GuiModification(String format, boolean wrapAround, TouchPoint upper, TouchPoint lower) {
			super(format);
			this.wrapAround = wrapAround;
			this.upper = upper;
			this.lower = lower;
		}

		@Override
		public void assign(SolvisDescription description) throws AssignmentException {
			if (this.upper != null) {
				this.upper.assign(description);
			}
			if (this.lower != null) {
				this.lower.assign(description);
			}
		}

		private static class Creator extends CreatorByXML<GuiModification> {

			private String format;
			private boolean wrapAround = false;
			private TouchPoint upper;
			private TouchPoint lower;

			private Creator(String id, BaseCreator<?> creator) {
				super(id, creator);
			}

			@Override
			public void setAttribute(QName name, String value) {
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
			public CreatorByXML<?> getCreator(QName name) {
				String id = name.getLocalPart();
				switch (id) {
					case XML_UPPER:
					case XML_LOWER:
						return new TouchPoint.Creator(id, this.getBaseCreator());
				}
				return null;
			}

			@Override
			public void created(CreatorByXML<?> creator, Object created) {
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
	public SingleData<?> createSingleData(String value) throws TypeException {
		try {
			return new IntegerValue(Integer.parseInt(value), -1);
		} catch (NumberFormatException e) {
			throw new TypeException(e);
		}
	}
}
