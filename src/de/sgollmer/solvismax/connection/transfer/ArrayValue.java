/************************************************************************
 * 
 * $Id$
 *
 * 
 ************************************************************************/

package de.sgollmer.solvismax.connection.transfer;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import de.sgollmer.solvismax.error.JsonException;

public class ArrayValue implements IValue {

	private final Collection<? extends IValue> values;

	public ArrayValue(final List<SingleValue> values) {
		this.values = values;
	}

	public ArrayValue() {
		this.values = new ArrayList<>();
	}

	@SuppressWarnings("unchecked")
	public void add(IValue value) {
		((Collection<IValue>) this.values).add(value);
	}

	@Override
	public void addTo(final StringBuilder builder) {
		boolean first = true;
		for (IValue value : this.values) {
			if (first) {
				builder.append('[');
				first = false;
			} else {
				builder.append(',');
			}
			value.addTo(builder);
		}
		builder.append(']');
	}

	@Override
	public int from(final String json, final int startPosition, final long timeStamp) throws JsonException {
		int position = startPosition;
		char c = Helper.charAt(json, position);
		if (c != '[') {
			throw new JsonException("Wrong character <" + c + "> at starting of a element");
		}
		++position;

		boolean finished = false;
		while (!finished) {
			switch (Helper.charAt(json, position)) {
				case '{':
					Frame frame = new Frame();
					position = frame.from(json, position, timeStamp);
					this.add(frame);
					c = Helper.charAt(json, position);
					break;
				case ',':
					++position;
					break;
				case ']':
					finished = true;
					break;
				default:
					SingleValue single = new SingleValue();
					single.from(json, position, timeStamp);
					this.add(single);
					break;
			}
		}
		return position;
	}

	@Override
	public String toString() {
		StringBuilder builder = new StringBuilder();
		this.addTo(builder);
		return builder.toString();
	}

}
