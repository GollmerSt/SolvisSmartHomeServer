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
import de.sgollmer.solvismax.error.HelperError;
import de.sgollmer.solvismax.error.LearningError;
import de.sgollmer.solvismax.error.TerminationException;
import de.sgollmer.solvismax.error.XmlError;
import de.sgollmer.solvismax.helper.AbortHelper;
import de.sgollmer.solvismax.helper.Helper;
import de.sgollmer.solvismax.helper.Helper.AverageInt;
import de.sgollmer.solvismax.imagepatternrecognition.image.MyImage;
import de.sgollmer.solvismax.imagepatternrecognition.ocr.OcrRectangle;
import de.sgollmer.solvismax.log.LogManager;
import de.sgollmer.solvismax.log.LogManager.Level;
import de.sgollmer.solvismax.log.LogManager.ILogger;
import de.sgollmer.solvismax.modbus.ModbusAccess;
import de.sgollmer.solvismax.model.Solvis;
import de.sgollmer.solvismax.model.objects.IAssigner;
import de.sgollmer.solvismax.model.objects.Observer.IObserver;
import de.sgollmer.solvismax.model.objects.OfConfigs;
import de.sgollmer.solvismax.model.objects.SolvisDescription;
import de.sgollmer.solvismax.model.objects.TouchPoint;
import de.sgollmer.solvismax.model.objects.data.DateValue;
import de.sgollmer.solvismax.model.objects.data.SolvisData;
import de.sgollmer.solvismax.model.objects.screen.IGraficsLearnable;
import de.sgollmer.solvismax.model.objects.screen.Screen;
import de.sgollmer.solvismax.model.objects.screen.ScreenGraficDescription;
import de.sgollmer.solvismax.model.objects.screen.SolvisScreen;
import de.sgollmer.solvismax.objects.Rectangle;
import de.sgollmer.solvismax.xml.BaseCreator;
import de.sgollmer.solvismax.xml.CreatorByXML;

public class ClockMonitor implements IAssigner, IGraficsLearnable {

	private static final ILogger logger = LogManager.getInstance().getLogger(ClockMonitor.class);
	private static final Level LEARN = Level.getLevel("LEARN");
	private static final Calendar CALENDAR_2018;

	private static final String XML_MODBUS_WRITE = "ModbusWrite";
//	private static final String XML_SECONDS_SCAN = "SecondsScan";
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

	private final ModbusAccess modbusWrite;
	private final String timeChannelId;
	private final String screenId;
	private final String okScreenId;
//	private final Rectangle secondsScan;
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

	private ClockMonitor(ModbusAccess modbusWrite, String timeChannelId, String screenId, String okScreenId,
			Rectangle secondsScan, List<DatePart> dateParts, TouchPoint upper, TouchPoint lower, TouchPoint ok,
			DisableClockSetting disableClockSetting) {
		this.modbusWrite = modbusWrite;
		this.timeChannelId = timeChannelId;
		this.screenId = screenId;
		this.okScreenId = okScreenId;
		this.dateParts = dateParts;
		this.upper = upper;
		this.lower = lower;
		this.ok = ok;
//		this.secondsScan = secondsScan;
		this.disableClockSetting = disableClockSetting;
	}

	@Override
	public void assign(SolvisDescription description) {
		this.screen = description.getScreens().get(this.screenId);
		this.okScreen = description.getScreens().get(this.okScreenId);
		if (this.upper != null) {
			this.upper.assign(description);
		}
		if (this.lower != null) {
			this.lower.assign(description);
		}
		if (this.ok != null) {
			this.ok.assign(description);
		}
		for (DatePart part : this.dateParts) {
			part.assign(description);
		}
	}

	static class NextAdjust {
		private final long solvisAdjustTime;
		private final long realAdjustTime;
		private final long startAdjustTime;
//		private final int fineAdjusts;

		private NextAdjust(long solvisAdjustTime, long realAdjustTime, long startAdjustTime) {
			this.solvisAdjustTime = solvisAdjustTime;
			this.realAdjustTime = realAdjustTime;
			this.startAdjustTime = startAdjustTime;
//			this.fineAdjusts = -1;
		}

		private NextAdjust(int fineAdjusts, long startAdjustTime) {
			this.solvisAdjustTime = -1;
			this.realAdjustTime = -1;
			this.startAdjustTime = startAdjustTime;
//			this.fineAdjusts = fineAdjusts;
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
		boolean execute(NextAdjust nextAdjust) throws IOException, TerminationException;

		void notExecuted();
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
					+ 8 * this.screen.get(solvis).getTouchPoint().getSettingTime(solvis);
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

		Executable executable = new Executable(solvis);
		solvis.registerObserver(executable);
	}

	public static class Creator extends CreatorByXML<ClockMonitor> {

		private ModbusAccess modbusWrite;
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
			return new ClockMonitor(this.modbusWrite, this.timeChannelId, this.screenId, this.okScreenId,
					this.secondsScan, this.dateParts, this.upper, this.lower, this.ok, this.disableClockSetting);
		}

		@Override
		public CreatorByXML<?> getCreator(QName name) {
			String id = name.getLocalPart();
			switch (id) {
				case XML_MODBUS_WRITE:
					return new ModbusAccess.Creator(id, getBaseCreator());
//				case XML_SECONDS_SCAN:
//					return new Rectangle.Creator(id, getBaseCreator());
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
		public void created(CreatorByXML<?> creator, Object created) {
			switch (creator.getId()) {
				case XML_MODBUS_WRITE:
					this.modbusWrite = (ModbusAccess) created;
					break;
//				case XML_SECONDS_SCAN:
//					this.secondsScan = (Rectangle) created;
//					break;
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
		private ClockAdjustmentThread adjustmentThread = null;
		private final StrategyAdjust strategyAdjust = new StrategyAdjust();
//		private final StrategyFineAdjust strategyFineAdjust = new StrategyFineAdjust();
		private final AverageInt averageDiff = new AverageInt(30);
		private boolean burner = false;
		private boolean hotWaterPump = false;
		private boolean adjustmentEnable = true;

		private Executable(Solvis solvis) {
			this.solvis = solvis;
			solvis.registerAbortObserver(new IObserver<Boolean>() {

				@Override
				public void update(Boolean data, Object source) {
					if (Executable.this.adjustmentThread != null) {
						Executable.this.adjustmentThread.abort();
					}

				}
			});

		}

		@Override
		public void update(SolvisData data, Object source) {
			if (!this.solvis.getFeatures().isClockTuning()) {
				return;
			}
			String channelId = data.getId();
			String burnerId = ClockMonitor.this.disableClockSetting.getBurnerId();
			String hotWaterPumpId = ClockMonitor.this.disableClockSetting.getHotWaterPumpId();
			boolean helper = false;
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
					if (this.solvis.getUnit().isModbus()) {
						long now = System.currentTimeMillis();
						try {
							this.solvis.writeUnsignedIntegerModbusData(ClockMonitor.this.modbusWrite, now / 1000L);
							logger.info("Setting of the clock via modbus successfull");
						} catch (IOException e) {
							logger.error("Setting of the clock via modbus not successfull");
						}
					} else {
						this.adjustmentTypeRequestPending = AdjustmentType.NORMAL;
						NextAdjust nextAdjust = calculateNextAdjustTime(dateValue, this.solvis);
						if (this.adjustmentThread != null) {
							this.adjustmentThread.abort();
						}
						this.sheduleAdjustment(this.strategyAdjust, nextAdjust);
					}
					return;
				}

//				if (!this.solvis.getUnit().getFeatures().isClockFineTuning()
//						|| this.adjustmentTypeRequestPending == AdjustmentType.NORMAL
//						|| clockAdjustment.getBurstLength() == 0 || (clockAdjustment.getFineLimitLower_ms() < diff
//								&& diff < clockAdjustment.getFineLimitUpper_ms())) {
//					return;
//				}
//				int delta = diff;
//				if (delta < 0) {
//					delta = 60000 + delta;
//				}
//
//				int fineAdjusts = delta / clockAdjustment.getAproximatlySetAjust_ms();
//				fineAdjusts = fineAdjusts == 0 ? 1 : fineAdjusts;
//				int burstLength = clockAdjustment.getBurstLength();
//				fineAdjusts = fineAdjusts > burstLength ? burstLength : fineAdjusts;
//
//				long current = System.currentTimeMillis();
//				long startAdjustTime = calculateNextAdjustmentTime(current, Constants.TIME_FINE_ADJUSTMENT_MS_N);
//				NextAdjust nextAdjust = new NextAdjust(fineAdjusts, startAdjustTime);
//
//				this.adjustmentTypeRequestPending = AdjustmentType.FINE;
//				this.sheduleAdjustment(strategyFineAdjust, nextAdjust);
			}
		}

		private void sheduleAdjustment(IAdjustStrategy strategy, NextAdjust nextAdjust) {
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
			public boolean execute(NextAdjust nextAdjust) throws IOException {
				int configurationMask = Executable.this.solvis.getConfigurationMask();
				Calendar adjustmentCalendar = Calendar.getInstance();
				long now = adjustmentCalendar.getTimeInMillis();
				if (now > nextAdjust.realAdjustTime) {
					Executable.this.adjustmentTypeRequestPending = AdjustmentType.NONE;
					return false;
				}
				adjustmentCalendar.setTimeInMillis(nextAdjust.solvisAdjustTime);
				boolean success = false;
				Screen clockAdjustScreen = ClockMonitor.this.screen.get(configurationMask);
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
								ClockMonitor.this.screen.get(configurationMask).goTo(Executable.this.solvis);
								Integer solvisData = part.getValue(Executable.this.solvis);
								if (solvisData == null) {
									logger.error("Setting of the solvis clock failed, it will be tried again.");
									throw new HelperError();
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
					if (Executable.this.adjustmentEnable && time < nextAdjust.realAdjustTime
							- Constants.SETTING_TIME_RANGE_LOWER + Constants.SETTING_TIME_RANGE_UPPER) {
						Executable.this.solvis.send(ClockMonitor.this.ok);
						Screen screen = SolvisScreen.get(Executable.this.solvis.getCurrentScreen());
						if (screen != ClockMonitor.this.okScreen.get(configurationMask)) {
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

//		private class StrategyFineAdjust implements AdjustStrategy {
//
//			@Override
//			public boolean execute(NextAdjust nextAdjust) throws IOException, TerminationException {
//
//				int configurationMask = solvis.getConfigurationMask();
//
//				Screen clockAdjustScreen = screen.get(configurationMask);
//				Screen okScreen = ClockMonitor.this.okScreen.get(configurationMask);
//
//				if (clockAdjustScreen == null) {
//					logger.error(
//							"Clock adjust screen not defined in the current configuratiion. Adjustment terminated");
//					return false;
//				}
//
//				int fineAdjusts = nextAdjust.fineAdjusts;
//
//				boolean success = true;
//
//				for (; fineAdjusts > 0; --fineAdjusts) {
//
//					boolean preparationSuccessfull = false;
//					for (int repeat = 0; !preparationSuccessfull && repeat < Constants.SET_REPEATS; ++repeat) {
//						success = false;
//						if (!okScreen.goTo(solvis)) {
//							logger.info("Fine tuning not successfull, will be repeated");
//							continue;
//						}
//						int secondsFormer = -1;
//						int seconds = -1;
//						boolean finished = false;
//						for (int repSec = 0; !finished && repSec < Constants.SET_REPEATS + 2; ++repSec) {
//							secondsFormer = seconds;
//							OcrRectangle ocr = new OcrRectangle(solvis.getCurrentScreen().getImage(), secondsScan);
//							String secondsString = ocr.getString();
//							solvis.clearCurrentScreen();
//							try {
//								seconds = Integer.parseInt(secondsString);
//							} catch (NumberFormatException e) {
//								seconds = -1;
//							}
//							if (seconds >= 0 && secondsFormer == seconds) {
//								finished = true;
//							} else {
//								AbortHelper.getInstance().sleep(100);
//							}
//						}
//						if (!finished) {
//							logger.info("Fine tuning not successfull, will be repeated");
//							continue;
//						}
//						if (seconds > 45) {
//							AbortHelper.getInstance().sleep((61 - seconds) * 1000);
//						}
//						success = true;
//						preparationSuccessfull = true;
//					}
//					if (!clockAdjustScreen.goTo(solvis)) {
//						logger.error("Fine tuning not successfull, will be repeated");
//						success = false;
//						break;
//					}
//					if (!adjustmentEnable) {
//						success = false;
//						break;
//					}
//					solvis.send(ok);
//					if (solvis.getCurrentScreen().get() != okScreen) {
//						logger.error("Fine tuning not successfull, will be repeated");
//						success = false;
//						break;
//					}
//				}
//				adjustmentTypeRequestPending = AdjustmentType.NONE;
//
//				if (success) {
//					logger.info("Fine adjustment of the solvis clock successful.");
//				} else {
//					logger.error("Fine adjustment of the solvis clock not successful.");
//
//				}
//				averageDiff.clear();
//				return success;
//			}
//
//		}

		private class ClockAdjustmentThread extends Helper.Runnable {

			private CommandClock command;
			private boolean abort = false;

			private ClockAdjustmentThread(CommandClock command) {
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
		Screen screen = this.screen.get(solvis);
		if (screen == null) {
			String error = "Learning of the clock screens not possible, rejected."
					+ "Screens undefined in the current configuration. Check the control.xml!";
			logger.error(error);
			throw new LearningError(error);
		}
		boolean finished = false;
		for (int repeat = 0; repeat < Constants.LEARNING_RETRIES && !finished; ++repeat) {
			try {
				if (repeat == 1) {
					logger.log(LEARN, "Learning of clock not successfull, try it again.");
				}
				this.screen.get(solvis).goTo(solvis);
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
			} catch (IOException e) {
				finished = false;
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

		private int getCalendarInt() {
			return this.calendarInt;
		}

		private void assign(SolvisDescription description) {
			if (this.touch != null) {
				this.touch.assign(description);
			}
			if (this.screenGrafic != null) {
				this.screenGrafic.assign(description);
			}

		}

		private final int calendarOrigin;
		private final int solvisOrigin;

		private DatePart(Rectangle rectangle, TouchPoint touch, ScreenGraficDescription screenGrafic, int calendarInt,
				int calendarOrigin, int solvisOrigin) {
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

			private Creator(String id, BaseCreator<?> creator, int calendarInt, int calendarOrigin, int solvisOrigin) {
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
				return new DatePart(this.rectangle, this.touch, this.screenGrafic, this.calendarInt,
						this.calendarOrigin, this.solvisOrigin);
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

		private Integer getValue(Solvis solvis) throws IOException, TerminationException {
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

		private DisableClockSetting(String burnerId, String hotWaterPumpId) {
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

			private Creator(String id, BaseCreator<?> creator) {
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
				return new DisableClockSetting(this.burnerId, this.hotWaterPumpId);
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