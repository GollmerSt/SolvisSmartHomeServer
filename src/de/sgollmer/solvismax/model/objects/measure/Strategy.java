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
	public boolean get(SolvisData destin, Collection<Field> fields, SolvisMeasurements data,
			de.sgollmer.solvismax.model.Solvis solvis) throws PowerOnException, IOException, NumberFormatException {
		return this.type.get(destin, fields, data, solvis);
	}

	@Override
	public boolean isBoolean() {
		return this.type.isBoolean();
	}

	@Override
	public boolean validate(Collection<Field> fields) {
		return this.type.validate(fields);
	}

	private static long toInt(String data) throws NumberFormatException {

		Long result = 0L;

		for (int i = data.length() / 2 - 1; i >= 0 / 2; --i) {

			char c = data.charAt(2 * i);
			int b16 = Character.digit(c, 16);
			if (b16 < 0) {
				result = null;
				break;
			}
			c = data.charAt(2 * i + 1);
			int b1 = Character.digit(c, 16);
			if (b1 < 0) {
				result = null;
				break;
			}
			int b = b16 * 16 + b1;

			result = result << 8 | b;
		}
		if (result == null) {
			throw new NumberFormatException("Unexpected characters in Solvis XML String <" + data + ">.");
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
		public boolean get(SolvisData destin, Collection<Field> fields, SolvisMeasurements data,
				de.sgollmer.solvismax.model.Solvis solvis) throws PowerOnException, IOException, NumberFormatException {
			Field field = getFirst(fields);
			String sub = field.subString(data.getHexString());
			long result = toInt(sub);
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
		public boolean get(SolvisData destin, Collection<Field> fields, SolvisMeasurements data,
				de.sgollmer.solvismax.model.Solvis solvis) throws PowerOnException, IOException, NumberFormatException {
			Field field = getFirst(fields);
			String sub = field.subString(data.getHexString());
			boolean result = toInt(sub) > 0;
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
		public boolean get(SolvisData destin, Collection<Field> fields, SolvisMeasurements data,
				de.sgollmer.solvismax.model.Solvis solvis) throws PowerOnException, IOException, NumberFormatException {
			String str = "";
			for (Iterator<Field> it = fields.iterator(); it.hasNext();) {
				str += it.next().subString(data.getHexString());
			}
			int second = (int) toInt(str.substring(4, 6));
			int minute = (int) toInt(str.substring(2, 4));
			int hour = (int) toInt(str.substring(0, 2));

			int year = (int) toInt(str.substring(6, 8)) + 2000;
			int month = (int) toInt(str.substring(8, 10)) - 1;
			int date = (int) toInt(str.substring(10, 12));

			Calendar calendar = Calendar.getInstance();
			calendar.set(year, month, date, hour, minute, second);

//			Calendar old = destin.getDate();
//			int measurementsInterval = solvis.getDefaultReadMeasurementsInterval_ms();
//
//			if (old != null && measurementsInterval >= 2000 && (calendar.getTimeInMillis() == old.getTimeInMillis() )) {
//				solvis.getSolvisState().setRemoteConnected();
//				throw new PowerOnException("Solvis time not changed");
//			}
//
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
