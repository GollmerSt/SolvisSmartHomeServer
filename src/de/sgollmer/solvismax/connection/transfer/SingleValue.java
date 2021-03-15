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
import de.sgollmer.solvismax.model.objects.data.BooleanValue;
import de.sgollmer.solvismax.model.objects.data.DoubleValue;
import de.sgollmer.solvismax.model.objects.data.IntegerValue;
import de.sgollmer.solvismax.model.objects.data.SingleData;
import de.sgollmer.solvismax.model.objects.data.StringData;

public class SingleValue implements IValue {

	private static final Pattern NULL = Pattern.compile("(null).*");
	private static final Pattern BOOLEAN = Pattern.compile("(true|false).*");
	private static final Pattern INTEGER = Pattern.compile("(-{0,1}\\d+)[^\\.Ee].*");
	private static final Pattern FLOAT = Pattern.compile("(-{0,1}\\d+(\\.\\d+){0,1}([Ee][+-]{0,1}\\d+){0,1}).*");
	private static final Pattern STRING = Pattern.compile("\"(((\\\")|[^\"])*)\".*");

	private SingleData<?> data;

	SingleValue() {
	}

	public SingleValue(String value) {
		this.data = new StringData(value, -1);
	}

	public SingleValue(SingleData<?> data) {
		this.data = data;
	}

	@Override
	public void addTo(StringBuilder builder) {
		builder.append(this.data.toJson());

	}

	@Override
	public int from(String json, int position, long timeStamp) throws JsonException {
		String sub = json.substring(position);
		String group = null;
		Matcher m = STRING.matcher(sub);
		if (m.matches()) {
			group = m.group(1);
			group = group.replace("\\\"", "\"");
			this.data = new StringData(group, timeStamp);
			return position + group.length() + 2;
		}
		m = INTEGER.matcher(sub);
		if (m.matches()) {
			group = m.group(1);
			this.data = new IntegerValue(Integer.parseInt(group), timeStamp);
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
			throw new JsonException("Valid single value vcan't be detected");
		}
	}

	/**
	 * @return the data
	 */
	public SingleData<?> getData() {
		return this.data;
	}

	@Override
	public String toString() {
		return this.data.toString();
	}
}
