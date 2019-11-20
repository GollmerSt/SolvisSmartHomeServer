package de.sgollmer.solvismax.model.objects.data;

import java.util.Calendar;
import java.util.Collection;

public class DateValue implements SingleData {
	private final Calendar calendar ;
	
	public DateValue( Calendar calendar ) {
		this.calendar = calendar ;
	}

	@Override
	public SingleData average(Collection<SingleData> values) {
		return null;
	}

	public Calendar getCalendar() {
		return calendar;
	}
	

}
