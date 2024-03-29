/************************************************************************
 * 
 * $Id$
 *
 * 
 ************************************************************************/

package de.sgollmer.solvismax.model.objects.clock;

import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;

import javax.xml.namespace.QName;

import de.sgollmer.solvismax.Constants;
import de.sgollmer.solvismax.error.HelperException;
import de.sgollmer.solvismax.error.LearningException;
import de.sgollmer.solvismax.error.SolvisErrorException;
import de.sgollmer.solvismax.error.TerminationException;
import de.sgollmer.solvismax.error.TypeException;
import de.sgollmer.solvismax.helper.AbortHelper;
import de.sgollmer.solvismax.helper.Helper;
import de.sgollmer.solvismax.helper.Helper.AverageInt;
import de.sgollmer.solvismax.imagepatternrecognition.image.MyImage;
import de.sgollmer.solvismax.imagepatternrecognition.ocr.OcrRectangle;
import de.sgollmer.solvismax.log.LogManager;
import de.sgollmer.solvismax.log.LogManager.ILogger;
import de.sgollmer.solvismax.log.LogManager.Level;
import de.sgollmer.solvismax.model.Solvis;
import de.sgollmer.solvismax.model.objects.AllSolvisData;
import de.sgollmer.solvismax.model.objects.Observer.IObserver;
import de.sgollmer.solvismax.model.objects.SolvisDescription;
import de.sgollmer.solvismax.model.objects.TouchPoint;
import de.sgollmer.solvismax.model.objects.configuration.OfConfigs;
import de.sgollmer.solvismax.model.objects.data.DateValue;
import de.sgollmer.solvismax.model.objects.data.SolvisData;
import de.sgollmer.solvismax.model.objects.screen.AbstractScreen;
import de.sgollmer.solvismax.model.objects.screen.IGraficsLearnable;
import de.sgollmer.solvismax.model.objects.screen.Screen;
import de.sgollmer.solvismax.model.objects.screen.ScreenGraficDescription;
import de.sgollmer.solvismax.model.objects.screen.SolvisScreen;
import de.sgollmer.solvismax.objects.Rectangle;
import de.sgollmer.xmllibrary.BaseCreator;
import de.sgollmer.xmllibrary.CreatorByXML;
import de.sgollmer.xmllibrary.IXmlElement;
import de.sgollmer.xmllibrary.XmlException;

public class ClockMonitor implements IGraficsLearnable, IXmlElement<SolvisDescription> {

	private static final ILogger logger = LogManager.getInstance().getLogger(ClockMonitor.class);
	private static final Level LEARN = Level.getLevel("LEARN");
	private static final Calendar CALENDAR_2018;

	private static final String XML_YEAR = "Year";
	private static final String XML_MONTH = "Month";
	private static final String XML_DAY = "Day";
	private static final String XML_HOUR = "Hour";
	private static final String XML_MINUTE = "Minute";
	private static final String XML_UPPER = "Upper";
	private static final String XML_LOWER = "Lower";
	private static final String XML_OK = "Ok";
	private static final String XML_DISABLE_CLOCK_SETTING = "DisableClockSetting";

	private static final SimpleDateFormat DATE_FORMAT = new SimpleDateFormat("HH:mm:ss");

	private final String timeChannelId;
	private final String screenId;
	private final String okScreenId;
	private final List<DatePart> dateParts;
	private final TouchPoint upper;
	private final TouchPoint lower;
	private final TouchPoint ok;
	private final DisableClockSetting disableClockSetting;

	private OfConfigs<AbstractScreen> screen = null;
	private OfConfigs<AbstractScreen> okScreen = null;
	private boolean initialized = false;

	static {
		CALENDAR_2018 = Calendar.getInstance();
		CALENDAR_2018.set(2018, 0, 1, 0, 0, 0);
		CALENDAR_2018.set(Calendar.MILLISECOND, 0);
	}

	private ClockMonitor(final String timeChannelId, final String screenId, final String okScreenId,
			final List<DatePart> dateParts, final TouchPoint upper, final TouchPoint lower, final TouchPoint ok,
			final DisableClockSetting disableClockSetting) {
		this.timeChannelId = timeChannelId;
		this.screenId = screenId;
		this.okScreenId = okScreenId;
		this.dateParts = dateParts;
		this.upper = upper;
		this.lower = lower;
		this.ok = ok;
		this.disableClockSetting = disableClockSetting;
	}

	@Override
	public void postProcess(final SolvisDescription description) throws XmlException {
		this.screen = description.getScreens().getScreen(this.screenId);
		this.okScreen = description.getScreens().getScreen(this.okScreenId);
		this.initialized = true;
	}

	@Override
	public boolean isInitialisationFinished() {
		return this.initialized;
	}

	static class NextAdjust {
		private final long solvisAdjustTime;
		private final long realAdjustTime;
		private final long startAdjustTime;

		private NextAdjust(final long solvisAdjustTime, final long realAdjustTime, final long startAdjustTime) {
			this.solvisAdjustTime = solvisAdjustTime;
			this.realAdjustTime = realAdjustTime;
			this.startAdjustTime = startAdjustTime;
		}

		private NextAdjust(final int fineAdjusts, final long startAdjustTime) {
			this.solvisAdjustTime = -1;
			this.realAdjustTime = -1;
			this.startAdjustTime = startAdjustTime;
		}

		private long getStartAdjustTime() {
			return this.startAdjustTime;
		}

		@Override
		public String toString() {
			if (this.realAdjustTime > 0) {
				return "Next time adjust is scheduled to " + DATE_FORMAT.format(new Date(this.realAdjustTime))
						+ ", adjustment starts at " + DATE_FORMAT.format(new Date(this.startAdjustTime))
						+ ", Solvis is set to " + DATE_FORMAT.format(new Date(this.solvisAdjustTime)) + ".";
			} else {
				return "Next adjustment starts at " + DATE_FORMAT.format(new Date(this.startAdjustTime)) + ".";
			}
		}
	}

	interface IAdjustStrategy {
		boolean execute(NextAdjust nextAdjust) throws IOException, TerminationException, SolvisErrorException;

		void notExecuted();
	}

	private NextAdjust calculateNextAdjustTime(final DateValue dateValue, final Solvis solvis) {
		Calendar solvisTimeCalendar = dateValue.get();
		long solvisTime = solvisTimeCalendar.getTimeInMillis();
		int singleSettingTime = Math.max(this.upper.getSettingTime(solvis), this.lower.getSettingTime(solvis))
				+ solvis.getMaxResponseTime();
		long diffSolvis = solvisTime - dateValue.getTimeStamp();
		int adjDiffSec = (int) ((diffSolvis % 60000 + 60000) % 60000);
		adjDiffSec = adjDiffSec > 30000 ? adjDiffSec - 60000 : adjDiffSec;
		long current = System.currentTimeMillis();
		long realAdjustTime = this.calculateNextAdjustmentTime(current, Constants.TIME_ADJUSTMENT_MS_N)
				+ Constants.SETTING_TIME_RANGE_LOWER - adjDiffSec;
		long startAdjustTime = realAdjustTime;
		long lastStartAdjustTime = 0;
		Calendar calendarProposal = Calendar.getInstance();
		Calendar calendarSolvisAdjust = Calendar.getInstance();
		Calendar calendarSolvisStart = Calendar.getInstance();
		long solvisAdjustTime = 0;
		int adjustmentTime = 0;
		for (int rep = 0; lastStartAdjustTime != startAdjustTime && rep < 10; ++rep) {
			lastStartAdjustTime = startAdjustTime;
			adjustmentTime = 0;
			calendarProposal.setTimeInMillis(startAdjustTime);
			calendarSolvisStart.setTimeInMillis(startAdjustTime + diffSolvis);
			solvisAdjustTime = realAdjustTime + adjDiffSec;
			calendarSolvisAdjust.setTimeInMillis(solvisAdjustTime);
			for (DatePart part : this.dateParts) {
				int calendarInt = part.getCalendarInt();
				int diff = Math.abs(calendarSolvisStart.get(calendarInt) - calendarProposal.get(calendarInt));
				adjustmentTime += diff * singleSettingTime + part.touch.getSettingTime(solvis);
			}
			adjustmentTime = adjustmentTime + this.ok.getSettingTime(solvis) + solvis.getMaxResponseTime()
					+ 8 * this.screen.get(solvis).getSelectScreenStrategy().getSettingTime(solvis);
			adjustmentTime = adjustmentTime * Constants.TIME_ADJUSTMENT_PROPOSAL_FACTOR_PERCENT / 100;

			startAdjustTime = realAdjustTime - adjustmentTime;
			if (startAdjustTime <= current) {
				realAdjustTime += Constants.TIME_ADJUSTMENT_MS_N;
				rep = 0;
				startAdjustTime = realAdjustTime - adjustmentTime;
			}
		}
		return new NextAdjust(solvisAdjustTime, realAdjustTime, startAdjustTime);
	}

	public void instantiate(final Solvis solvis) {

		Executable executable = new Executable(solvis);
		AllSolvisData allData = solvis.getAllSolvisData();

		SolvisData timeChannel = allData.get(this.timeChannelId);
		timeChannel.register(executable);

		this.disableClockSetting.instantiate(solvis, executable);
	}

	public static class Creator extends CreatorByXML<ClockMonitor> {

		private String timeChannelId;
		private String screenId;
		private String okScreenId;
		private List<DatePart> dateParts;
		private TouchPoint upper;
		private TouchPoint lower;
		private TouchPoint ok;
		private DisableClockSetting disableClockSetting;

		public Creator(final String id, final BaseCreator<?> creator) {
			super(id, creator);
			this.dateParts = new ArrayList<>(5);
			for (int i = 0; i < 5; ++i) {
				this.dateParts.add(null);
			}
		}

		@Override
		public void setAttribute(final QName name, final String value) {
			switch (name.getLocalPart()) {
				case "screenId":
					this.screenId = value;
					break;
				case "okScreenId":
					this.okScreenId = value;
					break;
				case "timeChannelId":
					this.timeChannelId = value;
					break;
			}
		}

		@Override
		public ClockMonitor create() throws XmlException, IOException {
			return new ClockMonitor(this.timeChannelId, this.screenId, this.okScreenId, this.dateParts, this.upper,
					this.lower, this.ok, this.disableClockSetting);
		}

		@Override
		public CreatorByXML<?> getCreator(final QName name) {
			String id = name.getLocalPart();
			switch (id) {
				case XML_YEAR:
					return new DatePart.Creator(id, getBaseCreator(), Calendar.YEAR, 0, 0);
				case XML_MONTH:
					return new DatePart.Creator(id, getBaseCreator(), Calendar.MONTH, 0, 1);
				case XML_DAY:
					return new DatePart.Creator(id, getBaseCreator(), Calendar.DAY_OF_MONTH, 1, 1);
				case XML_HOUR:
					return new DatePart.Creator(id, getBaseCreator(), Calendar.HOUR_OF_DAY, 0, 0);
				case XML_MINUTE:
					return new DatePart.Creator(id, getBaseCreator(), Calendar.MINUTE, 0, 0);
				case XML_UPPER:
				case XML_LOWER:
				case XML_OK:
					return new TouchPoint.Creator(id, getBaseCreator());
				case XML_DISABLE_CLOCK_SETTING:
					return new DisableClockSetting.Creator(id, getBaseCreator());
			}
			return null;
		}

		@Override
		public void created(final CreatorByXML<?> creator, final Object created) {
			switch (creator.getId()) {
				case XML_YEAR:
					this.dateParts.set(0, (DatePart) created);
					break;
				case XML_MONTH:
					this.dateParts.set(1, (DatePart) created);
					break;
				case XML_DAY:
					this.dateParts.set(2, (DatePart) created);
					break;
				case XML_HOUR:
					this.dateParts.set(3, (DatePart) created);
					break;
				case XML_MINUTE:
					this.dateParts.set(4, (DatePart) created);
					break;
				case XML_UPPER:
					this.upper = (TouchPoint) created;
					break;
				case XML_LOWER:
					this.lower = (TouchPoint) created;
					break;
				case XML_OK:
					this.ok = (TouchPoint) created;
					break;
				case XML_DISABLE_CLOCK_SETTING:
					this.disableClockSetting = (DisableClockSetting) created;
					break;
			}
		}
	}

	private enum AdjustmentType {
		NONE, NORMAL, FINE
	}

	private class Executable implements IObserver<SolvisData> {

		private AdjustmentType adjustmentTypeRequestPending = AdjustmentType.NONE;
		private final Solvis solvis;
		private final boolean clockTuuning;
		private ClockAdjustmentThread adjustmentThread = null;
		private final StrategyAdjust strategyAdjust = new StrategyAdjust();
		private final AverageInt averageDiff = new AverageInt(30);
		private boolean burner = false;
		private boolean hotWaterPump = false;
		private boolean adjustmentEnable = true;

		private Executable(final Solvis solvis) {
			this.solvis = solvis;
			this.clockTuuning = solvis.getFeatures().isClockTuning();
			solvis.registerAbortObserver(new IObserver<Boolean>() {

				@Override
				public void update(final Boolean data, final Object source) {
					if (Executable.this.adjustmentThread != null) {
						Executable.this.adjustmentThread.abort();
					}

				}
			});

		}

		@Override
		public void update(final SolvisData data, final Object source) {
			if (!this.clockTuuning) {
				return;
			}
			String channelId = data.getId();
			String burnerId = ClockMonitor.this.disableClockSetting.getBurnerId();
			String hotWaterPumpId = ClockMonitor.this.disableClockSetting.getHotWaterPumpId();
			boolean helper = false;
			try {
				if (burnerId.equals(channelId) || helper) {
					this.burner = helper || data.getBool();
					this.adjustementDisableHandling();
				} else if (hotWaterPumpId != null && hotWaterPumpId.equals(channelId)) {
					this.hotWaterPump = data.getBool();
					this.adjustementDisableHandling();
				} else if (data.getId().equals(ClockMonitor.this.timeChannelId)) {
					DateValue dateValue = (DateValue) data.getSingleData();

					if (this.adjustmentTypeRequestPending != AdjustmentType.NONE) {
						return;
					}

					Calendar solvisDate = dateValue.get();
					long timeStamp = dateValue.getTimeStamp();

					if (timeStamp < CALENDAR_2018.getTimeInMillis()) {
						return;
					}

					Calendar checkSeasonChange = (Calendar) solvisDate.clone();
					checkSeasonChange.setTimeInMillis(timeStamp);
					int hour = checkSeasonChange.get(Calendar.HOUR_OF_DAY);
					checkSeasonChange.add(Calendar.HOUR_OF_DAY, 2);
					int seasonDiff = checkSeasonChange.get(Calendar.HOUR_OF_DAY) - hour;
					seasonDiff = seasonDiff < 0 ? seasonDiff + 24 : seasonDiff;
					if (seasonDiff != 2) {
						return;
					}

					int diff = (int) (solvisDate.getTimeInMillis() - timeStamp);

					// logger.info( "SolvisDate: " + solvisDate.getTimeInMillis() + ", timeStamp: "
					// + timeStamp + ", diff: " + diff);

					if (this.averageDiff.size() > 0 && Math.abs(this.averageDiff.get() - diff) > 2000) { // clock was
																											// manually
																											// adjusted
						// logger.info( "Fehler erkannt");
						this.averageDiff.clear();
						this.averageDiff.put(diff);
					}

					this.averageDiff.put(diff);

					if (!this.averageDiff.isFilled()) {
						return;
					}

					diff = this.averageDiff.get();

					if (Math.abs(diff) > 30500) {
						this.adjustmentTypeRequestPending = AdjustmentType.NORMAL;
						NextAdjust nextAdjust = calculateNextAdjustTime(dateValue, this.solvis);
						if (this.adjustmentThread != null) {
							this.adjustmentThread.abort();
						}
						this.sheduleAdjustment(this.strategyAdjust, nextAdjust);
						return;
					}

				}
			} catch (TypeException e) {
				logger.error("Topic error, update ignored", e);
			}
		}

		private void sheduleAdjustment(final IAdjustStrategy strategy, final NextAdjust nextAdjust) {
			if (this.adjustmentThread != null) {
				this.adjustmentThread.abort();
			}
			this.adjustmentThread = new ClockAdjustmentThread(new CommandClock(strategy, nextAdjust));
			this.adjustmentThread.submit();
		}

		private void adjustementDisableHandling() {
			this.adjustmentEnable = !this.burner && !this.hotWaterPump;
		}

		private class StrategyAdjust implements IAdjustStrategy {

			@Override
			public boolean execute(final NextAdjust nextAdjust)
					throws IOException, TerminationException, SolvisErrorException {
				Solvis solvis = Executable.this.solvis;
				Calendar adjustmentCalendar = Calendar.getInstance();
				long now = adjustmentCalendar.getTimeInMillis();
				if (now > nextAdjust.realAdjustTime) {
					Executable.this.adjustmentTypeRequestPending = AdjustmentType.NONE;
					return false;
				}
				adjustmentCalendar.setTimeInMillis(nextAdjust.solvisAdjustTime);
				boolean success = false;
				Screen clockAdjustScreen = (Screen) ClockMonitor.this.screen.get(solvis);
				if (clockAdjustScreen == null) {
					logger.error("Clock adjust screen not defined in the current configuration. Adjustment terminated");
					Executable.this.adjustmentTypeRequestPending = AdjustmentType.NONE;
					return false;
				}

				for (int repeatFail = 0; !success && repeatFail < Constants.FAIL_REPEATS; ++repeatFail) {
					success = true;
					if (repeatFail > 0) {
						Executable.this.solvis.gotoHome(true);
					}
					try {
						for (DatePart part : ClockMonitor.this.dateParts) {
							int offset = part.solvisOrigin - part.calendarOrigin;
							boolean adjusted = false;
							for (int repeat = 0; !adjusted && repeat < Constants.SET_REPEATS + 1; ++repeat) {
								clockAdjustScreen.goTo(Executable.this.solvis);
								Integer solvisData = part.getValue(Executable.this.solvis);
								if (solvisData == null) {
									logger.error("Setting of the solvis clock failed, it will be tried again.");
									throw new HelperException();
								} else {
									int diff = adjustmentCalendar.get(part.calendarInt) + offset - solvisData;
									int steps;
									TouchPoint touchPoint;
									if (diff != 0) {
										if (repeat == 1) {
											logger.error("Setting of the solvis clock failed, it will be tried again.");
										}
										if (diff > 0) {
											steps = diff;
											touchPoint = ClockMonitor.this.upper;
										} else {
											steps = -diff;
											touchPoint = ClockMonitor.this.lower;
										}
										for (int cnt = 0; cnt < steps; ++cnt) {
											Executable.this.solvis.send(touchPoint);
										}
									} else {
										adjusted = true;
									}
								}
							}
							if (!adjusted) {
								success = false;
								throw new HelperException();
							}
						}
					} catch (HelperException he) {
						success = false;
					}
				}
				if (success) {
					long time = System.currentTimeMillis();
					int waitTime = (int) (nextAdjust.realAdjustTime - time);
					if (waitTime > 0) {
						AbortHelper.getInstance().sleep(waitTime);
					}

					time = System.currentTimeMillis();
					if (Executable.this.adjustmentEnable && time < nextAdjust.realAdjustTime
							- Constants.SETTING_TIME_RANGE_LOWER + Constants.SETTING_TIME_RANGE_UPPER) {
						Executable.this.solvis.send(ClockMonitor.this.ok);
						AbstractScreen screen = SolvisScreen.get(Executable.this.solvis.getCurrentScreen());
						if (screen != ClockMonitor.this.okScreen.get(solvis)) {
							success = false;
						}
						Executable.this.averageDiff.clear();
					} else {
						success = false;
					}
					Executable.this.adjustmentTypeRequestPending = AdjustmentType.NONE;

					if (success) {
						logger.info("Setting of the solvis clock successful.");
					} else {
						logger.error("Setting of the solvis clock not successful, will be tried again.");
					}
				}
				return success;
			}

			@Override
			public void notExecuted() {
				Executable.this.adjustmentTypeRequestPending = AdjustmentType.NONE;
			}
		}

		private class ClockAdjustmentThread extends Helper.Runnable {

			private CommandClock command;
			private boolean abort = false;

			private ClockAdjustmentThread(final CommandClock command) {
				super("ClockAdjustmentThread");
				this.command = command;
			}

			@Override
			public void run() {
				synchronized (this) {
					NextAdjust nextAdjust = this.command.getNextAdjust();
					logger.info(nextAdjust.toString());

					int waitTime = (int) (this.command.getNextAdjust().getStartAdjustTime()
							- System.currentTimeMillis());
					if (waitTime > 0) {
						try {
							this.wait(waitTime);
						} catch (InterruptedException e) {
						}
					}
				}
				if (!this.abort) {
					Executable.this.solvis.execute(this.command);
					this.command = null;
				}
			}

			private synchronized void abort() {
				this.abort = true;
				this.notifyAll();
			}

		}

	}

	private long calculateNextAdjustmentTime(final long time, final int ms) {
		int delta = Constants.TIME_ADJUSTMENT_MS_N;
		long next = time / delta * delta + ms;
		if (next < time) {
			next += delta;
		}
		if (next % 3600000 == 0) {
			next += delta;
		}
		return next;
	}

	@Override
	public void learn(final Solvis solvis) throws IOException, LearningException, TerminationException, SolvisErrorException {
		Screen screen = (Screen) this.screen.get(solvis);
		if (screen == null) {
			String error = "Learning of the clock screens not possible, rejected."
					+ "Screens undefined in the current configuration. Check the control.xml!";
			logger.error(error);
			throw new LearningException(error);
		}
		boolean finished = false;
		for (int repeat = 0; repeat < Constants.LEARNING_RETRIES && !finished; ++repeat) {
			try {
				if (repeat > 0) {
					logger.log(LEARN, "Learning of clock not successfull, try it again.");
				}
				screen.goTo(solvis);
				finished = true;
				for (DatePart part : this.dateParts) {
					if (!part.screenGrafic.isLearned(solvis)) {
						MyImage former = new MyImage(SolvisScreen.getImage(solvis.getCurrentScreen()), part.rectangle,
								false);
						solvis.send(part.touch);
						SolvisScreen currentScreen = solvis.getCurrentScreen();
						MyImage current = new MyImage(SolvisScreen.getImage(currentScreen), part.rectangle, false);
						if (former.equals(current)) {
							finished = false;
							break;
						}
						solvis.writeLearningImage(currentScreen, screen.getId() + "__" + part.screenGrafic.getId());
						part.screenGrafic.learn(solvis);
						if (part.getValue(solvis) == null) {
							finished = false;
							break;
						}
					}
				}
			} catch (IOException e) {
				finished = false;
			}
			if (!finished) {
				if (repeat > Constants.LEARNING_RETRIES / 2) {
					solvis.gotoHome(true);
				} else {
					solvis.sendBackWithCheckError();
				}
			}
		}
		if (!finished) {
			String error = "Learning of clock not possible, rekjected.";
			logger.error(error);
			throw new LearningException(error);
		}
	}

	private static class DatePart {

		private static Calendar CALENDAR = Calendar.getInstance();

		private static final String XML_RECTANGLE = "Rectangle";
		private static final String XML_TOUCH = "Touch";
		private static final String XML_SCREEN_GRAFIC = "ScreenGrafic";

		private final Rectangle rectangle;
		private final TouchPoint touch;
		private final ScreenGraficDescription screenGrafic;
		private final int calendarInt;

		private int getCalendarInt() {
			return this.calendarInt;
		}

		private final int calendarOrigin;
		private final int solvisOrigin;

		private DatePart(final Rectangle rectangle, final TouchPoint touch, final ScreenGraficDescription screenGrafic,
				final int calendarInt, final int calendarOrigin, final int solvisOrigin) {
			this.rectangle = rectangle;
			this.touch = touch;
			this.screenGrafic = screenGrafic;
			this.calendarInt = calendarInt;
			this.calendarOrigin = calendarOrigin;
			this.solvisOrigin = solvisOrigin;
		}

		private static class Creator extends CreatorByXML<DatePart> {

			private Rectangle rectangle;
			private TouchPoint touch;
			private ScreenGraficDescription screenGrafic;
			private final int calendarInt;
			private final int calendarOrigin;
			private final int solvisOrigin;

			private Creator(final String id, final BaseCreator<?> creator, final int calendarInt,
					final int calendarOrigin, final int solvisOrigin) {
				super(id, creator);
				this.calendarInt = calendarInt;
				this.calendarOrigin = calendarOrigin;
				this.solvisOrigin = solvisOrigin;
			}

			@Override
			public void setAttribute(final QName name, final String value) {
			}

			@Override
			public DatePart create() throws XmlException, IOException {
				return new DatePart(this.rectangle, this.touch, this.screenGrafic, this.calendarInt,
						this.calendarOrigin, this.solvisOrigin);
			}

			@Override
			public CreatorByXML<?> getCreator(final QName name) {
				String id = name.getLocalPart();
				switch (id) {
					case XML_RECTANGLE:
						return new Rectangle.Creator(id, getBaseCreator());
					case XML_TOUCH:
						return new TouchPoint.Creator(id, getBaseCreator());
					case XML_SCREEN_GRAFIC:
						return new ScreenGraficDescription.Creator(id, getBaseCreator());
				}
				return null;
			}

			@Override
			public void created(final CreatorByXML<?> creator, final Object created) {
				switch (creator.getId()) {
					case XML_RECTANGLE:
						this.rectangle = (Rectangle) created;
						break;
					case XML_TOUCH:
						this.touch = (TouchPoint) created;
						break;
					case XML_SCREEN_GRAFIC:
						this.screenGrafic = (ScreenGraficDescription) created;
						break;
				}

			}

		}

		private Integer getValue(final Solvis solvis) throws IOException, TerminationException {
			solvis.send(this.touch);
			solvis.clearCurrentScreen();
			MyImage image = solvis.getCurrentScreen().getImage();
			Integer solvisData = null;
			if (this.screenGrafic.isElementOf(image, solvis)) {
				OcrRectangle ocr = new OcrRectangle(image, this.rectangle);
				try {
					solvisData = Integer.parseInt(ocr.getString());
				} catch (NumberFormatException e) {
					logger.error("Scanned date not an integer, check the control.xml");
					return null;
				}
				if (solvisData < this.solvisOrigin
						|| CALENDAR.getMaximum(this.calendarInt) + this.solvisOrigin < solvisData) {
					logger.warn("Illegal time value <" + solvisData + "> on reading the current solvis date.");
					solvisData = null;
				}
			}
			return solvisData;
		}
	}

	private static class DisableClockSetting {
		private final String burnerId;
		private final String hotWaterPumpId;

		private DisableClockSetting(final String burnerId, final String hotWaterPumpId) {
			this.burnerId = burnerId;
			this.hotWaterPumpId = hotWaterPumpId;
		}

		private String getBurnerId() {
			return this.burnerId;
		}

		private String getHotWaterPumpId() {
			return this.hotWaterPumpId;
		}

		private static class Creator extends CreatorByXML<DisableClockSetting> {

			private String burnerId;
			private String hotWaterPumpId = null;

			private Creator(final String id, final BaseCreator<?> creator) {
				super(id, creator);
			}

			@Override
			public void setAttribute(final QName name, final String value) {
				switch (name.getLocalPart()) {
					case "burnerId":
						this.burnerId = value;
						break;
					case "hotWaterPumpId":
						this.hotWaterPumpId = value;
						break;
				}

			}

			@Override
			public DisableClockSetting create() throws XmlException, IOException {
				return new DisableClockSetting(this.burnerId, this.hotWaterPumpId);
			}

			@Override
			public CreatorByXML<?> getCreator(final QName name) {
				return null;
			}

			@Override
			public void created(final CreatorByXML<?> creator, final Object created) {
			}

		}

		public void instantiate(final Solvis solvis, final Executable executable) {
			AllSolvisData allData = solvis.getAllSolvisData();

			SolvisData burnerChannel = allData.get(this.burnerId);
			burnerChannel.register(executable);

			SolvisData hotWaterPumpChannel = allData.get(this.hotWaterPumpId);
			hotWaterPumpChannel.register(executable);

		}

	}

}