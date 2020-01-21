/************************************************************************
 * 
 * $Id$
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
	private final long timeStamp ;

	public DateValue(Calendar calendar, long timeStamp ) {
		this.calendar = calendar;
		this.timeStamp = timeStamp ;
	}

	public Calendar getCalendar() {
		return calendar;
	}
	
	public long getTimeStamp() {
		return timeStamp;
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
