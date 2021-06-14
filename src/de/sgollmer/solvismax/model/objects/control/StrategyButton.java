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

import de.sgollmer.solvismax.Constants.Csv;
import de.sgollmer.solvismax.error.AssignmentException;
import de.sgollmer.solvismax.error.TerminationException;
import de.sgollmer.solvismax.error.TypeException;
import de.sgollmer.solvismax.helper.Helper;
import de.sgollmer.solvismax.helper.SolvisDataHelper;
import de.sgollmer.solvismax.model.Solvis;
import de.sgollmer.solvismax.model.objects.Duration;
import de.sgollmer.solvismax.model.objects.IChannelSource.SetResult;
import de.sgollmer.solvismax.model.objects.IChannelSource.UpperLowerStep;
import de.sgollmer.solvismax.model.objects.ResultStatus;
import de.sgollmer.solvismax.model.objects.SolvisDescription;
import de.sgollmer.solvismax.model.objects.control.Control.GuiAccess;
import de.sgollmer.solvismax.model.objects.data.BooleanValue;
import de.sgollmer.solvismax.model.objects.data.IMode;
import de.sgollmer.solvismax.model.objects.data.SingleData;
import de.sgollmer.solvismax.model.objects.data.SolvisData;
import de.sgollmer.solvismax.model.objects.screen.SolvisScreen;
import de.sgollmer.solvismax.objects.Rectangle;
import de.sgollmer.xmllibrary.BaseCreator;
import de.sgollmer.xmllibrary.CreatorByXML;
import de.sgollmer.xmllibrary.XmlException;

public class StrategyButton implements IStrategy {

	private final boolean invert;
	private final String pushTimeId;
	private final String releaseTimeId;

	private StrategyButton(final boolean invert, final String pushTimeId, final String releaseTimeId) {
		this.invert = invert;
		this.pushTimeId = pushTimeId;
		this.releaseTimeId = releaseTimeId;
	}

	@Override
	public void assign(final SolvisDescription description) throws AssignmentException {
		Duration pushTimeDuration = description.getDuration(this.pushTimeId);
		Duration releaseTimeDuration = description.getDuration(this.releaseTimeId);

		if (pushTimeDuration == null || releaseTimeDuration == null) {
			throw new AssignmentException("Duration time not found");
		}
	}

	@Override
	public SingleData<?> getValue(final SolvisScreen solvisScreen, final Solvis solvis,
			final IControlAccess controlAccess, final boolean optional) throws TerminationException, IOException {
		if (controlAccess instanceof GuiAccess) {
			Rectangle rectangle = ((GuiAccess) controlAccess).getValueRectangle();
			Button button = new Button(solvisScreen.getImage(), rectangle, this.pushTimeId, this.releaseTimeId);
			return new BooleanValue(button.isSelected() ^ this.invert, System.currentTimeMillis());
		}
		return null;
	}

	@Override
	public SetResult setValue(final Solvis solvis, final IControlAccess controlAccess, final SolvisData value)
			throws IOException, TerminationException, TypeException {
		Helper.Boolean helperBool = value.getBoolean();
		if (helperBool == Helper.Boolean.UNDEFINED) {
			throw new TypeException("Wrong value type, should be boolean");
		}

		boolean bool = helperBool.result();

		Button button = new Button(solvis.getCurrentScreen().getImage(),
				((GuiAccess) controlAccess).getValueRectangle(), this.pushTimeId, this.releaseTimeId);
		boolean cmp = button.isSelected() ^ this.invert;
		if (cmp == bool) {
			return new SetResult(ResultStatus.SUCCESS, new BooleanValue(cmp, System.currentTimeMillis()), true);
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
	public UpperLowerStep getUpperLowerStep() {
		return null;
	}

	@Override
	public boolean mustBeLearned() {
		return false;
	}

	@Override
	public boolean learn(final Solvis solvis, final IControlAccess controlAccess) throws IOException {
		return true;
	}

	@Override
	public BooleanValue interpretSetData(final SingleData<?> singleData, final boolean debug) throws TypeException {
		return SolvisDataHelper.toBoolean(singleData);
	}

	@Override
	public boolean isXmlValid() {
		return true;
	}

	static class Creator extends CreatorByXML<StrategyButton> {

		private boolean invert = false;
		private String pushTimeId;
		private String releaseTimeId;

		Creator(final String id, final BaseCreator<?> creator) {
			super(id, creator);
		}

		@Override
		public void setAttribute(final QName name, final String value) {
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
		public CreatorByXML<?> getCreator(final QName name) {
			return null;
		}

		@Override
		public void created(final CreatorByXML<?> creator, final Object created) {

		}

	}

	@Override
	public boolean isBoolean() {
		return true;
	}

	@Override
	public List<? extends IMode<?>> getModes() {
		return null;
	}

	@Override
	public String getCsvMeta(final String column, final boolean semicolon) {
		switch (column) {
			case Csv.WRITE:
				return "true";
		}
		return null;
	}

	@Override
	public void setControl(final Control control) {

	}

	@Override
	public SetResult setValueFast(final Solvis solvis, final SolvisData value) {
		return null;
	}

	@Override
	public SetResult setDebugValue(Solvis solvis, SingleData<?> value) {
		return new SetResult(ResultStatus.SUCCESS, value, false);
	}

	@Override
	public void instantiate(Solvis solvis) {
		// TODO Auto-generated method stub

	}

	@Override
	public boolean inhibitGuiReadAfterWrite() {
		return false;
	}

}
