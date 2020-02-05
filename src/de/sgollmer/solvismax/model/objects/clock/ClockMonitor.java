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

import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import de.sgollmer.solvismax.Constants;
import de.sgollmer.solvismax.error.HelperError;
import de.sgollmer.solvismax.error.LearningError;
import de.sgollmer.solvismax.error.TerminationException;
import de.sgollmer.solvismax.error.XmlError;
import de.sgollmer.solvismax.helper.AbortHelper;
import de.sgollmer.solvismax.helper.Helper;
import de.sgollmer.solvismax.helper.Helper.AverageInt;
import de.sgollmer.solvismax.imagepatternrecognition.image.MyImage;
import de.sgollmer.solvismax.imagepatternrecognition.ocr.OcrRectangle;
import de.sgollmer.solvismax.model.Solvis;
import de.sgollmer.solvismax.model.objects.Assigner;
import de.sgollmer.solvismax.model.objects.Observer.ObserverI;
import de.sgollmer.solvismax.model.objects.OfConfigs;
import de.sgollmer.solvismax.model.objects.SolvisDescription;
import de.sgollmer.solvismax.model.objects.TouchPoint;
import de.sgollmer.solvismax.model.objects.data.DateValue;
import de.sgollmer.solvismax.model.objects.data.SolvisData;
import de.sgollmer.solvismax.model.objects.screen.GraficsLearnable;
import de.sgollmer.solvismax.model.objects.screen.Screen;
import de.sgollmer.solvismax.model.objects.screen.ScreenGraficDescription;
import de.sgollmer.solvismax.objects.Rectangle;
import de.sgollmer.solvismax.xml.BaseCreator;
import de.sgollmer.solvismax.xml.CreatorByXML;

public class ClockMonitor implements Assigner, GraficsLearnable {

	private static final Logger logger = LogManager.getLogger(ClockMonitor.class);
	private static final Level LEARN = Level.getLevel("LEARN");
	private static final Calendar CALENDAR_2018;

	private static final String XML_SECONDS_SCAN = "SecondsScan";
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
	private final Rectangle secondsScan;
	private final List<DatePart> dateParts;
	private final TouchPoint upper;
	private final TouchPoint lower;
	private final TouchPoint ok;
	private final DisableClockSetting disableClockSetting;

	private OfConfigs<Screen> screen = null;
	private OfConfigs<Screen> okScreen = null;

	static {
		CALENDAR_2018 = Calendar.getInstance();
		CALENDAR_2018.set(2018, 0, 1, 0, 0, 0);
		CALENDAR_2018.set(Calendar.MILLISECOND, 0);
	}

	public ClockMonitor(String timeChannelId, String screenId, String okScreenId, Rectangle secondsScan,
			List<DatePart> dateParts, TouchPoint upper, TouchPoint lower, TouchPoint ok,
			DisableClockSetting disableClockSetting) {
		this.timeChannelId = timeChannelId;
		this.screenId = screenId;
		this.okScreenId = okScreenId;
		this.dateParts = dateParts;
		this.upper = upper;
		this.lower = lower;
		this.ok = ok;
		this.secondsScan = secondsScan;
		this.disableClockSetting = disableClockSetting;
	}

	public boolean set(Solvis solvis) throws IOException, TerminationException {
		boolean result = this.screen.get(solvis.getConfigurationMask()).goTo(solvis);
		if (!result) {
			return false;
		}

		return true;
	}

	@Override
	public void assign(SolvisDescription description) {
		this.screen = description.getScreens().get(screenId);
		this.okScreen = description.getScreens().get(okScreenId);
		this.upper.assign(description);
		this.lower.assign(description);
		this.ok.assign(description);
		for (DatePart part : this.dateParts) {
			part.assign(description);
		}
	}

	public static class NextAdjust {
		private final long solvisAdjustTime;
		private final long realAdjustTime;
		private final long startAdjustTime;
		private final int fineAdjusts;

		public NextAdjust(long solvisAdjustTime, long realAdjustTime, long startAdjustTime) {
			this.solvisAdjustTime = solvisAdjustTime;
			this.realAdjustTime = realAdjustTime;
			this.startAdjustTime = startAdjustTime;
			this.fineAdjusts = -1;
		}

		public NextAdjust(int fineAdjusts, long startAdjustTime) {
			this.solvisAdjustTime = -1;
			this.realAdjustTime = -1;
			this.startAdjustTime = startAdjustTime;
			this.fineAdjusts = fineAdjusts;
		}

		public long getStartAdjustTime() {
			return startAdjustTime;
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

	interface AdjustStrategy {
		public boolean execute(NextAdjust nextAdjust) throws IOException, TerminationException;
	}

	private NextAdjust calculateNextAdjustTime(DateValue dateValue, Solvis solvis) {
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
					+ 8 * this.screen.get(solvis.getConfigurationMask()).getTouchPoint().getSettingTime(solvis);
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

	public void instantiate(Solvis solvis) {

		if (solvis.getUnit().getClockAdjustment().isEnable()) {
			Executable executable = new Executable(solvis);
			solvis.registerObserver(executable);
		}
	}

	public static class Creator extends CreatorByXML<ClockMonitor> {

		private String timeChannelId;
		private String screenId;
		private String okScreenId;
		private List<DatePart> dateParts;
		private Rectangle secondsScan;
		private TouchPoint upper;
		private TouchPoint lower;
		private TouchPoint ok;
		private DisableClockSetting disableClockSetting;

		public Creator(String id, BaseCreator<?> creator) {
			super(id, creator);
			this.dateParts = new ArrayList<>(5);
			for (int i = 0; i < 5; ++i) {
				this.dateParts.add(null);
			}
		}

		@Override
		public void setAttribute(QName name, String value) {
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
		public ClockMonitor create() throws XmlError, IOException {
			return new ClockMonitor(timeChannelId, screenId, okScreenId, secondsScan, dateParts, upper, lower, ok,
					disableClockSetting);
		}

		@Override
		public CreatorByXML<?> getCreator(QName name) {
			String id = name.getLocalPart();
			switch (id) {
				case XML_SECONDS_SCAN:
					return new Rectangle.Creator(id, getBaseCreator());
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
					return new TouchPoint.Creator(id, getBaseCreator());
				case XML_LOWER:
					return new TouchPoint.Creator(id, getBaseCreator());
				case XML_OK:
					return new TouchPoint.Creator(id, getBaseCreator());
				case XML_DISABLE_CLOCK_SETTING:
					return new DisableClockSetting.Creator(id, getBaseCreator());
			}
			return null;
		}

		@Override
		public void created(CreatorByXML<?> creator, Object created) {
			switch (creator.getId()) {
				case XML_SECONDS_SCAN:
					this.secondsScan = (Rectangle) created;
					break;
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

	public class Executable implements ObserverI<SolvisData> {

		private AdjustmentType adjustmentTypeRequestPending = AdjustmentType.NONE;
		private final Solvis solvis;
		private ClockAdjustmentThread adjustmentThread = null;
		private final ClockAdjustment clockAdjustment;
		private final StrategyAdjust strategyAdjust = new StrategyAdjust();
		private final StrategyFineAdjust strategyFineAdjust = new StrategyFineAdjust();
		private final AverageInt averageDiff = new AverageInt(30);
		private boolean burner = false;
		private boolean hotWaterPump = false;
		private boolean adjustmentEnable = true;

		public Executable(Solvis solvis) {
			this.solvis = solvis;
			this.clockAdjustment = solvis.getUnit().getClockAdjustment();
			if (this.clockAdjustment.isEnable()) {
				solvis.registerAbortObserver(new ObserverI<Boolean>() {

					@Override
					public void update(Boolean data, Object source) {
						if (adjustmentThread != null) {
							adjustmentThread.abort();
						}

					}
				});
			} else {
				this.adjustmentThread = null;

			}
		}

		@Override
		public void update(SolvisData data, Object source) {
			String channelId = data.getId();
			String burnerId = disableClockSetting.getBurnerId();
			String hotWaterPumpId = disableClockSetting.getHotWaterPumpId();
			boolean helper = false;
			if (burnerId.equals(channelId) || helper) {
				this.burner = helper || data.getBool();
				this.adjustementDisableHandling();
			} else if (hotWaterPumpId != null && hotWaterPumpId.equals(channelId)) {
				this.hotWaterPump = data.getBool();
				this.adjustementDisableHandling();
			} else if (data.getId().equals(timeChannelId)) {
				DateValue dateValue = (DateValue) data.getSingleData();

				if (this.adjustmentTypeRequestPending != AdjustmentType.NONE) {
					return;
				}

				Calendar solvisDate = dateValue.get();
				long timeStamp = dateValue.getTimeStamp();
				if (timeStamp < CALENDAR_2018.getTimeInMillis()) {
					return;
				}

				int diff = (int) (solvisDate.getTimeInMillis() - timeStamp);

				this.averageDiff.put(diff);

				if (Math.abs(this.averageDiff.get() - diff) > 2000) { // clock was manually adjusted
					this.averageDiff.clear();
					this.averageDiff.put(diff);
				}

				if (!this.averageDiff.isFilled()) {
					return;
				}

				diff = this.averageDiff.get();

				if (Math.abs(diff) > 30500) {
					this.adjustmentTypeRequestPending = AdjustmentType.NORMAL;
					NextAdjust nextAdjust = calculateNextAdjustTime(dateValue, solvis);
					if (this.adjustmentThread != null) {
						this.adjustmentThread.abort();
					}
					this.sheduleAdjustment(strategyAdjust, nextAdjust);
					return;
				}

				if (this.adjustmentTypeRequestPending == AdjustmentType.NORMAL || clockAdjustment.getBurstLength() == 0
						|| (clockAdjustment.getFineLimitLower_ms() < diff
								&& diff < clockAdjustment.getFineLimitUpper_ms())) {
					return;
				}
				int delta = diff;
				if (delta < 0) {
					delta = 60000 + delta;
				}

				int fineAdjusts = delta / clockAdjustment.getAproximatlySetAjust_ms();
				fineAdjusts = fineAdjusts == 0 ? 1 : fineAdjusts;
				int burstLength = clockAdjustment.getBurstLength();
				fineAdjusts = fineAdjusts > burstLength ? burstLength : fineAdjusts;

				long current = System.currentTimeMillis();
				long startAdjustTime = calculateNextAdjustmentTime(current, Constants.TIME_FINE_ADJUSTMENT_MS_N);
				NextAdjust nextAdjust = new NextAdjust(fineAdjusts, startAdjustTime);

				this.adjustmentTypeRequestPending = AdjustmentType.FINE;
				this.sheduleAdjustment(strategyFineAdjust, nextAdjust);
			}
		}

		private void sheduleAdjustment(AdjustStrategy strategy, NextAdjust nextAdjust) {
			if (this.adjustmentThread != null) {
				this.adjustmentThread.abort();
			}
			this.adjustmentThread = new ClockAdjustmentThread(new CommandClock(strategy, nextAdjust));
			this.adjustmentThread.submit();
		}

		private void adjustementDisableHandling() {
			this.adjustmentEnable = !this.burner && !this.hotWaterPump;
		}

		private class StrategyAdjust implements AdjustStrategy {

			@Override
			public boolean execute(NextAdjust nextAdjust) throws IOException {
				int configurationMask = solvis.getConfigurationMask();
				Calendar adjustementCalendar = Calendar.getInstance();
				adjustementCalendar.setTimeInMillis(nextAdjust.solvisAdjustTime);
				boolean success = false;
				Screen clockAdjustScreen = screen.get(configurationMask);
				if (clockAdjustScreen == null) {
					logger.error("Clock adjust screen not defined in the current configuration. Adjustment terminated");
					return false;
				}

				for (int repeatFail = 0; !success && repeatFail < Constants.FAIL_REPEATS; ++repeatFail) {
					success = true;
					if (repeatFail > 0) {
						solvis.gotoHome();
					}
					try {
						for (DatePart part : dateParts) {
							int offset = part.solvisOrigin - part.calendarOrigin;
							boolean adjusted = false;
							for (int repeat = 0; !adjusted && repeat < Constants.SET_REPEATS + 1; ++repeat) {
								screen.get(configurationMask).goTo(solvis);
								Integer solvisData = part.getValue(solvis);
								if (solvisData == null) {
									logger.error("Setting of the solvis clock failed, it will be tried again.");
									throw new HelperError();
								} else {
									int diff = adjustementCalendar.get(part.calendarInt) + offset - solvisData;
									int steps;
									TouchPoint touchPoint;
									if (diff != 0) {
										if (repeat == 1) {
											logger.error("Setting of the solvis clock failed, it will be tried again.");
										}
										if (diff > 0) {
											steps = diff;
											touchPoint = upper;
										} else {
											steps = -diff;
											touchPoint = lower;
										}
										for (int cnt = 0; cnt < steps; ++cnt) {
											solvis.send(touchPoint);
										}
									} else {
										adjusted = true;
									}
								}
							}
							if (!adjusted) {
								success = false;
								throw new HelperError();
							}
						}
					} catch (HelperError he) {
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
					if (adjustmentEnable && time < nextAdjust.realAdjustTime - Constants.SETTING_TIME_RANGE_LOWER
							+ Constants.SETTING_TIME_RANGE_UPPER) {
						solvis.send(ok);
						Screen screen = solvis.getCurrentScreen().get();
						if (screen != ClockMonitor.this.okScreen.get(configurationMask)) {
							success = false;
						}
						averageDiff.clear();
					} else {
						success = false;
					}
					adjustmentTypeRequestPending = AdjustmentType.NONE;

					if (success) {
						logger.info("Setting of the solvis clock successful.");
					} else {
						logger.error("Setting of the solvis clock not successful, will be tried again.");
					}
				}
				return success;
			}
		}

		private class StrategyFineAdjust implements AdjustStrategy {

			@Override
			public boolean execute(NextAdjust nextAdjust) throws IOException, TerminationException {

				int configurationMask = solvis.getConfigurationMask();

				Screen clockAdjustScreen = screen.get(configurationMask);
				Screen okScreen = ClockMonitor.this.okScreen.get(configurationMask);

				if (clockAdjustScreen == null) {
					logger.error(
							"Clock adjust screen not defined in the current configuratiion. Adjustment terminated");
					return false;
				}

				int fineAdjusts = nextAdjust.fineAdjusts;

				boolean success = true;

				for (; fineAdjusts > 0; --fineAdjusts) {

					boolean preparationSuccessfull = false;
					for (int repeat = 0; !preparationSuccessfull && repeat < Constants.SET_REPEATS; ++repeat) {
						success = false;
						if (!okScreen.goTo(solvis)) {
							logger.info("Fine tuning not successfull, will be repeated");
							continue;
						}
						int secondsFormer = -1;
						int seconds = -1;
						boolean finished = false;
						for (int repSec = 0; !finished && repSec < Constants.SET_REPEATS + 2; ++repSec) {
							secondsFormer = seconds;
							OcrRectangle ocr = new OcrRectangle(solvis.getCurrentScreen().getImage(), secondsScan);
							String secondsString = ocr.getString();
							solvis.clearCurrentScreen();
							try {
								seconds = Integer.parseInt(secondsString);
							} catch (NumberFormatException e) {
								seconds = -1;
							}
							if (seconds >= 0 && secondsFormer == seconds) {
								finished = true;
							} else {
								AbortHelper.getInstance().sleep(100);
							}
						}
						if (!finished) {
							logger.info("Fine tuning not successfull, will be repeated");
							continue;
						}
						if (seconds > 45) {
							AbortHelper.getInstance().sleep((61 - seconds) * 1000);
						}
						success = true;
						preparationSuccessfull = true;
					}
					if (!clockAdjustScreen.goTo(solvis)) {
						logger.error("Fine tuning not successfull, will be repeated");
						success = false;
						break;
					}
					if (!adjustmentEnable) {
						success = false;
						break;
					}
					solvis.send(ok);
					if (solvis.getCurrentScreen().get() != okScreen) {
						logger.error("Fine tuning not successfull, will be repeated");
						success = false;
						break;
					}
				}
				adjustmentTypeRequestPending = AdjustmentType.NONE;

				if (success) {
					logger.info("Fine adjustment of the solvis clock successful.");
				} else {
					logger.error("Fine adjustment of the solvis clock not successful.");

				}
				averageDiff.clear();
				return success;
			}

		}

		private class ClockAdjustmentThread extends Helper.Runnable {

			private CommandClock command;
			private boolean abort = false;

			public ClockAdjustmentThread(CommandClock command) {
				super("ClockAdjustmentThread");
				this.command = command;
			}

			@Override
			public void run() {
				synchronized (this) {
					NextAdjust nextAdjust = this.command.getNextAdjust();
					logger.info(nextAdjust.toString());

					int waitTime = (int) (this.command.getNextAdjust().startAdjustTime - System.currentTimeMillis());
					if (waitTime > 0) {
						try {
							this.wait(waitTime);
						} catch (InterruptedException e) {
						}
					}
				}
				if (!abort) {
					solvis.execute(command);
					this.command = null;
				}
			}

			public synchronized void abort() {
				this.abort = true;
				this.notifyAll();
			}

		}

	}

	private long calculateNextAdjustmentTime(long time, int ms) {
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
	public void learn(Solvis solvis) throws IOException, LearningError {
		Screen screen = this.screen.get(solvis.getConfigurationMask());
		if (screen == null) {
			String error = "Learning of the clock screens not possible, rejected."
					+ "Screens undefined in the current configuration. Check the control.xml!";
			logger.error(error);
			throw new LearningError(error);
		}
		this.screen.get(solvis.getConfigurationMask()).goTo(solvis);
		boolean finished = false;
		for (int repeat = 0; repeat < Constants.LEARNING_RETRIES && !finished; ++repeat) {
			if (repeat == 1) {
				logger.log(LEARN, "Learning of clock not successfull, try it again.");
			}
			finished = true;
			for (DatePart part : this.dateParts) {
				solvis.send(part.touch);
				solvis.clearCurrentScreen();
				part.screenGrafic.learn(solvis);
				if (part.getValue(solvis) == null) {
					finished = false;
					break;
				}
			}
		}
		if (!finished) {
			String error = "Learning of clock not possible, rekjected.";
			logger.error(error);
			throw new LearningError(error);
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

		public int getCalendarInt() {
			return calendarInt;
		}

		public void assign(SolvisDescription description) {
			this.touch.assign(description);
			this.screenGrafic.assign(description);

		}

		private final int calendarOrigin;
		private final int solvisOrigin;

		public DatePart(Rectangle rectangle, TouchPoint touch, ScreenGraficDescription screenGrafic, int calendarInt,
				int calendarOrigin, int solvisOrigin) {
			this.rectangle = rectangle;
			this.touch = touch;
			this.screenGrafic = screenGrafic;
			this.calendarInt = calendarInt;
			this.calendarOrigin = calendarOrigin;
			this.solvisOrigin = solvisOrigin;
		}

		public static class Creator extends CreatorByXML<DatePart> {

			private Rectangle rectangle;
			private TouchPoint touch;
			private ScreenGraficDescription screenGrafic;
			private final int calendarInt;
			private final int calendarOrigin;
			private final int solvisOrigin;

			public Creator(String id, BaseCreator<?> creator, int calendarInt, int calendarOrigin, int solvisOrigin) {
				super(id, creator);
				this.calendarInt = calendarInt;
				this.calendarOrigin = calendarOrigin;
				this.solvisOrigin = solvisOrigin;
			}

			@Override
			public void setAttribute(QName name, String value) {
			}

			@Override
			public DatePart create() throws XmlError, IOException {
				return new DatePart(rectangle, touch, screenGrafic, calendarInt, calendarOrigin, solvisOrigin);
			}

			@Override
			public CreatorByXML<?> getCreator(QName name) {
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
			public void created(CreatorByXML<?> creator, Object created) {
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

		public Integer getValue(Solvis solvis) throws IOException, TerminationException {
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

	public static class DisableClockSetting {
		private final String burnerId;
		private final String hotWaterPumpId;

		public DisableClockSetting(String burnerId, String hotWaterPumpId) {
			this.burnerId = burnerId;
			this.hotWaterPumpId = hotWaterPumpId;
		}

		public String getBurnerId() {
			return burnerId;
		}

		public String getHotWaterPumpId() {
			return hotWaterPumpId;
		}

		public static class Creator extends CreatorByXML<DisableClockSetting> {

			private String burnerId;
			private String hotWaterPumpId = null;

			public Creator(String id, BaseCreator<?> creator) {
				super(id, creator);
			}

			@Override
			public void setAttribute(QName name, String value) {
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
			public DisableClockSetting create() throws XmlError, IOException {
				return new DisableClockSetting(burnerId, hotWaterPumpId);
			}

			@Override
			public CreatorByXML<?> getCreator(QName name) {
				return null;
			}

			@Override
			public void created(CreatorByXML<?> creator, Object created) {
			}

		}
	}

}