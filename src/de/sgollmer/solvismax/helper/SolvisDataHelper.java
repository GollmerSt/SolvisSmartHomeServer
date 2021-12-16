package de.sgollmer.solvismax.helper;

import java.util.Collection;

import de.sgollmer.solvismax.error.TypeException;
import de.sgollmer.solvismax.model.objects.data.BooleanValue;
import de.sgollmer.solvismax.model.objects.data.DoubleValue;
import de.sgollmer.solvismax.model.objects.data.IMode;
import de.sgollmer.solvismax.model.objects.data.IntegerValue;
import de.sgollmer.solvismax.model.objects.data.ModeValue;
import de.sgollmer.solvismax.model.objects.data.SingleData;
import de.sgollmer.solvismax.model.objects.data.StringData;

public class SolvisDataHelper {

	public static BooleanValue toBoolean(final SingleData<?> singleData) {

		if (singleData instanceof BooleanValue) {
			return (BooleanValue) singleData;
		} else if (singleData instanceof StringData) {
			String data = ((StringData) singleData).get();
			if (data.equalsIgnoreCase("true") || data.equalsIgnoreCase("false")) {
				return new BooleanValue(data.equalsIgnoreCase("true"), singleData.getTimeStamp());
			}
			if (data.equalsIgnoreCase("on") || data.equalsIgnoreCase("off")) {
				return new BooleanValue(data.equalsIgnoreCase("on"), singleData.getTimeStamp());
			}
		}
		return null;
	}

	@SuppressWarnings({ "unchecked", "rawtypes" })
	public static SingleData<?> toMode(final String value, final long timeStamp, final IMode<?>[] modes)
			throws TypeException {
		IMode<?> setMode = null;
		for (IMode<?> mode : modes) {
			if (mode.getName().equals(value)) {
				setMode = mode;
				break;
			}
		}
		if (setMode == null) {
			throw new TypeException("Mode <" + value + "> is unknown");
		}
		return new ModeValue(setMode, timeStamp);
	}

	@SuppressWarnings({ "unchecked", "rawtypes" })
	public static SingleData<?> toMode(final String value, final long timeStamp, final Collection<IMode<?>> modes)
			throws TypeException {
		IMode<?> setMode = null;
		for (IMode<?> mode : modes) {
			if (mode.getName().equals(value)) {
				setMode = mode;
				break;
			}
		}
		if (setMode == null) {
			throw new TypeException("Mode <" + value + "> is unknown");
		}
		return new ModeValue(setMode, timeStamp);
	}

	public static SingleData<?> toValue(final SingleData<?> singleData) throws TypeException {
		if (singleData instanceof StringData) {
			String string = (String) singleData.get();
			try {
				if (string.contains(".")) {
					return new DoubleValue(Double.parseDouble(string), singleData.getTimeStamp());
				} else {
					return new IntegerValue(Integer.parseInt(string), singleData.getTimeStamp());
				}
			} catch (NumberFormatException e) {
				throw new TypeException(e);
			}
		} else if (singleData.isNumeric()) {
			return singleData;
		} else {
			return null;
		}

	}

}
