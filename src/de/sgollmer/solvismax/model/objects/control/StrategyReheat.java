/************************************************************************
 * 
 * $Id$
 *
 * 
 ************************************************************************/

package de.sgollmer.solvismax.model.objects.control;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
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
import de.sgollmer.solvismax.model.objects.IChannelSource.SetResult;
import de.sgollmer.solvismax.model.objects.IChannelSource.UpperLowerStep;
import de.sgollmer.solvismax.model.objects.Observer.IObserver;
import de.sgollmer.solvismax.model.objects.ResultStatus;
import de.sgollmer.solvismax.model.objects.SolvisDescription;
import de.sgollmer.solvismax.model.objects.TouchPoint;
import de.sgollmer.solvismax.model.objects.control.Control.GuiAccess;
import de.sgollmer.solvismax.model.objects.data.BooleanValue;
import de.sgollmer.solvismax.model.objects.data.IMode;
import de.sgollmer.solvismax.model.objects.data.ModeValue;
import de.sgollmer.solvismax.model.objects.data.SingleData;
import de.sgollmer.solvismax.model.objects.data.SolvisData;
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
	private Collection<Execute> executes = new ArrayList<>(3);

	private StrategyReheat(final TouchPoint touchPoint) {
		this.touchPoint = touchPoint;
	}

	private Execute getExecute(Solvis solvis) {
		for (Execute execute : this.executes) {
			if (execute.solvis == solvis) {
				return execute;
			}
		}
		Execute execute = new Execute(solvis);
		this.executes.add(execute);
		return execute;
	}

	private enum Mode implements IMode<Mode> {
		OFF("off", Handling.READ), //
		HEATING("heating", Handling.BOTH), //
		NOT_REQUIRED("not_required", Handling.READ);

		private final String name;
		private final Handling handling;

		private Mode(final String name, final Handling handling) {
			this.name = name;
			this.handling = handling;
		}

		@Override
		public String getName() {
			return this.name;
		}

		@Override
		public ModeValue<?> create(long timeStamp) {
			return new ModeValue<>(this, timeStamp);
		}

		@Override
		public Handling getHandling() {
			return this.handling;
		}

	}

	@Override
	public void assign(final SolvisDescription description) throws AssignmentException {
		this.touchPoint.assign(description);
	}

	private class Execute {

		private final Solvis solvis;
		private WaitForReheatingFinished waitForReheatingFinished = null;
		private ClearNotRequired clearNotRequired = null;

		public Execute(Solvis solvis) {
			this.solvis = solvis;
		}

		public SingleData<?> getValue(final SolvisScreen solvisScreen, final IControlAccess controlAccess,
				final boolean optional) throws TerminationException, IOException {
			if (controlAccess instanceof GuiAccess) {
				Rectangle rectangle = ((GuiAccess) controlAccess).getValueRectangle();

				Reheat reheat = new Reheat(rectangle);

				boolean active = reheat.isActive(solvisScreen);

				if (!active) {
					SolvisData former = this.solvis.getAllSolvisData()
							.get(StrategyReheat.this.control.getDescription());
					if (former.getMode() != null && former.getMode().get() == Mode.NOT_REQUIRED) {
						return Mode.NOT_REQUIRED.create(0);
					} else {
						return Mode.OFF.create(0);
					}

				} else {
					return Mode.HEATING.create(0);
				}
			}
			return null;

		}

		public SetResult setValue(final IControlAccess controlAccess, final SolvisData value)
				throws IOException, TerminationException, TypeException {

			StrategyReheat.this.interpretSetData(value.getSingleData());

			ClearNotRequired clearNotRequired = this.clearNotRequired;

			if (clearNotRequired != null) {
				clearNotRequired.abort();
				this.clearNotRequired = null;
			}

			Rectangle rectangle = ((GuiAccess) controlAccess).getValueRectangle();
			Reheat reheat = new Reheat(rectangle);

			Mode mode = null;
			ResultStatus status = null;

			boolean notRequired = Reheat.isNotRequired(this.solvis.getCurrentScreen());

			if (notRequired) {
				mode = Mode.NOT_REQUIRED;
				status = ResultStatus.SUCCESS;
				this.solvis.sendBack();
				this.clearNotRequired = new ClearNotRequired();
				this.clearNotRequired.submit();
			} else {

				boolean active = reheat.isActive(this.solvis.getCurrentScreen());

				if (active) {
					mode = Mode.HEATING;

					status = ResultStatus.SUCCESS;

					if (this.waitForReheatingFinished == null) {

						this.waitForReheatingFinished = new WaitForReheatingFinished();
						this.waitForReheatingFinished.submit();
					}

				}
			}

			if (status != null) {

				return new SetResult(status, new ModeValue<>(mode, System.currentTimeMillis()));
			}

			this.solvis.getAllSolvisData().get(StrategyReheat.this.control.getDescription()).setMode(Mode.HEATING,
					System.currentTimeMillis());

			this.solvis.send(StrategyReheat.this.touchPoint);

			return null;
		}

		private class ClearNotRequired extends Helper.Runnable {

			private boolean abort = false;

			public ClearNotRequired() {
				super("ClearNotRequired");
			}

			@Override
			public void run() {

				int waitTime = Execute.this.solvis.getUnit().getClearNotRequiredTime_ms();
				if (waitTime <= 0) {
					return;
				}

				synchronized (this) {
					try {
						AbortHelper.getInstance().sleep(waitTime);
					} catch (TerminationException e) {
						this.abort = true;
					}
				}
				if (!this.abort) {
					SolvisData data = Execute.this.solvis.getAllSolvisData()
							.get(StrategyReheat.this.control.getDescription());
					synchronized (data) {
						ModeValue<?> mode = data.getMode();
						if (mode == null || !(mode.get() instanceof Mode) || mode.get() == Mode.NOT_REQUIRED) {
							data.setMode(Mode.OFF, System.currentTimeMillis());
						}
					}
				}
				Execute.this.clearNotRequired = null;
			}

			public void abort() {
				this.abort = true;
				synchronized (this) {
					this.notifyAll();
				}
			}
		}

		private class WaitForReheatingFinished extends Helper.Runnable implements IObserver<SolvisData> {

			private boolean abort = false;

			public WaitForReheatingFinished() {
				super("WaitForReheatingEnd");
			}

			@Override
			public void run() {

				int interval = Execute.this.solvis.getUnit().getMeasurementsIntervalFast_ms();

				SolvisData data = Execute.this.solvis.getAllSolvisData()
						.get(StrategyReheat.this.control.getDescription());

				Execute.this.solvis.execute(new CommandScreenRestore(false, this));
				Execute.this.solvis.execute(new CommandSetQueuePriority(Constants.Commands.REHEATING_PRIORITY, this));

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
						Execute.this.solvis.execute(new CommandControl(StrategyReheat.this.control.getDescription(),
								Execute.this.solvis, Constants.Commands.REHEATING_PRIORITY));
					}
				}

				Execute.this.solvis.execute(new CommandSetQueuePriority(Constants.Commands.REHEATING_PRIORITY, this));
				Execute.this.solvis.execute(new CommandScreenRestore(true, this));
				data.unregisterContinuousObserver(this);
			}

			@Override
			public void update(SolvisData data, Object source) {

				boolean active = false;

				ModeValue<?> mode = data.getMode();
				active = mode.get() == Mode.HEATING;

				if (!active) {
					this.abort = true;
					StrategyReheat.Execute.this.waitForReheatingFinished = null;
					synchronized (this) {
						this.notifyAll();
					}
				}
			}

		}
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
	public ModeValue<Mode> interpretSetData(final SingleData<?> singleData) throws TypeException {
		return this.interpretSetData(singleData.toString(), singleData.getTimeStamp());
	}

	private ModeValue<Mode> interpretSetData(String value, long timeStamp) throws TypeException {
		if (value.equals(Mode.HEATING.getName())) {
			return new ModeValue<>(Mode.HEATING, timeStamp);
		} else {
			throw new TypeException("Mode <" + value + "> is unknown or not allowed");
		}
	}

	@Override
	public boolean isXmlValid() {
		return true;
	}

	@Override
	public List<Mode> getModes() {
		return Arrays.asList(Mode.values());
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
		return false;
	}

	@Override
	public SingleData<?> createSingleData(final String value, final long timeStamp) {
		return new BooleanValue(Boolean.parseBoolean(value), timeStamp);
	}

	@Override
	public String getCsvMeta(final String column, final boolean semicolon) {
		switch (column) {
			case Csv.WRITE:
				return "true";
			case Csv.MODES:
				StringBuilder builder = new StringBuilder();
				for (Mode entry : Mode.values()) {
					if (builder.length() != 0) {
						builder.append(semicolon ? ',' : ';');
					}
					builder.append(entry.getName());
				}
				return builder.toString();

		}
		return null;
	}

	@Override
	public void setControl(Control control) {
		this.control = control;
	}

	@Override
	public SingleData<?> getValue(SolvisScreen solvisScreen, Solvis solvis, IControlAccess controlAccess,
			boolean optional) throws TerminationException, IOException {

		return this.getExecute(solvis).getValue(solvisScreen, controlAccess, optional);
	}

	@Override
	public SetResult setValue(Solvis solvis, IControlAccess controlAccess, SolvisData value)
			throws IOException, TerminationException, TypeException {

		return this.getExecute(solvis).setValue(controlAccess, value);
	}
}
