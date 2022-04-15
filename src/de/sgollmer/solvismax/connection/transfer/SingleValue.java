/************************************************************************
 * 
 * $Id$
 *
 * 
 ************************************************************************/

package de.sgollmer.solvismax.connection.transfer;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import de.sgollmer.solvismax.error.JsonException;
import de.sgollmer.solvismax.error.PackageException;
import de.sgollmer.solvismax.model.objects.data.BooleanValue;
import de.sgollmer.solvismax.model.objects.data.DoubleValue;
import de.sgollmer.solvismax.model.objects.data.LongValue;
import de.sgollmer.solvismax.model.objects.data.SingleData;
import de.sgollmer.solvismax.model.objects.data.StringData;

public class SingleValue implements IValue {

	private static final Pattern VALUE = Pattern.compile("(.*?)[,}\\]].*");
	private static final Pattern VALUE_STRING = Pattern.compile("\\s*\"(((\\\\\")|[^\"])*)\".*?");

	private static final Pattern NULL = Pattern.compile("^(null)$");
	private static final Pattern BOOLEAN = Pattern.compile("^(true|false)$");
	private static final Pattern INTEGER = Pattern.compile("^(-{0,1}\\d+)$");
	private static final Pattern FLOAT = Pattern.compile("^(-{0,1}\\d+(\\.\\d+){0,1}([Ee][+-]{0,1}\\d+){0,1})$");

	private SingleData<?> data;

	SingleValue() {
	}

	public SingleValue(final String value) {
		this.data = new StringData(value, -1L);
	}

	public SingleValue(final SingleData<?> data) {
		this.data = data;
	}

	@Override
	public void addTo(final StringBuilder builder) {
		builder.append(this.data.toJson());

	}

	@Override
	public int from(final String json, final int position, final long timeStamp) throws JsonException {
		String sub = json.substring(position);

		Matcher m = VALUE_STRING.matcher(sub);
		boolean isString = m.matches();
		if (!isString) {
			m = VALUE.matcher(sub);
			if (!m.matches()) {
				throw new JsonException("Valid single value can't be detected");
			}
		}

		sub = m.group(1);
		sub = sub.trim(); // never leading white spaces. Only trailing

		String group = null;
		if (isString) {
			group = sub.replace("\\\"", "\"");
			this.data = new StringData(group, timeStamp);
			return position + group.length() + 2;
		}
		m = INTEGER.matcher(sub);
		if (m.matches()) {
			group = m.group(1);
			this.data = new LongValue(Long.parseLong(group), timeStamp);
			return position + group.length();
		}
		m = FLOAT.matcher(sub);
		if (m.matches()) {
			group = m.group(1);
			this.data = new DoubleValue(Float.parseFloat(group), timeStamp);
			return position + group.length();
		}
		m = BOOLEAN.matcher(sub);
		if (m.matches()) {
			group = m.group(1);
			this.data = new BooleanValue(Boolean.parseBoolean(group), timeStamp);
			return position + group.length();
		}
		m = NULL.matcher(sub);
		if (m.matches()) {
			group = m.group(1);
			this.data = null;
			return position + group.length();
		} else {
			throw new JsonException("Valid single value can't be detected");
		}
	}

	@Override
	public String toString() {
		return this.data.toString();
	}

	@Override
	public SingleData<?> getSingleData() throws PackageException {
		return this.data;
	}

	@Override
	public Frame getFrame() {
		return null;
	}
}
