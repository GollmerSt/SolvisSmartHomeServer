/************************************************************************
 * 
 * $Id: 73_SolvisClient.pm 78 2020-01-03 17:50:08Z stefa $
 *
 * 
 ************************************************************************/

package de.sgollmer.solvismax.model.objects.data;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;

import de.sgollmer.solvismax.model.objects.backup.Measurement;

public class DateValue implements SingleData<Calendar> {
	private static final SimpleDateFormat formater = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");

	private final Calendar calendar;

	public DateValue(Calendar calendar) {
		this.calendar = calendar;
	}

	public Calendar getCalendar() {
		return calendar;
	}

	@Override
	public Integer getInt() {
		return null;
	}

	@Override
	public SingleData<Calendar> create(int value) {
		return null;
	}

	@Override
	public String toString() {
		Date date = new Date( this.calendar.getTimeInMillis() );
		return formater.format(date);
	}
	
	@Override
	public boolean equals( Object obj ) {
		if ( obj instanceof DateValue ) {
			return this.calendar.equals(((DateValue)obj).calendar) ;
		}
		return false ;
	}

	@Override
	public int hashCode() {
		return this.calendar.hashCode() ;
	}

	@Override
	public String getXmlId() {
		return Measurement.XML_MEASUREMENT_STRING;
	}

	@Override
	public String toJson() {
		return "\"" + this.toString() + "\"";
	}

	@Override
	public Calendar get() {
		return this.calendar;
	}
	
	

}
