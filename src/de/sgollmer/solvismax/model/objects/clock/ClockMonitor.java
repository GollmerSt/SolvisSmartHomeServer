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
import de.sgollmer.solvismax.helper.Helper.AverageInt;
import de.sgollmer.solvismax.imagepatternrecognition.image.MyImage;
import de.sgollmer.solvismax.imagepatternrecognition.ocr.OcrRectangle;
import de.sgollmer.solvismax.model.Solvis;
import de.sgollmer.solvismax.model.objects.Assigner;
import de.sgollmer.solvismax.model.objects.GraficsLearnable;
import de.sgollmer.solvismax.model.objects.Observer.ObserverI;
import de.sgollmer.solvismax.model.objects.OfConfigs;
import de.sgollmer.solvismax.model.objects.Screen;
import de.sgollmer.solvismax.model.objects.ScreenGraficDescription;
import de.sgollmer.solvismax.model.objects.SolvisDescription;
import de.sgollmer.solvismax.model.objects.TouchPoint;
import de.sgollmer.solvismax.model.objects.data.DateValue;
import de.sgollmer.solvismax.model.objects.data.SolvisData;
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

	private static final SimpleDateFormat DATE_FORMAT = new SimpleDateFormat("HH:mm:ss");

	private final String channelId;
	private final String screenId;
	private final String okScreenId;
	private final Rectangle secondsScan;
	private final List<DatePart> dateParts;
	private final TouchPoint upper;
	private final TouchPoint lower;
	private final TouchPoint ok;

	private OfConfigs<Screen> screen = null;
	private OfConfigs<Screen> okScreen = null;

	static {
		CALENDAR_2018 = Calendar.getInstance();
		CALENDAR_2018.set(2018, 0, 1, 0, 0, 0);
		CALENDAR_2018.set(Calendar.MILLISECOND, 0);
	}

	public ClockMonitor(String channelId, String screenId, String okScreenId, Rectangle secondsScan,
			List<DatePart> dateParts, TouchPoint upper, TouchPoint lower, TouchPoint ok) {
		this.channelId = channelId;
		this.screenId = screenId;
		this.okScreenId = okScreenId;
		this.dateParts = dateParts;
		this.upper = upper;
		this.lower = lower;
		this.ok = ok;
		this.secondsScan = secondsScan;
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
	}

	public interface AdjustStrategy {
		public boolean execute(NextAdjust nextAdjust) throws IOException, TerminationException;
	}

	private NextAdjust calculateNextAdjustTime(DateValue dateValue, Solvis solvis) {
		long current = System.currentTimeMillis();
		Calendar solvisTimeCalendar = dateValue.get();
		int singleSettingTime = Math.max(this.upper.getSettingTime(solvis), this.lower.getSettingTime(solvis))
				+ solvis.getMaxResponseTime();
		long diffSolvis = solvisTimeCalendar.getTimeInMillis() - current;
		int diffSec = (int) (diffSolvis % 60000 + 60000) % 60000;
		long realAdjustTime = this.calculateNextAdjustmentTime(current, Constants.TIME_ADJUSTMENT_MS_N);
		long startAdjustTime = realAdjustTime;
		long lastStartAdjustTime = 0;
		long earliestStartAdjustTime = realAdjustTime;
		Calendar calendarProposal = Calendar.getInstance();
		Calendar calendarSolvisAdjust = Calendar.getInstance();
		Calendar calendarSolvisStart = Calendar.getInstance();
		int adjustmentTime = 0;
		long solvisAdjustTime = 0;
		for (int rep = 0; lastStartAdjustTime != startAdjustTime && rep < 10; ++rep) {
			adjustmentTime = 0;
			calendarProposal.setTimeInMillis(startAdjustTime);
			calendarSolvisStart.setTimeInMillis(startAdjustTime + diffSolvis);
			solvisAdjustTime = realAdjustTime - (diffSec > 30000 ? 60000 : 0);
			calendarSolvisAdjust.setTimeInMillis(solvisAdjustTime);
			for (DatePart part : this.dateParts) {
				int calendarInt = part.getCalendarInt();
				int diff = Math.abs(calendarSolvisStart.get(calendarInt) - calendarProposal.get(calendarInt));
				adjustmentTime += diff * singleSettingTime + part.touch.getSettingTime(solvis);
			}
			adjustmentTime = adjustmentTime + this.ok.getSettingTime(solvis) + solvis.getMaxResponseTime()
					+ 8 * this.screen.get(solvis.getConfigurationMask()).getTouchPoint().getSettingTime(solvis);
			adjustmentTime = adjustmentTime * Constants.TIME_ADJUSTMENT_PROPOSAL_FACTOR_PERCENT / 100;

			if (realAdjustTime - adjustmentTime <= current) {
				realAdjustTime = this.calculateNextAdjustmentTime(current, Constants.TIME_ADJUSTMENT_MS_N);
				rep = 0;
			}
			lastStartAdjustTime = startAdjustTime;
			startAdjustTime = realAdjustTime - adjustmentTime;

			if (earliestStartAdjustTime > startAdjustTime) {
				earliestStartAdjustTime = startAdjustTime;
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

		private String channelId;
		private String screenId;
		private String okScreenId;
		private List<DatePart> dateParts;
		private Rectangle secondsScan;
		private TouchPoint upper;
		private TouchPoint lower;
		private TouchPoint ok;

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
				case "channelId":
					this.channelId = value;
					break;
			}
		}

		@Override
		public ClockMonitor create() throws XmlError, IOException {
			return new ClockMonitor(channelId, screenId, okScreenId, secondsScan, dateParts, upper, lower, ok);
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
			}
			return null;
		}

		@Override
		public void created(CreatorByXML<?> creator, Object created) {
			switch (creator.getId()) {
				case XML_SECONDS_SCAN:
					this.secondsScan = (Rectangle) created;
					break ;
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
			}
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
			solvis.clearCurrentImage();
			MyImage image = solvis.getCurrentImage();
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

	public class Executable implements ObserverI<SolvisData> {

		private boolean timeAdjustmentRequestPending = false;
		private boolean fineAdjustmentRequestPending = false;
		private final Solvis solvis;
		private final ClockAdjustmentThread adjustmentThread;
		private final ClockData clockData = new ClockData();
		private final ClockAdjustment clockAdjustment;
		private final StrategyAdjust strategyAdjust = new StrategyAdjust();
		private final StrategyFineAdjust strategyFineAdjust = new StrategyFineAdjust();

		public Executable(Solvis solvis) {
			this.solvis = solvis;
			this.clockAdjustment = solvis.getUnit().getClockAdjustment();
			if (this.clockAdjustment.isEnable()) {
				this.adjustmentThread = new ClockAdjustmentThread();
				this.adjustmentThread.start();
				solvis.registerAbortObserver(new ObserverI<Boolean>() {

					@Override
					public void update(Boolean data, Object source) {
						adjustmentThread.abort();

					}
				});
			} else {
				this.adjustmentThread = null;

			}
		}

		@Override
		public void update(SolvisData data, Object source) {
			if (!data.getId().equals(channelId)) {
				return;
			}
			DateValue dateValue = (DateValue) data.getSingleData();
			this.clockData.current(dateValue);

			if (this.timeAdjustmentRequestPending) {
				return;
			}

			Calendar solvisDate = dateValue.get();
			long timeStamp = dateValue.getTimeStamp();
			if (timeStamp < CALENDAR_2018.getTimeInMillis()) {
				return;
			}

			int diff = (int) (solvisDate.getTimeInMillis() - timeStamp);

			if (Math.abs(diff) > 30500) {
				this.timeAdjustmentRequestPending = true;
				this.fineAdjustmentRequestPending = false;
				NextAdjust nextAdjust = calculateNextAdjustTime(dateValue, solvis);
				this.adjustmentThread.trigger(new CommandClock(strategyAdjust, nextAdjust));
				return;
			}

			if (this.fineAdjustmentRequestPending || clockAdjustment.getBurstLength() == 0
					|| (clockAdjustment.getFineLimitLower_ms() < diff && diff < clockAdjustment.getFineLimitUpper_ms())) {
				return;
			}
			int delta = diff;
			if (delta < 0) {
				delta = 30001 + delta;
			}

			int fineAdjusts = delta / clockAdjustment.getAproximatlySetAjust_ms();
			fineAdjusts = fineAdjusts == 0 ? 1 : fineAdjusts;
			int burstLength = clockAdjustment.getBurstLength();
			fineAdjusts = fineAdjusts > burstLength ? burstLength : fineAdjusts;

			long current = System.currentTimeMillis();
			long startAdjustTime = calculateNextAdjustmentTime(current, Constants.TIME_FINE_ADJUSTMENT_MS_N - diff)
					+ 1000;
			NextAdjust nextAdjust = new NextAdjust(fineAdjusts, startAdjustTime);

			this.fineAdjustmentRequestPending = true;
			this.adjustmentThread.trigger(new CommandClock(strategyFineAdjust, nextAdjust));
		}

		public class StrategyAdjust implements AdjustStrategy {

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
				long time = System.currentTimeMillis();
				int waitTime = (int) (nextAdjust.realAdjustTime - time);
				if (waitTime > 0) {
					AbortHelper.getInstance().sleep(waitTime);
					solvis.send(ok);
					solvis.clearMeasuredData();
					Screen screen = solvis.getCurrentScreen();
					if (screen != ClockMonitor.this.okScreen.get(configurationMask)) {
						success = false;
					}
				}
				timeAdjustmentRequestPending = false;
				fineAdjustmentRequestPending = false;

				if (success) {
					logger.info("Setting of the solvis clock successful.");
				} else {
					logger.error("Setting of the solvis clock not successful.");

				}
				clockData.clear();
				return success;
			}

		}

		public class StrategyFineAdjust implements AdjustStrategy {

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
						OcrRectangle ocr = new OcrRectangle(solvis.getCurrentImage(), secondsScan);
						String secondsString = ocr.getString();
						int seconds;
						try {
							seconds = Integer.parseInt(secondsString);
						} catch (NumberFormatException e) {
							logger.info("Fine tuning not successfull, will be repeated");
							continue;
						}
						if (seconds > 45) {
							AbortHelper.getInstance().sleep((61 - seconds) * 1000);
						}
						success = true;
						preparationSuccessfull = true ;
					}
					if (!clockAdjustScreen.goTo(solvis)) {
						logger.error("Fine tuning not successfull, will be repeated");
						success = false;
						break;
					}
					solvis.send(ok);
					if (solvis.getCurrentScreen() != okScreen) {
						logger.error("Fine tuning not successfull, will be repeated");
						success = false;
						break;
					}
				}
				fineAdjustmentRequestPending = false;
				clockData.clear();
				
				if (success) {
					logger.info("Fine adjustment of the solvis clock successful.");
				} else {
					logger.error("Fine adjustment of the solvis clock not successful.");

				}
				return success;
			}
		}

		public void abort() {
			this.adjustmentThread.abort();
		}

		private class ClockAdjustmentThread extends Thread {

			private int waitTime = -1;
			private CommandClock command = null;
			// private NextAdjust nextAdjust = null;
			// private int fineAdjustments = -1 ;
			private boolean abort = false;

			public ClockAdjustmentThread() {
				super("ClockAdjustmentThread ");
			}

			@Override
			public void run() {
				while (!this.abort) {
					try {
						boolean adjust;

						synchronized (this) {

							if (command != null && waitTime < 0) {
								NextAdjust nextAdjust = command.getNextAdjust();
								if (this.command.getNextAdjust() != null) {

									if (nextAdjust.realAdjustTime > 0) {
										logger.info("Next time adjust is scheduled to "
												+ DATE_FORMAT.format(new Date(nextAdjust.realAdjustTime))
												+ ", adjustment starts at "
												+ DATE_FORMAT.format(new Date(nextAdjust.startAdjustTime)) + ".");
									} else {
										logger.info("Next adjustment starts at "
												+ DATE_FORMAT.format(new Date(nextAdjust.startAdjustTime)) + ".");
									}

								}
								waitTime = (int) (command.getNextAdjust().startAdjustTime - System.currentTimeMillis());
								adjust = false;
							} else if (waitTime > 0) {
								this.waitTime = -1;
								adjust = true;
							} else {
								adjust = false;
							}
						}
						if (adjust) {
							solvis.execute(command);
						}
						if (!this.abort) {
							synchronized (this) {
								try {
									if (this.waitTime < 0) {
										this.wait();
									} else {
										this.wait(this.waitTime);
									}
								} catch (InterruptedException e) {
								}
							}
						}
					} catch (

					TerminationException e1) {
						this.abort = true;
					} catch (Throwable e) {
					}
				}
			}

			public synchronized void trigger(CommandClock command) {
				this.command = command;
				this.notifyAll();
			}

			public synchronized void abort() {
				this.abort = true;
				this.notifyAll();
			}

		}

		public class ClockData {
			private int accumulatedDelta_ms;
			private long accumulatedInterval_ms;
			private long startTimeStamp_ms;
			private long lastTimeStamp_ms;
			private long lastSolvis_ms;
			private int startDelta_ms;
			private AverageInt currentDelta = new AverageInt(
					Constants.AVERAGE_COUNT_SOLVIS_CLOCK_PRECISION_CALCULATION);

			public ClockData() {
				this.clear();
			}

			public void current(DateValue date) {
				long currentSolvis = date.get().getTimeInMillis();
				long timeStamp = date.getTimeStamp();

				int delta = (int) (currentSolvis - timeStamp);
				this.currentDelta.put(delta);

				if (this.startTimeStamp_ms > 0 && Math.abs(delta - this.lastSolvis_ms + this.lastTimeStamp_ms) > 2000) {
					// clock was adjusted
					this.accumulate();
				}

				long last = this.lastTimeStamp_ms;

				if (this.startTimeStamp_ms < 0) {
					if (currentDelta.isFilled()) {
						this.startTimeStamp_ms = timeStamp;
						this.lastTimeStamp_ms = timeStamp;
						this.startDelta_ms = currentDelta.get();
						this.lastSolvis_ms = currentSolvis;
					}
					return;
				}
				this.lastTimeStamp_ms = timeStamp;
				this.lastSolvis_ms = currentSolvis;

				Calendar lastCal = (Calendar) date.get().clone();
				lastCal.setTimeInMillis(last);

				Calendar currentCal = (Calendar) lastCal.clone();
				currentCal.setTimeInMillis(timeStamp);

				if (lastCal.get(Calendar.HOUR_OF_DAY) != currentCal.get(Calendar.HOUR_OF_DAY)) {
					logger.info("Divergence of solvis clock per day: " + this.getPrecision() + "ms");
				}
			}

			public void accumulate() {
				this.accumulatedDelta_ms += this.currentDelta.get() - this.startDelta_ms;
				this.accumulatedInterval_ms += lastTimeStamp_ms - startTimeStamp_ms;
				this.startTimeStamp_ms = -1;
				this.currentDelta.clear();
			}

			public int getPrecision() {
				long accumulatedDelta = this.accumulatedDelta_ms;
				long accumulatedInterval = this.accumulatedInterval_ms;
				accumulatedDelta += this.currentDelta.get() - this.startDelta_ms;
				accumulatedInterval += lastTimeStamp_ms - startTimeStamp_ms;

				return (int) (accumulatedDelta * 24 * 60 * 60 * 1000 / accumulatedInterval);
			}

			public void clear() {
				this.accumulatedDelta_ms = 0;
				this.accumulatedInterval_ms = 0;
				this.startTimeStamp_ms = -1;
				;
				this.currentDelta.clear();
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
				solvis.clearCurrentImage();
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

}