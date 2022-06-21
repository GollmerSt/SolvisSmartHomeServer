/************************************************************************
 * 
 * $Id$
 *
 * 
 ************************************************************************/

package de.sgollmer.solvismax.model.objects.control;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;

import javax.xml.namespace.QName;

import de.sgollmer.solvismax.Constants;
import de.sgollmer.solvismax.Constants.Csv;
import de.sgollmer.solvismax.error.TerminationException;
import de.sgollmer.solvismax.error.TypeException;
import de.sgollmer.solvismax.helper.AbortHelper;
import de.sgollmer.solvismax.helper.AbortHelper.Abortable;
import de.sgollmer.solvismax.helper.Helper;
import de.sgollmer.solvismax.log.LogManager;
import de.sgollmer.solvismax.log.LogManager.ILogger;
import de.sgollmer.solvismax.model.Solvis;
import de.sgollmer.solvismax.model.command.CommandControl;
import de.sgollmer.solvismax.model.command.CommandObserver;
import de.sgollmer.solvismax.model.command.CommandScreenRestore;
import de.sgollmer.solvismax.model.command.CommandSetQueuePriority;
import de.sgollmer.solvismax.model.objects.IChannelSource.SetResult;
import de.sgollmer.solvismax.model.objects.IChannelSource.UpperLowerStep;
import de.sgollmer.solvismax.model.objects.Observer.IObserver;
import de.sgollmer.solvismax.model.objects.ResultStatus;
import de.sgollmer.solvismax.model.objects.TouchPoint;
import de.sgollmer.solvismax.model.objects.control.Control.GuiAccess;
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

public class StrategyReheat extends AbstractStrategy {

	private static final ILogger logger = LogManager.getInstance().getLogger(StrategyReheat.class);

	private static final String XML_TOUCH_POINT = "TouchPoint";

	private final TouchPoint touchPoint;
	private final String desiredId;
	private final String pufferId;
	private final String deltaId;

	private StrategyReheat(final TouchPoint touchPoint, final String desiredId, final String pufferId, String deltaId) {
		this.touchPoint = touchPoint;
		this.desiredId = desiredId;
		this.pufferId = pufferId;
		this.deltaId = deltaId;
	}

	private enum Mode implements IMode<Mode> {
		OFF("off", Handling.RO), //
		HEATING("heating", Handling.RW), //
		NOT_REQUIRED("not_required", Handling.RO);

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
		public ModeValue<?> create(final long timeStamp) {
			return new ModeValue<>(this, timeStamp);
		}

		@Override
		public Handling getHandling() {
			return this.handling;
		}

		@Override
		public String getCvsMeta() {
			return this.name + this.handling.getCvsMeta();
		}

	}

	private SolvisData getData(final String id, final Solvis solvis) {
		SolvisData data = solvis.getAllSolvisData().get(id);
		if (data == null) {
			logger.error("Warning: Channel <" + id + "> is not known.");
		}
		return data;
	}

	private SolvisData getDesired(final Solvis solvis) {
		return this.getData(this.desiredId, solvis);
	}

	private SolvisData getPuffer(Solvis solvis) {
		return this.getData(this.pufferId, solvis);
	}

	private SolvisData getDelta(Solvis solvis) {
		return this.getData(this.deltaId, solvis);
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
	public boolean learn(final Solvis solvis) throws IOException {
		return true;
	}

	@Override
	public ModeValue<Mode> interpretSetData(final SingleData<?> singleData, final boolean debug) throws TypeException {
		return this.interpretSetData(singleData.toString(), singleData.getTimeStamp(), debug);
	}

	private ModeValue<Mode> interpretSetData(final String value, final long timeStamp, final boolean debug)
			throws TypeException {
		Mode mode = null;
		if (value.equals(Mode.HEATING.getName())) {
			mode = Mode.HEATING;
		} else if (debug) {
			mode = (Mode) this.getControl().getDescription().getMode(value);
		}
		if (mode != null) {
			return new ModeValue<>(mode, timeStamp);
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
		private String desiredId;
		private String pufferId;
		private String deltaId;

		Creator(final String id, final BaseCreator<?> creator) {
			super(id, creator);
		}

		@Override
		public void setAttribute(final QName name, final String value) {
			switch (name.getLocalPart()) {
				case "desiredId":
					this.desiredId = value;
					break;
				case "pufferId":
					this.pufferId = value;
					break;
				case "deltaId":
					this.deltaId = value;
					break;
			}
		}

		@Override
		public StrategyReheat create() throws XmlException, IOException {
			return new StrategyReheat(this.touchPoint, this.desiredId, this.pufferId, this.deltaId);
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
	public String getCsvMeta(final String column, final boolean semicolon) {
		switch (column) {
			case Csv.WRITE:
				return "true";
			case Csv.MODES:
				StringBuilder builder = new StringBuilder();
				for (Mode entry : Mode.values()) {
					if (builder.length() != 0) {
						builder.append('|');
					}
					builder.append(entry.getCvsMeta());
				}
				return builder.toString();

		}
		return null;
	}

	@Override
	public SingleData<?> getValue(final SolvisScreen solvisScreen, final Solvis solvis, final boolean optional)
			throws TerminationException, IOException {

		GuiAccess access = this.getControl().getGuiAccess();
		Rectangle rectangle = access.getValueRectangle();

		Reheat reheat = new Reheat(rectangle);

		boolean active = reheat.isActive(solvisScreen);

		if (!active) {
			return Mode.OFF.create(System.currentTimeMillis());
		} else {
			return Mode.HEATING.create(System.currentTimeMillis());
		}
	}

	@Override
	public SetResult setValue(final Solvis solvis, final SolvisData value)
			throws IOException, TerminationException, TypeException {

		StrategyReheat.this.interpretSetData(value.getSingleData(), false);

		SetResult setResult = setValueFast(solvis, value);

		if (setResult != null) {
			return setResult;
		}

		GuiAccess access = this.getControl().getGuiAccess();
		Rectangle rectangle = access.getValueRectangle();
		Reheat reheat = new Reheat(rectangle);

		boolean notRequired = Reheat.isNotRequired(solvis.getCurrentScreen());

		if (notRequired) {
			solvis.sendBack();
			return new SetResult(ResultStatus.SUCCESS, new ModeValue<>(Mode.NOT_REQUIRED, System.currentTimeMillis()),
					true);
		} else {
			if (reheat.isActive(solvis.getCurrentScreen())) {

				return new SetResult(ResultStatus.SUCCESS, new ModeValue<>(Mode.HEATING, System.currentTimeMillis()),
						true);
			}
		}

		solvis.send(StrategyReheat.this.touchPoint);

		return null;
	}

	@Override
	public SetResult setValueFast(final Solvis solvis, final SolvisData value)
			throws IOException, TerminationException {

		boolean notRequired = false;

		SolvisData desired = this.getDesired(solvis);
		SolvisData puffer = this.getPuffer(solvis);
		SolvisData delta = this.getDelta(solvis);

		if (desired != null && desired.isValid() && puffer != null && puffer.isValid() && delta != null
				&& delta.isValid()) {
			int desiredDivisor = desired.getDescription().getDivisor();
			int pufferDivisor = puffer.getDescription().getDivisor();
			int deltaDivisor = delta.getDescription().getDivisor();

			try {
				int desiredNormized = desired.getInt() * pufferDivisor * deltaDivisor;
				int pufferNormized = puffer.getInt() * desiredDivisor * deltaDivisor;
				int deltaNormized = delta.getInt() * desiredDivisor * pufferDivisor;
				if (desiredNormized + deltaNormized < pufferNormized) {
					notRequired = true;
				}
			} catch (TypeException e) {
				logger.error("Warning: Definition of reheating not correct. Check the xml file. Error ignored.");
			}
		}

		if (notRequired) {
			return new SetResult(ResultStatus.SUCCESS, new ModeValue<>(Mode.NOT_REQUIRED, System.currentTimeMillis()),
					true);
		} else {

			return null;
		}

	}

	@Override
	public SetResult setDebugValue(final Solvis solvis, final SingleData<?> value) throws TypeException {
		return new SetResult(ResultStatus.SUCCESS, value, false);
	}

	@Override
	public void instantiate(Solvis solvis) {

		SolvisData data = solvis.getAllSolvisData().get(this.getControl().getDescription());

		data.registerContinuousObserver(new ReheatObserver());

	}

	private class ReheatObserver implements IObserver<SolvisData> {

		private ReheatThread reheatThread = null;
		private Mode former = null;
		private boolean monitoring = false;

		@Override
		public void update(final SolvisData data, final Object source) {

			Solvis solvis = data.getSolvis();

			Mode current = (Mode) data.getSingleData().get();

			boolean changed = this.former == null || !this.former.equals(current) || current == Mode.NOT_REQUIRED;

			this.former = current;

			switch (current) {
				case OFF:
					if (changed) {
						synchronized (this) {
							if (this.reheatThread != null) {
								this.reheatThread.abort();
								this.reheatThread = null;
							}
							this.setMonitoring(solvis, false);
						}
					}
					break;
				case HEATING:
					synchronized (this) {
						if (this.reheatThread != null) {
							this.reheatThread.abort();
						}
						this.setMonitoring(solvis, true);
						this.reheatThread = new ReheatThread(null, solvis);
						this.reheatThread.submit();
					}
					break;
				case NOT_REQUIRED:
					synchronized (this) {

						if (this.reheatThread != null) {
							this.reheatThread.abort();
						}
						this.setMonitoring(solvis, false);
						this.reheatThread = new ReheatThread(data, solvis);
						this.reheatThread.submit();

					}
					break;
			}
		}

		private void setMonitoring(Solvis solvis, boolean monitoring) {
			boolean changed = this.monitoring != monitoring;
			this.monitoring = monitoring;

			if (changed) {
				solvis.updateByMonitoringTask(this.monitoring ? CommandObserver.Status.MONITORING_STARTED
						: CommandObserver.Status.MONITORING_FINISHED, this);
				solvis.execute(new CommandScreenRestore(!this.monitoring, this));
				solvis.execute(new CommandSetQueuePriority(
						this.monitoring ? Constants.Commands.REHEATING_PRIORITY : null, this));
			}

		}

	}

	private class ReheatThread extends Helper.Runnable implements Abortable {

		private final SolvisData data;
		private final Solvis solvis;
		private boolean abort = false;

		/**
		 * 
		 * @param data   null: look for heating finished otherwise rest not_required
		 *               after a specified time
		 * @param solvis
		 */
		protected ReheatThread(final SolvisData data, final Solvis solvis) {
			super("ReheatThread");
			this.data = data;
			this.solvis = solvis;
		}

		@Override
		public void run() {
			int waitTime = this.data == null ? this.solvis.getUnit().getMeasurementsIntervalFast_ms()
					: this.solvis.getUnit().getReheatingNotRequiredActiveTime_ms();

			try {
				AbortHelper.getInstance().sleepAndLock(waitTime, this);
			} catch (TerminationException e) {
				this.abort = true;
			}
			if (!this.abort) {
				if (this.data == null) {
					this.solvis.execute(new CommandControl(StrategyReheat.this.getControl().getDescription(),
							this.solvis, Constants.Commands.REHEATING_PRIORITY));
				} else {
					try {
						this.data.setMode(Mode.OFF, System.currentTimeMillis());
					} catch (TypeException e) {
						logger.error("Type exception of " + this.data.getName() + ", ignored");
					}
				}
			}
		}

		@Override
		public synchronized void abort() {
			this.abort = true;
			this.notifyAll();
		}

	}

	@Override
	public boolean inhibitGuiReadAfterWrite() {
		return true;
	}

}
