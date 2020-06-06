package de.sgollmer.solvismax.model.objects.control;

import java.io.IOException;
import java.util.List;

import javax.xml.namespace.QName;

import de.sgollmer.solvismax.error.AssignmentError;
import de.sgollmer.solvismax.error.TerminationException;
import de.sgollmer.solvismax.error.TypeError;
import de.sgollmer.solvismax.error.XmlError;
import de.sgollmer.solvismax.modbus.ModbusAccess;
import de.sgollmer.solvismax.model.Solvis;
import de.sgollmer.solvismax.model.objects.ChannelSourceI.UpperLowerStep;
import de.sgollmer.solvismax.model.objects.Duration;
import de.sgollmer.solvismax.model.objects.SolvisDescription;
import de.sgollmer.solvismax.model.objects.control.Control.GuiAccess;
import de.sgollmer.solvismax.model.objects.data.BooleanValue;
import de.sgollmer.solvismax.model.objects.data.ModeI;
import de.sgollmer.solvismax.model.objects.data.SingleData;
import de.sgollmer.solvismax.model.objects.data.SolvisData;
import de.sgollmer.solvismax.model.objects.screen.SolvisScreen;
import de.sgollmer.solvismax.objects.Rectangle;
import de.sgollmer.solvismax.xml.BaseCreator;
import de.sgollmer.solvismax.xml.CreatorByXML;

public class StrategyButton implements Strategy {

	private final boolean invert;
	private final String pushTimeId;
	private final String releaseTimeId;

	private Integer pushTime = null;
	private Integer releaseTime = null;

	public StrategyButton(boolean invert, String pushTimeId, String releaseTimeId) {
		this.invert = invert;
		this.pushTimeId = pushTimeId;
		this.releaseTimeId = releaseTimeId;
	}

	@Override
	public void assign(SolvisDescription description) {
		Duration pushTimeDuration = description.getDurations().get(this.pushTimeId);
		Duration releaseTimeDuration = description.getDurations().get(this.releaseTimeId);

		if (pushTimeDuration == null || releaseTimeDuration == null) {
			throw new AssignmentError("Duration time not found");
		}
		this.pushTime = pushTimeDuration.getTime_ms();
		this.releaseTime = releaseTimeDuration.getTime_ms();
	}

	@Override
	public SingleData<?> getValue(SolvisScreen solvisScreen, Solvis solvis, ControlAccess controlAccess,
			boolean optional) throws TerminationException, IOException {
		if (controlAccess instanceof GuiAccess) {
			Rectangle rectangle = ((GuiAccess) controlAccess).getValueRectangle();
			Button button = new Button(solvisScreen.getImage(), rectangle, this.pushTime, this.releaseTime);
			return new BooleanValue(button.isSelected() ^ this.invert, System.currentTimeMillis());
		} else {
			int num = solvis.readUnsignedShortModbusData((ModbusAccess) controlAccess);
			return new BooleanValue(num > 0, System.currentTimeMillis());
		}
	}

	@Override
	public SingleData<?> setValue(Solvis solvis, ControlAccess controlAccess, SolvisData value)
			throws IOException, TerminationException, TypeError {
		Boolean bool = value.getBoolean();
		if (bool == null) {
			throw new TypeError("Wrong value type");
		}
		if (controlAccess.isModbus()) {
			solvis.writeUnsignedShortModbusData((ModbusAccess) controlAccess, bool ? 1 : 0);
		} else {

			Button button = new Button(solvis.getCurrentScreen().getImage(),
					((GuiAccess) controlAccess).getValueRectangle(), this.pushTime, this.releaseTime);
			boolean cmp = button.isSelected() ^ this.invert;
			if (cmp == bool) {
				return new BooleanValue(cmp, System.currentTimeMillis());
			}
			button.set(solvis, bool);
		}

		return null;
	}

	@Override
	public boolean isWriteable() {
		return true;
	}

	@Override
	public Integer getDivisor() {
		return null;
	}

	@Override
	public Float getAccuracy() {
		return null;
	}

	@Override
	public List<? extends ModeI> getModes() {
		return null;
	}

	@Override
	public UpperLowerStep getUpperLowerStep() {
		return null;
	}

	@Override
	public void setCurrentRectangle(Rectangle rectangle) {

	}

	@Override
	public boolean mustBeLearned() {
		return false;
	}

	@Override
	public boolean learn(Solvis solvis, ControlAccess controlAccess) throws IOException {
		return true;
	}

	@Override
	public BooleanValue interpretSetData(SingleData<?> singleData) throws TypeError {
		if (singleData instanceof BooleanValue) {
			return (BooleanValue) singleData;
		} else {
			return null;
		}
	}

	@Override
	public boolean isXmlValid(boolean modbus) {
		return true;
	}

	public static class Creator extends CreatorByXML<StrategyButton> {

		private boolean invert = false;
		private String pushTimeId;
		private String releaseTimeId;

		public Creator(String id, BaseCreator<?> creator) {
			super(id, creator);
		}

		@Override
		public void setAttribute(QName name, String value) {
			switch (name.getLocalPart()) {
				case "invert":
					this.invert = Boolean.parseBoolean(value);
					break;
				case "pushTimeId":
					this.pushTimeId = value;
					break;
				case "releaseTimeId":
					this.releaseTimeId = value;
					break;
			}

		}

		@Override
		public StrategyButton create() throws XmlError, IOException {
			return new StrategyButton(this.invert, this.pushTimeId, this.releaseTimeId);
		}

		@Override
		public CreatorByXML<?> getCreator(QName name) {
			return null;
		}

		@Override
		public void created(CreatorByXML<?> creator, Object created) {

		}

	}

	@Override
	public boolean isBoolean() {
		return true;
	}

}
