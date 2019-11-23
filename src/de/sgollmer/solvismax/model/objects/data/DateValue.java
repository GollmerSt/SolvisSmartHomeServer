package de.sgollmer.solvismax.model.objects.data;

import java.util.Calendar;

public class DateValue implements SingleData {
	private final Calendar calendar ;
	
	public DateValue( Calendar calendar ) {
		this.calendar = calendar ;
	}

	public Calendar getCalendar() {
		return calendar;
	}

	@Override
	public Integer getInt() {
		return null;
	}

	@Override
	public SingleData create(long divisor, int divident) {
		return null;
	}
	

}
