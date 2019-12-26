package de.sgollmer.solvismax.model.objects;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;

import javax.xml.namespace.QName;

import de.sgollmer.solvismax.Constants;
import de.sgollmer.solvismax.error.TerminationException;
import de.sgollmer.solvismax.error.XmlError;
import de.sgollmer.solvismax.model.Solvis;
import de.sgollmer.solvismax.objects.Rectangle;
import de.sgollmer.solvismax.xml.BaseCreator;
import de.sgollmer.solvismax.xml.CreatorByXML;

public class Clock implements Assigner {

	private static final String XML_YEAR = "Year";
	private static final String XML_MONTH = "Month";
	private static final String XML_DAY = "Day";
	private static final String XML_HOUR = "Hour";
	private static final String XML_MINUTE = "Minute";
	private static final String XML_UPPER = "Upper";
	private static final String XML_LOWER = "Lower";
	private static final String XML_OK = "Ok";

	private final String screenId;
	private final List<DatePart> dateParts;
	private final TouchPoint upper;
	private final TouchPoint lower;
	private final TouchPoint ok;

	private OfConfigs<Screen> screen = null;

	public Clock(String screenId, List<DatePart> dateParts, TouchPoint upper, TouchPoint lower, TouchPoint ok) {
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
	}

	public void syncRequest(Calendar solvisTimeCalendar) {
		long time = System.currentTimeMillis();
		if (Math.abs(time - solvisTimeCalendar.getTimeInMillis()) < Constants.MIN_TIME_ERROR_ADJUSTMENT_S) {
			return;
		}

	}

	private int calculateMaxSettingTime(Calendar solvisTimeCalendar) {
		int singleSettingTime = Math.max(this.upper.getSettingTime(), this.lower.getSettingTime());
		Calendar now = Calendar.getInstance();
		int result = 0 ;
		Calendar cal1, cal2 ;
		if ( solvisTimeCalendar.before(now)) {
			cal1 = solvisTimeCalendar ;
			cal2 = now ;
		} else {
			cal1 = now ;
			cal2 = solvisTimeCalendar ;
		}
//		for ( DatePart part : this.dateParts ) {
//			diff = 
//		}
//		int result = Math.abs(solvisTimeCalendar.get(Calendar.YEAR) - now.get(Calendar.YEAR)) + 1;
//		result += 12 + 31;
//		result += 24 + 60;
		return -1;
	}

	public static class Creator extends CreatorByXML<Clock> {

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
			}
		}

		@Override
		public Clock create() throws XmlError, IOException {
			return new Clock(screenId, dateParts, upper, lower, ok);
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
			}
		}
	}

	private static class DatePart {

		private static final String XML_RECTANGLE = "Rectangle";
		private static final String XML_TOUCH = "Touch";
		private static final String XML_SCREEN_GRAFIC = "ScreenGrafic";

		private final Integer least;
		private final Rectangle rectangle;
		private final TouchPoint touch;
		private final ScreenGraficDescription screenGrafic;
		private final int calendarInt;
		private final int calendarOrigin;
		private final int solvisOrigin;

		public DatePart(Integer least, Rectangle rectangle, TouchPoint touch, ScreenGraficDescription screenGrafic,
				int calendarInt, int calendarOrigin, int solvisOrigin) {
			this.least = least;
			this.rectangle = rectangle;
			this.touch = touch;
			this.screenGrafic = screenGrafic;
			this.calendarInt = calendarInt;
			this.calendarOrigin = calendarOrigin;
			this.solvisOrigin = solvisOrigin;
		}

		public static class Creator extends CreatorByXML<DatePart> {

			private Integer least = null;
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
				switch (name.getLocalPart()) {
					case "least":
						this.least = Integer.parseInt(value);
						break;
				}

			}

			@Override
			public DatePart create() throws XmlError, IOException {
				return new DatePart(least, rectangle, touch, screenGrafic, calendarInt, calendarOrigin, solvisOrigin);
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
	}

}
