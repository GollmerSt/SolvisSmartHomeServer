package de.sgollmer.solvismax.model.objects.data;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;

public class DateValue implements SingleData {
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
	public SingleData create(int value) {
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


}
