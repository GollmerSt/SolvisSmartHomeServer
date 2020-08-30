/************************************************************************
 * 
 * $Id$
 *
 * 
 ************************************************************************/

package de.sgollmer.solvismax.model.objects.control;

import java.io.IOException;
import java.util.List;

import javax.xml.namespace.QName;

import de.sgollmer.solvismax.error.AssignmentException;
import de.sgollmer.solvismax.error.ModbusException;
import de.sgollmer.solvismax.error.TerminationException;
import de.sgollmer.solvismax.error.TypeException;
import de.sgollmer.solvismax.error.XmlException;
import de.sgollmer.solvismax.model.Solvis;
import de.sgollmer.solvismax.model.objects.Duration;
import de.sgollmer.solvismax.model.objects.IChannelSource.SetResult;
import de.sgollmer.solvismax.model.objects.IChannelSource.Status;
import de.sgollmer.solvismax.model.objects.IChannelSource.UpperLowerStep;
import de.sgollmer.solvismax.model.objects.SolvisDescription;
import de.sgollmer.solvismax.model.objects.control.Control.GuiAccess;
import de.sgollmer.solvismax.model.objects.data.BooleanValue;
import de.sgollmer.solvismax.model.objects.data.IMode;
import de.sgollmer.solvismax.model.objects.data.SingleData;
import de.sgollmer.solvismax.model.objects.data.SolvisData;
import de.sgollmer.solvismax.model.objects.data.StringData;
import de.sgollmer.solvismax.model.objects.screen.SolvisScreen;
import de.sgollmer.solvismax.objects.Rectangle;
import de.sgollmer.solvismax.xml.BaseCreator;
import de.sgollmer.solvismax.xml.CreatorByXML;

public class StrategyButton implements IStrategy {

	private final boolean invert;
	private final String pushTimeId;
	private final String releaseTimeId;

	private Integer pushTime = null;
	private Integer releaseTime = null;

	private StrategyButton(boolean invert, String pushTimeId, String releaseTimeId) {
		this.invert = invert;
		this.pushTimeId = pushTimeId;
		this.releaseTimeId = releaseTimeId;
	}

	@Override
	public void assign(SolvisDescription description) throws AssignmentException {
		Duration pushTimeDuration = description.getDuration(this.pushTimeId);
		Duration releaseTimeDuration = description.getDuration(this.releaseTimeId);

		if (pushTimeDuration == null || releaseTimeDuration == null) {
			throw new AssignmentException("Duration time not found");
		}
		this.pushTime = pushTimeDuration.getTime_ms();
		this.releaseTime = releaseTimeDuration.getTime_ms();
	}

	@Override
	public SingleData<?> getValue(SolvisScreen solvisScreen, Solvis solvis, IControlAccess controlAccess,
			boolean optional) throws TerminationException, IOException, ModbusException {
		if (controlAccess instanceof GuiAccess) {
			Rectangle rectangle = ((GuiAccess) controlAccess).getValueRectangle();
			Button button = new Button(solvisScreen.getImage(), rectangle, this.pushTime, this.releaseTime);
			return new BooleanValue(button.isSelected() ^ this.invert, System.currentTimeMillis());
		}
		return null;
	}

	@Override
	public SetResult setValue(Solvis solvis, IControlAccess controlAccess, SolvisData value)
			throws IOException, TerminationException, TypeException, ModbusException {
		Boolean bool = value.getBoolean();
		if (bool == null) {
			throw new TypeException("Wrong value type");
		}

		Button button = new Button(solvis.getCurrentScreen().getImage(),
				((GuiAccess) controlAccess).getValueRectangle(), this.pushTime, this.releaseTime);
		boolean cmp = button.isSelected() ^ this.invert;
		if (cmp == bool) {
			return new SetResult(Status.SUCCESS, new BooleanValue(cmp, System.currentTimeMillis()));
		}
		button.set(solvis, bool);

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
	public Double getAccuracy() {
		return null;
	}

	@Override
	public List<? extends IMode> getModes() {
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
	public boolean learn(Solvis solvis, IControlAccess controlAccess) throws IOException {
		return true;
	}

	@Override
	public BooleanValue interpretSetData(SingleData<?> singleData) throws TypeException {
		if (singleData instanceof BooleanValue) {
			return (BooleanValue) singleData;
		} else if (singleData instanceof StringData) {
			String data = ((StringData) singleData).get();
			if (data.equalsIgnoreCase("true") || data.equalsIgnoreCase("false")) {
				return new BooleanValue(data.equalsIgnoreCase("true"), -1);
			}
		}
		return null;
	}

	@Override
	public boolean isXmlValid() {
		return true;
	}

	static class Creator extends CreatorByXML<StrategyButton> {

		private boolean invert = false;
		private String pushTimeId;
		private String releaseTimeId;

		Creator(String id, BaseCreator<?> creator) {
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
		public StrategyButton create() throws XmlException, IOException {
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

	@Override
	public SingleData<?> createSingleData(String value) {
		return new BooleanValue(Boolean.parseBoolean(value), -1);
	}

}
