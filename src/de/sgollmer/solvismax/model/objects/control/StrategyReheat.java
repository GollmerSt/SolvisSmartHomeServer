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

import de.sgollmer.solvismax.Constants;
import de.sgollmer.solvismax.Constants.Csv;
import de.sgollmer.solvismax.error.AssignmentException;
import de.sgollmer.solvismax.error.TerminationException;
import de.sgollmer.solvismax.error.TypeException;
import de.sgollmer.solvismax.helper.AbortHelper;
import de.sgollmer.solvismax.helper.Helper;
import de.sgollmer.solvismax.log.LogManager;
import de.sgollmer.solvismax.log.LogManager.ILogger;
import de.sgollmer.solvismax.model.Solvis;
import de.sgollmer.solvismax.model.command.CommandControl;
import de.sgollmer.solvismax.model.command.CommandScreenRestore;
import de.sgollmer.solvismax.model.command.CommandSetQueuePriority;
import de.sgollmer.solvismax.model.objects.ChannelDescription;
import de.sgollmer.solvismax.model.objects.IChannelSource.SetResult;
import de.sgollmer.solvismax.model.objects.IChannelSource.UpperLowerStep;
import de.sgollmer.solvismax.model.objects.Observer.IObserver;
import de.sgollmer.solvismax.model.objects.ResultStatus;
import de.sgollmer.solvismax.model.objects.SolvisDescription;
import de.sgollmer.solvismax.model.objects.TouchPoint;
import de.sgollmer.solvismax.model.objects.control.Control.GuiAccess;
import de.sgollmer.solvismax.model.objects.data.BooleanValue;
import de.sgollmer.solvismax.model.objects.data.IMode;
import de.sgollmer.solvismax.model.objects.data.SingleData;
import de.sgollmer.solvismax.model.objects.data.SolvisData;
import de.sgollmer.solvismax.model.objects.data.StringData;
import de.sgollmer.solvismax.model.objects.data.TwoValues;
import de.sgollmer.solvismax.model.objects.screen.Reheat;
import de.sgollmer.solvismax.model.objects.screen.SolvisScreen;
import de.sgollmer.solvismax.objects.Rectangle;
import de.sgollmer.xmllibrary.BaseCreator;
import de.sgollmer.xmllibrary.CreatorByXML;
import de.sgollmer.xmllibrary.XmlException;

public class StrategyReheat implements IStrategy {

	private static final ILogger logger = LogManager.getInstance().getLogger(StrategyReheat.class);

	private static final String XML_TOUCH_POINT = "TouchPoint";

	private final TouchPoint touchPoint;
	private Control control;

	private StrategyReheat(final TouchPoint touchPoint) {
		this.touchPoint = touchPoint;
	}

	@Override
	public void assign(final SolvisDescription description) throws AssignmentException {
		this.touchPoint.assign(description);
	}

	@Override
	public SingleData<?> getValue(final SolvisScreen solvisScreen, final Solvis solvis,
			final IControlAccess controlAccess, final boolean optional) throws TerminationException, IOException {
		if (controlAccess instanceof GuiAccess) {
			Rectangle rectangle = ((GuiAccess) controlAccess).getValueRectangle();

			Reheat reheat = new Reheat(rectangle);

			return new BooleanValue(reheat.isActive(solvisScreen), 0);
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

		Rectangle rectangle = ((GuiAccess) controlAccess).getValueRectangle();
		Reheat reheat = new Reheat(rectangle);

		Boolean first = null;
		Boolean second = null;
		ResultStatus status = null;

		boolean notRequired = Reheat.isNotRequired(solvis.getCurrentScreen());

		if (notRequired) {
			first = true;
			second = false;
			status = ResultStatus.SUCCESS;
			solvis.sendBack();
		} else {

			boolean bool = helperBool.result();

			boolean active = reheat.isActive(solvis.getCurrentScreen());

			if (active && !bool) {
				// TODO hier sollte noch eine Fehlermeldung sein. Rücksetzen nicht möglich
				first = false;
				second = true;
				status = ResultStatus.SUCCESS;
			}

			if (active == bool) {
				first = active;
				status = ResultStatus.SUCCESS;

				if (active) {
					Runnable runnable = new Runnable(solvis, this.control.getDescription());
					runnable.submit();
				}

			}
		}

		if (status != null) {
			long timeStamp = System.currentTimeMillis();
			BooleanValue firstValue = new BooleanValue(first, timeStamp);
			if (second == null) {
				return new SetResult(status, firstValue);
			} else {
				BooleanValue secondValue = new BooleanValue(second, timeStamp);
				return new SetResult(status, new TwoValues(firstValue, secondValue, timeStamp));
			}
		}

		solvis.send(this.touchPoint);

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
	public BooleanValue interpretSetData(final SingleData<?> singleData) throws TypeException {
		if (singleData instanceof BooleanValue) {
			return (BooleanValue) singleData;
		} else if (singleData instanceof StringData) {
			String data = ((StringData) singleData).get();
			if (data.equalsIgnoreCase("true") || data.equalsIgnoreCase("false")) {
				return new BooleanValue(data.equalsIgnoreCase("true"), singleData.getTimeStamp());
			}
		}
		return null;
	}

	@Override
	public boolean isXmlValid() {
		return true;
	}

	static class Creator extends CreatorByXML<StrategyReheat> {

		private TouchPoint touchPoint;

		Creator(final String id, final BaseCreator<?> creator) {
			super(id, creator);
		}

		@Override
		public void setAttribute(final QName name, final String value) {
		}

		@Override
		public StrategyReheat create() throws XmlException, IOException {
			return new StrategyReheat(this.touchPoint);
		}

		@Override
		public CreatorByXML<?> getCreator(final QName name) {
			String id = name.getLocalPart();
			switch (id) {
				case XML_TOUCH_POINT:
					return new TouchPoint.Creator(id, this.getBaseCreator());
			}
			return null;
		}

		@Override
		public void created(final CreatorByXML<?> creator, final Object created) {
			switch (creator.getId()) {
				case XML_TOUCH_POINT:
					this.touchPoint = (TouchPoint) created;
					break;
			}

		}
	}

	@Override
	public boolean isBoolean() {
		return true;
	}

	@Override
	public SingleData<?> createSingleData(final String value, final long timeStamp) {
		return new BooleanValue(Boolean.parseBoolean(value), timeStamp);
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
	public void setControl(Control control) {
		this.control = control;
	}

	private static class Runnable extends Helper.Runnable implements IObserver<SolvisData> {

		private final Solvis solvis;
		private final ChannelDescription description;
		private boolean abort = false;

		public Runnable(final Solvis solvis, final ChannelDescription description) {
			super("WaitForReheatingEnd");
			this.solvis = solvis;
			this.description = description;
		}

		@Override
		public void submit() {

			SolvisData data = this.solvis.getAllSolvisData().get(this.description);

			if (data.getContinuousObserver(this) != null) {
				return;
			}
			
			this.solvis.execute(new CommandScreenRestore(false, this));
			this.solvis.execute(new CommandSetQueuePriority(Constants.Commands.REHEATING_PRIORITY, this));

			super.submit();

		}

		@Override
		public void run() {

			int interval = this.solvis.getUnit().getMeasurementsIntervalFast_ms();

			SolvisData data = this.solvis.getAllSolvisData().get(this.description);

			if (data.getContinuousObserver(this) != null) {
				return;
			}

			data.registerContinuousObserver(this);

			while (!this.abort) {
				synchronized (this) {
					try {
						AbortHelper.getInstance().sleep(interval);
					} catch (TerminationException e) {
						this.abort = true;
					}
				}
				if (!this.abort) {
					this.solvis.execute(
							new CommandControl(this.description, this.solvis, Constants.Commands.REHEATING_PRIORITY));
				}
			}

			this.solvis.execute(new CommandSetQueuePriority(Constants.Commands.REHEATING_PRIORITY, this));
			this.solvis.execute(new CommandScreenRestore(true, this));
			data.unregisterContinuousObserver(this);
		}

		@Override
		public void update(SolvisData data, Object source) {

			boolean active = false;

			try {
				active = data.getBool();
			} catch (TypeException e) {
				logger.error("Reheating error, detection aborted", e);
				active = false;
			}

			if (!active) {
				this.abort = true;
				synchronized (this) {
					this.notifyAll();
				}
			}
		}

	}
}
