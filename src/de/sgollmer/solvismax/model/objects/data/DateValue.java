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
import de.sgollmer.solvismax.error.TypeException;
import de.sgollmer.solvismax.helper.Helper;

public class DateValue extends SingleData<Calendar> {
	private static final SimpleDateFormat formater = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");

	private final Calendar calendar;

	public DateValue(final Calendar calendar, final long timeStamp) {
		super(timeStamp);
		this.calendar = calendar;
	}

	@Override
	public Helper.Boolean getBoolean() throws TypeException {
		throw new TypeException(this.getClass().toString() + " can't be converted to Boolean");
	}

	@Override
	public Integer getInt() throws TypeException {
		throw new TypeException(this.getClass().toString() + " can't be converted to Integer");
	}

	@Override
	public Long getLong() throws TypeException {
		throw new TypeException(this.getClass().toString() + " can't be converted to Long");
	}

	@Override
	public Double getDouble() throws TypeException {
		throw new TypeException(this.getClass().toString() + " can't be converted to Double");
	}

	@Override
	public SingleData<Calendar> create(final int value, final long timeStamp) {
		return null;
	}

	@Override
	public String toString() {
		Date date = new Date(this.calendar.getTimeInMillis());
		return ((DateFormat) formater.clone()).format(date);
	}

	@Override
	public boolean equals(final Object obj) {
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
	public int compareTo(final SingleData<?> o) {
		if (o instanceof DateValue) {
			return this.calendar.compareTo(((DateValue) o).calendar);
		} else if (o != null) {
			return this.getClass().getCanonicalName().compareTo(o.getClass().getCanonicalName());
		} else {
			return 1;
		}
	}

	@Override
	public SingleData<Calendar> clone(final long timeStamp) {
		return new DateValue(this.calendar, timeStamp);
	}

	@Override
	public SingleData<Calendar> add(SingleData<?> data) throws TypeException {
		throw new TypeException("not supported");
	}

	@Override
	public SingleData<Calendar> mult(SingleData<?> data) throws TypeException {
		throw new TypeException("not supported");
	}

	@Override
	public boolean isNumeric() {
		return false;
	}
}
