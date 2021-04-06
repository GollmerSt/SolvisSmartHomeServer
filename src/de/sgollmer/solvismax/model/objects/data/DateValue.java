/************************************************************************
 * 
 * $Id$
 *
 * 
 ************************************************************************/

package de.sgollmer.solvismax.model.objects.data;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;

import de.sgollmer.solvismax.Constants;
import de.sgollmer.solvismax.helper.Helper;

public class DateValue extends SingleData<Calendar> {
	private static final SimpleDateFormat formater = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");

	private final Calendar calendar;

	DateValue(Calendar calendar, long timeStamp) {
		super(timeStamp);
		this.calendar = calendar;
	}

	@Override
	public Helper.Boolean getBoolean() {
		return Helper.Boolean.UNDEFINED;
	}

	@Override
	public Integer getInt() {
		return null;
	}

	@Override
	public Long getLong() {
		return null;
	}

	@Override
	public Double getDouble() {
		return null;
	}

	@Override
	public SingleData<Calendar> create(int value, long timeStamp) {
		return null;
	}

	@Override
	public String toString() {
		Date date = new Date(this.calendar.getTimeInMillis());
		return ((DateFormat) formater.clone()).format(date);
	}

	@Override
	public boolean equals(Object obj) {
		if (obj instanceof DateValue) {
			return this.calendar.equals(((DateValue) obj).calendar);
		}
		return false;
	}

	@Override
	public int hashCode() {
		return this.calendar.hashCode();
	}

	@Override
	public String getXmlId() {
		return Constants.XmlStrings.XML_MEASUREMENT_STRING;
	}

	@Override
	public String toJson() {
		return "\"" + this.toString() + "\"";
	}

	@Override
	public Calendar get() {
		return this.calendar;
	}

	@Override
	public int compareTo(SingleData<?> o) {
		if (o instanceof DateValue) {
			return this.calendar.compareTo(((DateValue) o).calendar);
		} else if (o != null) {
			return this.getClass().getCanonicalName().compareTo(o.getClass().getCanonicalName());
		} else {
			return 1;
		}
	}

	@Override
	public SingleData<Calendar> create(long timeStamp) {
		return new DateValue(this.calendar, timeStamp);
	}
}
