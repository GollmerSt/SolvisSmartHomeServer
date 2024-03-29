/************************************************************************
 * 
 * $Id$
 *
 * 
 ************************************************************************/

package de.sgollmer.solvismax.model.objects.measure;

import java.io.IOException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Collection;
import java.util.Iterator;

import de.sgollmer.solvismax.connection.SolvisConnection.SolvisMeasurements;
import de.sgollmer.solvismax.error.PowerOnException;
import de.sgollmer.solvismax.error.TypeException;
import de.sgollmer.solvismax.helper.SolvisDataHelper;
import de.sgollmer.solvismax.model.Solvis;
import de.sgollmer.solvismax.model.objects.IChannelSource.SetResult;
import de.sgollmer.solvismax.model.objects.ResultStatus;
import de.sgollmer.solvismax.model.objects.data.DateValue;
import de.sgollmer.solvismax.model.objects.data.IntegerValue;
import de.sgollmer.solvismax.model.objects.data.SingleData;
import de.sgollmer.solvismax.model.objects.data.SolvisData;
import de.sgollmer.solvismax.model.objects.data.StringData;
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
	public boolean get(final SolvisData destin, final Collection<Field> fields, final SolvisMeasurements data,
			final Solvis solvis) throws PowerOnException, IOException, NumberFormatException, TypeException {
		return this.type.get(destin, fields, data, solvis);
	}

	@Override
	public SetResult setDebugValue(final Solvis solvis, final SingleData<?> value) {
		SetResult setResult = this.type.setDebugValue(solvis, value);
		if (setResult == null) {
			setResult = new SetResult(ResultStatus.SUCCESS, value, false);
		}
		return setResult;
	}

	@Override
	public boolean isBoolean() {
		return this.type.isBoolean();
	}

	@Override
	public boolean validate(final Collection<Field> fields) {
		return this.type.validate(fields);
	}

	private static long toInt(final String data) throws NumberFormatException {

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

	private static Field getFirst(final Collection<Field> fields) {
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

	@Override
	public SingleData<?> interpretSetData(final SingleData<?> singleData, final int divisor) throws TypeException {
		return this.type.interpretSetData(singleData, divisor);
	}

	private static class Integer implements IType {
		private final boolean signed;

		private Integer(final boolean signed) {
			this.signed = signed;
		}

		@Override
		public boolean get(final SolvisData destin, final Collection<Field> fields, final SolvisMeasurements data,
				final Solvis solvis) throws PowerOnException, IOException, NumberFormatException, TypeException {
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
		public boolean validate(final Collection<Field> fields) {
			return fields.size() == 1;
		}

		@Override
		public boolean isNumeric() {
			return true;
		}

		@Override
		public SingleData<?> interpretSetData(final SingleData<?> singleData, final int divisor) throws TypeException {
			SingleData<?> value = SolvisDataHelper.toValue(singleData);

			if (value != null) {
				return new IntegerValue((int) Math.round(value.getDouble() * divisor), -1l);
			} else {
				return null;
			}
		}

		@Override
		public SetResult setDebugValue(Solvis solvis, SingleData<?> value) {
			return null;
		}

	}

	private static class Boolean implements IType {

		@Override
		public boolean get(SolvisData destin, Collection<Field> fields, SolvisMeasurements data,
				de.sgollmer.solvismax.model.Solvis solvis)
				throws PowerOnException, IOException, NumberFormatException, TypeException {
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

		@Override
		public SingleData<?> interpretSetData(final SingleData<?> singleData, final int divisor) throws TypeException {
			return SolvisDataHelper.toBoolean(singleData);
		}

		@Override
		public SetResult setDebugValue(Solvis solvis, SingleData<?> value) {
			return null;
		}

	}

	private static class Date implements IType {

		private static final String DATE_FORMAT_STRING = "yyyy-MM-dd HH:mm:ss";
		private static final SimpleDateFormat DATE_FORMAT = new SimpleDateFormat(DATE_FORMAT_STRING);

		@Override
		public boolean get(SolvisData destin, Collection<Field> fields, SolvisMeasurements data,
				de.sgollmer.solvismax.model.Solvis solvis)
				throws PowerOnException, IOException, NumberFormatException, TypeException {
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
			calendar.set(Calendar.MILLISECOND, 0);
			calendar.set(year, month, date, hour, minute, second);

			Calendar old = destin.getDate();

			int measurementsInterval = destin.getScanInterval_ms();

			if (old != null && measurementsInterval >= 2000 && (calendar.getTimeInMillis() == old.getTimeInMillis())) {
				solvis.getSolvisState().setSolvisClockValid(false);
				throw new PowerOnException("Solvis time not changed");
			} else {
				solvis.getSolvisState().setSolvisClockValid(true);
			}

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

		@Override
		public SingleData<?> interpretSetData(final SingleData<?> singleData, final int divisor) throws TypeException {
			if (singleData instanceof StringData) {
				java.util.Date date = null;
				try {
					date = ((SimpleDateFormat) DATE_FORMAT.clone()).parse(singleData.toString());
				} catch (ParseException e) {

				}
				if (date == null) {
					throw new TypeException("Format error, the Date format must be \"" + DATE_FORMAT_STRING + "\".");
				}
				Calendar calendar = Calendar.getInstance();
				calendar.setTime(date);

				return new DateValue(calendar, singleData.getTimeStamp());
			}
			return null;
		}

		@Override
		public SetResult setDebugValue(Solvis solvis, SingleData<?> value) {
			return null;
		}
	}

}
