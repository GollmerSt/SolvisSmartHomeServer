package de.sgollmer.solvismax.model.objects.measure;

import java.util.Calendar;
import java.util.Collection;
import java.util.Iterator;

import de.sgollmer.solvismax.error.ErrorPowerOn;
import de.sgollmer.solvismax.error.FieldError;
import de.sgollmer.solvismax.model.objects.data.SolvisData;
import de.sgollmer.solvismax.objects.Field;

public enum Strategy {
	DATE(new Date()), UNSIGNED(new Integer(false)), SIGNED(new Integer(true)), BOOLEAN(new Boolean());

	private static String NULL_STRING = new String("0000000000000000000000000000000000000000000000000000000000000000");

	private final StrategyClass type;

	private Strategy(StrategyClass type) {
		this.type = type;
	}

	public boolean get(SolvisData destin, Collection<Field> fields, String data) throws ErrorPowerOn {
		return type.get(destin, fields, data);
	}

	private interface StrategyClass {
		public boolean get(SolvisData destin, Collection<Field> fields, String data) throws ErrorPowerOn;
	}

	private static class Integer implements StrategyClass {
		private final boolean signed;

		public Integer(boolean signed) {
			this.signed = signed;
		}

		@Override
		public boolean get(SolvisData destin, Collection<Field> fields, String data) throws ErrorPowerOn {
			Field field = getFirst(fields);
			if (field == null) {
				throw new FieldError("Field <std> unknown");
			}
			String sub = field.subString(data);
			long result = toInt(sub);
			if (signed) {
				long threshold = 1 << (4 * field.getLength() - 1);
				if (result >= threshold) {
					result -= threshold * 2;
				}
			}
			destin.setInteger((int) result);
			return true;
		}

	}

	private static class Date implements StrategyClass {

		@Override
		public boolean get(SolvisData destin, Collection<Field> fields, String data) {
			String str = "";
			for (Iterator<Field> it = fields.iterator(); it.hasNext();) {
				str += it.next().subString(data);
			}
			if (NULL_STRING.contains(str)) {
				throw new ErrorPowerOn("Power on detected");
			}
			int second = (int) toInt(str.substring(4, 6));
			int minute = (int) toInt(str.substring(2, 4));
			int hour = (int) toInt(str.substring(0, 2));

			int year = (int) toInt(str.substring(6, 8)) + 2000;
			int month = (int) toInt(str.substring(8, 10)) -1;
			int date = (int) toInt(str.substring(10, 12));

			Calendar calendar = Calendar.getInstance();
			calendar.set(year, month, date, hour, minute, second);

			destin.setDate(calendar);

			return true;
		}
	}

	private static class Boolean implements StrategyClass {

		@Override
		public boolean get(SolvisData destin, Collection<Field> fields, String data) throws ErrorPowerOn {
			Field field = getFirst(fields);
			if (field == null) {
				throw new FieldError("Field <std> unknown");
			}
			String sub = field.subString(data);
			boolean result = toInt(sub) > 0;
			destin.setBoolean(result);
			return true;
		}

	}

	private static long toInt(String data) {

		long result = 0;

		for (int i = data.length() / 2-1; i >= 0 / 2; --i) {

			char c = data.charAt(2*i);
			int b = Character.digit(c, 16);
			c = data.charAt(2*i + 1);
			b = b * 16 + Character.digit(c, 16);

			result = result * 256 + b;
		}
		return result;
	}

	private static Field getFirst(Collection<Field> fields) {
		Iterator<Field> it = fields.iterator();
		if (it.hasNext()) {
			return it.next();
		} else {
			return null;
		}
	}

}
