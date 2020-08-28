/************************************************************************
 * 
 * $Id$
 *
 * 
 ************************************************************************/

package de.sgollmer.solvismax.model.objects.measure;

import java.io.IOException;
import java.util.Calendar;
import java.util.Collection;
import java.util.Iterator;

import de.sgollmer.solvismax.connection.SolvisConnection.SolvisMeasurements;
import de.sgollmer.solvismax.error.PowerOnException;
import de.sgollmer.solvismax.model.objects.data.SolvisData;
import de.sgollmer.solvismax.model.objects.measure.Measurement.IType;
import de.sgollmer.solvismax.objects.Field;

public enum Strategy implements IType {

	DATE(new Date()), UNSIGNED(new Integer(false)), SIGNED(new Integer(true)), BOOLEAN(new Boolean());

	// private static final Logger logger = LogManager.getLogger(Strategy.class);

	private final IType type;

	private Strategy(IType type) {
		this.type = type;
	}

	@Override
	public boolean get(SolvisData destin, Collection<Field> fields, SolvisMeasurements data) throws PowerOnException, IOException {
		return this.type.get(destin, fields, data);
	}

	@Override
	public boolean isBoolean() {
		return this.type.isBoolean();
	}

	@Override
	public boolean validate(Collection<Field> fields) {
		return this.type.validate(fields);
	}

	private static Long toInt(String data) {

		long result = 0;

		for (int i = data.length() / 2 - 1; i >= 0 / 2; --i) {

			char c = data.charAt(2 * i);
			int b = Character.digit(c, 16);
			if ( b < 0 ) {
				return null;
			}
			c = data.charAt(2 * i + 1);
			if (c < 0 ) {
				return null ;
			}
			b = b * 16 + Character.digit(c, 16);

			result = result << 8 | b;
		}
		return result;
	}

	private static long getValue( String string ) throws IOException {
		Long value = toInt(string);
		if ( value == null ) {
			throw new IOException("Unexpected characters in Solvis XML String"); 
		}
		return value;
	}

	private static Field getFirst(Collection<Field> fields) {
		Iterator<Field> it = fields.iterator();
		if (it.hasNext()) {
			return it.next();
		} else {
			return null;
		}
	}

	@Override
	public boolean isNumeric() {
		return this.type.isNumeric();
	}

	private static class Integer implements IType {
		private final boolean signed;

		private Integer(boolean signed) {
			this.signed = signed;
		}

		@Override
		public boolean get(SolvisData destin, Collection<Field> fields, SolvisMeasurements data)
				throws PowerOnException, IOException {
			Field field = getFirst(fields);
			String sub = field.subString(data.getHexString());
			long result = getValue(sub);
			if (this.signed) {
				long threshold = 1 << (4 * field.getLength() - 1);
				if (result >= threshold) {
					result -= threshold * 2;
				}
			}
			destin.setInteger((int) result, data.getTimeStamp());
			return true;
		}

		@Override
		public boolean isBoolean() {
			return false;
		}

		@Override
		public boolean validate(Collection<Field> fields) {
			return fields.size() == 1;
		}

		@Override
		public boolean isNumeric() {
			return true;
		}

	}
	
	private static class Boolean implements IType {

		@Override
		public boolean get(SolvisData destin, Collection<Field> fields, SolvisMeasurements data)
				throws PowerOnException, IOException {
			Field field = getFirst(fields);
			String sub = field.subString(data.getHexString());
			boolean result = getValue(sub) > 0;
			destin.setBoolean(result, data.getTimeStamp());
			return true;
		}

		@Override
		public boolean isBoolean() {
			return true;
		}

		@Override
		public boolean validate(Collection<Field> fields) {
			return fields.size() == 1;
		}

		@Override
		public boolean isNumeric() {
			return false;
		}

	}

	private static class Date implements IType {

		@Override
		public boolean get(SolvisData destin, Collection<Field> fields, SolvisMeasurements data) throws IOException {
			String str = "";
			for (Iterator<Field> it = fields.iterator(); it.hasNext();) {
				str += it.next().subString(data.getHexString());
			}
			int second = (int) getValue(str.substring(4, 6));
			int minute = (int) getValue(str.substring(2, 4));
			int hour = (int) getValue(str.substring(0, 2));

			int year = (int) getValue(str.substring(6, 8)) + 2000;
			int month = (int) getValue(str.substring(8, 10)) - 1;
			int date = (int) getValue(str.substring(10, 12));

			Calendar calendar = Calendar.getInstance();
			calendar.set(year, month, date, hour, minute, second);

			destin.setDate(calendar, data.getTimeStamp());

			return true;
		}

		@Override
		public boolean isBoolean() {
			return false;
		}

		@Override
		public boolean validate(Collection<Field> fields) {
			int length = 0;
			for (Iterator<Field> it = fields.iterator(); it.hasNext();) {
				length += it.next().getLength();
			}
			return length == 12;
		}

		@Override
		public boolean isNumeric() {
			return false;
		}
	}

}
