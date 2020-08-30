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
import de.sgollmer.solvismax.error.ModbusException;
import de.sgollmer.solvismax.error.TerminationException;
import de.sgollmer.solvismax.error.TypeException;
import de.sgollmer.solvismax.error.XmlException;
import de.sgollmer.solvismax.model.Solvis;
import de.sgollmer.solvismax.model.objects.IAssigner;
import de.sgollmer.solvismax.model.objects.IChannelSource.SetResult;
import de.sgollmer.solvismax.model.objects.IChannelSource.Status;
import de.sgollmer.solvismax.model.objects.IChannelSource.UpperLowerStep;
import de.sgollmer.solvismax.model.objects.SolvisDescription;
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
	private final GuiModification guiModification;

	private StrategyReadWrite(int increment, int divisor, int least, int most, GuiModification guiModification) {
		super(divisor, guiModification);
		this.increment = increment;
		this.least = least;
		this.most = most;
		this.guiModification = guiModification;
	}

	@Override
	public boolean isWriteable() {
		return true;
	}

	@Override
	public SetResult setValue(Solvis solvis, IControlAccess controlAccess, SolvisData setValue)
			throws IOException, TerminationException, ModbusException, TypeException {
		if (controlAccess instanceof GuiAccess) {
			Integer target = setValue.getInteger();
			IntegerValue data = this.getValue(solvis.getCurrentScreen(), solvis, controlAccess, false);
			if (data == null) {
				return null;
			} else if (data.get() == null) {
				return new SetResult(Status.NO_SUCCESS, data);
			}
			int current = data.get();

			int goal = Math.max(target, this.least);
			goal = Math.min(goal, this.most);

			int value = (2 * this.increment * goal + (goal > 0 ? this.increment : -this.increment))
					/ (2 * this.increment);

			if (current == value) {
				return new SetResult(target == current ? Status.SUCCESS : Status.VALUE_VIOLATION, data);
			}

			int[] dist = new int[3];

			int minDist = value - current;
			int maxDist = this.most - this.least + this.increment;

			dist[0] = minDist; // no wrap around

			dist[1] = minDist + maxDist; // wrap upper
			dist[2] = minDist - maxDist; // wrap lower
			if (this.guiModification.wrapAround) {
				for (int i = 1; i < dist.length; ++i) {
					if (Math.abs(minDist) > Math.abs(dist[i])) {
						minDist = dist[i];
					}
				}
			}
			TouchPoint point;
			if (minDist < 0) {
				point = this.guiModification.lower;
			} else {
				point = this.guiModification.upper;
			}

			int touches = Math.abs(minDist) / this.increment;
			boolean interrupt = false;

			if (touches > Constants.INTERRUPT_AFTER_N_TOUCHES) {
				touches = Constants.INTERRUPT_AFTER_N_TOUCHES;
				interrupt = true;
			}

			for (int c = 0; c < touches; ++c) {
				solvis.send(point);
			}
			if (interrupt) {
				return new SetResult(Status.INTERRUPTED, data);
			}
		}
		return null;
	}

	static class Creator extends CreatorByXML<StrategyReadWrite> {

		private int divisor = 1;
		private int increment;
		private int least;
		private int most;
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
			}

		}

		@Override
		public StrategyReadWrite create() throws XmlException {
			return new StrategyReadWrite(this.increment, this.divisor, this.least, this.most, this.guiModification);
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
		return new UpperLowerStep((float) this.most / this.getDivisor(), (float) this.least / this.getDivisor(),
				(float) this.increment / this.getDivisor());
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
