package de.sgollmer.solvismax.model.transfer;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import de.sgollmer.solvismax.error.JsonError;
import de.sgollmer.solvismax.model.objects.data.BooleanValue;
import de.sgollmer.solvismax.model.objects.data.FloatValue;
import de.sgollmer.solvismax.model.objects.data.SingleData;
import de.sgollmer.solvismax.model.objects.data.StringData;

public class SingleValue implements Value {

	private static final Pattern BOOLEAN = Pattern.compile("(true|false).*");
	private static final Pattern FLOAT = Pattern.compile("([+-]{0,1}\\d*\\.{0,1}\\d).*");
	private static final Pattern STRING = Pattern.compile("\"([^\"]*)\".*");

	private SingleData<?> data;

	public SingleValue() {
	}

	public SingleValue(String value) {
		this.data = new StringData(value);
	}
	
	public SingleValue( SingleData<?> data) {
		this.data = data ;
	}

	@Override
	public void addTo(StringBuilder builder) {
		builder.append(data.toJson());

	}

	@Override
	public int from(String json, int position) throws JsonError {
		String sub = json.substring(position);
		String group = null;
		Matcher m = STRING.matcher(sub);
		if (m.matches()) {
			group = m.group(1);
			this.data = new StringData(group);
			position += group.length() + 2;
		} else {
			m = BOOLEAN.matcher(sub);
			if (m.matches()) {
				group = m.group(1);
				this.data = new BooleanValue(Boolean.parseBoolean(group));
				position += group.length();
			}
			m = FLOAT.matcher(sub);
			if (m.matches()) {
				group = m.group(1);
				this.data = new FloatValue(Float.parseFloat(group));
				position += group.length();
			}
		}
		if ( this.data == null ) {
			throw new JsonError("Valid single value vcan't be detected") ;
		}
		return position ;
	}

	/**
	 * @return the data
	 */
	public SingleData<?> getData() {
		return data;
	}
}
