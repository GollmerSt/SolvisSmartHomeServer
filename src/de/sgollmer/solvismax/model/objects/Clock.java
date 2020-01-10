/************************************************************************
 * 
 * $Id$
 *
 * 
 ************************************************************************/

package de.sgollmer.solvismax.model.objects;

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
import de.sgollmer.solvismax.imagepatternrecognition.image.MyImage;
import de.sgollmer.solvismax.imagepatternrecognition.ocr.OcrRectangle;
import de.sgollmer.solvismax.model.CommandClock;
import de.sgollmer.solvismax.model.Solvis;
import de.sgollmer.solvismax.model.objects.Observer.ObserverI;
import de.sgollmer.solvismax.model.objects.data.SolvisData;
import de.sgollmer.solvismax.objects.Rectangle;
import de.sgollmer.solvismax.xml.BaseCreator;
import de.sgollmer.solvismax.xml.CreatorByXML;

public class Clock implements Assigner, GraficsLearnable {

	private static final Logger logger = LogManager.getLogger(Clock.class);
	private static final Level LEARN = Level.getLevel("LEARN");
	private static final Calendar CALENDAR_2018 ;

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
	private final List<DatePart> dateParts;
	private final TouchPoint upper;
	private final TouchPoint lower;
	private final TouchPoint ok;

	private OfConfigs<Screen> screen = null;
	
	static {
		CALENDAR_2018 = Calendar.getInstance() ;
		CALENDAR_2018.set(2018, 0, 1, 0, 0, 0);
		CALENDAR_2018.set(Calendar.MILLISECOND, 0);
	}

	public Clock(String channelId, String screenId, List<DatePart> dateParts, TouchPoint upper, TouchPoint lower,
			TouchPoint ok) {
		this.channelId = channelId;
		this.screenId = screenId;
		this.dateParts = dateParts;
		this.upper = upper;
		this.lower = lower;
		this.ok = ok;
	}

	public boolean set(Solvis solvis) throws IOException, TerminationException {
		boolean result = solvis.gotoScreen(this.screen.get(solvis.getConfigurationMask()));
		if (!result) {
			return false;
		}

		return true;
	}

	@Override
	public void assign(SolvisDescription description) {
		this.screen = description.getScreens().get(screenId);
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

		public NextAdjust(long solvisAdjustTime, long realAdjustTime, long startAdjustTime) {
			this.solvisAdjustTime = solvisAdjustTime;
			this.realAdjustTime = realAdjustTime;
			this.startAdjustTime = startAdjustTime;
		}
	}

	private NextAdjust calculateNextAdjustTime(Calendar solvisTimeCalendar, Solvis solvis) {
		long now = System.currentTimeMillis();
		int singleSettingTime = Math.max(this.upper.getSettingTime(solvis), this.lower.getSettingTime(solvis))
				+ solvis.getMaxResponseTime();
		long diffSolvis = solvisTimeCalendar.getTimeInMillis() - now;
		int diffSec = (int) (diffSolvis % 60000 + 60000) % 60000;
		long realAdjustTime = this.calculateNext(now);
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

			if (realAdjustTime - adjustmentTime - now <= 0) {
				realAdjustTime = this.calculateNext(realAdjustTime);
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

	public Executable instantiate(Solvis solvis) {

		Executable executable = new Executable(solvis);
		solvis.registerObserver(executable);
		return executable;
	}

	public static class Creator extends CreatorByXML<Clock> {

		private String channelId;
		private String screenId;
		private List<DatePart> dateParts;
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
				case "channelId":
					this.channelId = value;
					break;
			}
		}

		@Override
		public Clock create() throws XmlError, IOException {
			return new Clock(channelId, screenId, dateParts, upper, lower, ok);
		}

		@Override
		public CreatorByXML<?> getCreator(QName name) {
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
		private final Solvis solvis;
		private final ClockAdjustmentThread adjustmentThread;

		public Executable(Solvis solvis) {
			this.solvis = solvis;
			this.adjustmentThread = new ClockAdjustmentThread(this);
			this.adjustmentThread.start();
			solvis.registerAbortObserver(new ObserverI<Boolean>() {

				@Override
				public void update(Boolean data, Object source) {
					adjustmentThread.abort();

				}
			});
		}

		@Override
		public void update(SolvisData data, Object source) {
			if (!data.getId().equals(channelId) || this.timeAdjustmentRequestPending) {
				return;
			}

			Calendar solvisDate = (Calendar) data.getSingleData().get();
			long now = System.currentTimeMillis();
			if ( now < CALENDAR_2018.getTimeInMillis() ) {
				return ;
			}

			int diff = (int) Math.abs(solvisDate.getTimeInMillis() - now);
			if (diff > 30000) {
				this.timeAdjustmentRequestPending = true;
				this.adjustmentThread.trigger(calculateNextAdjustTime(solvisDate, solvis));
			}
		}

		public boolean adjust(NextAdjust nextAdjust) throws IOException, TerminationException {
			int configurationMask = solvis.getConfigurationMask();
			Calendar adjustementCalendar = Calendar.getInstance();
			adjustementCalendar.setTimeInMillis(nextAdjust.solvisAdjustTime);
			boolean success = false;
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
							solvis.gotoScreen(screen.get(configurationMask));
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
				if (screen != Clock.this.screen.get(configurationMask).getPreviousScreen(configurationMask)) {
					success = false;
				}
			}
			this.timeAdjustmentRequestPending = false;

			if (success) {
				logger.info("Setting of the solvis clock successful.");
			} else {
				logger.error("Setting of the solvis clock not successful.");

			}
			return success;
		}

		public void abort() {
			this.adjustmentThread.abort();
		}

		public void execute(NextAdjust nextAdjust) {
			CommandClock command = new CommandClock(this, nextAdjust);
			solvis.execute(command);
		}
	}

	private class ClockAdjustmentThread extends Thread {

		private int waitTime = -1;
		private NextAdjust nextAdjust = null;
		private boolean abort = false;
		private final Executable executable;

		public ClockAdjustmentThread(Executable executable) {
			super("ClockAdjustmentThread ");
			this.executable = executable;
		}

		@Override
		public void run() {
			while (!this.abort) {
				try {
					boolean adjust = false;

					synchronized (this) {

						if (this.nextAdjust != null && waitTime < 0) {
							logger.info("Next time adjust is scheduled to "
									+ DATE_FORMAT.format(new Date(nextAdjust.realAdjustTime))
									+ ", adjustment starts at "
									+ DATE_FORMAT.format(new Date(nextAdjust.startAdjustTime)) + ".");
							long time = System.currentTimeMillis();
							this.waitTime = (int) (this.nextAdjust.startAdjustTime - time);
							adjust = false;
						} else if (waitTime > 0) {
							this.waitTime = -1;
							adjust = true;
						}
					}
					if (adjust) {
						executable.execute(this.nextAdjust);
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
				} catch (TerminationException e1) {
					this.abort = true;
				} catch (Throwable e) {
				}
			}
		}

		public synchronized void trigger(NextAdjust nextAdjust) {
			this.nextAdjust = nextAdjust;
			this.notifyAll();
		}

		public synchronized void abort() {
			this.abort = true;
			this.notifyAll();
		}

	}

	private long calculateNext(long time) {
		int delta = Constants.TIME_ADJUSTMENT_MINUTE_N * 60 * 1000;
		long next = time / delta * delta + delta;
		if (next % 3600000 == 0) {
			next += delta;
		}
		return next;
	}

	@Override
	public void learn(Solvis solvis) throws IOException, LearningError {
		solvis.gotoScreen(this.screen.get(solvis.getConfigurationMask()));
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