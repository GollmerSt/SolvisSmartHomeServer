/************************************************************************
 * 
 * $Id$
 *
 * 
 ************************************************************************/

package de.sgollmer.solvismax.connection.transfer;

import java.util.ArrayList;
import java.util.Collection;

import de.sgollmer.solvismax.error.JsonError;

public class ArrayValue implements Value {

	private Collection<Value> values = new ArrayList<>();
	
	public void add( Value value ) {
		this.values.add(value) ;
	}
	
	@Override
	public void addTo(StringBuilder builder) {
		boolean first = true;
		for (Value value : this.values) {
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
	public int from(String json, int position) throws JsonError {
		char c = Helper.charAt(json,position);
		if (c != '[') {
			throw new JsonError("Wrong character <" + c + "> at starting of a element");
		}
		++position;

		boolean finished = false;
		while (!finished) {
			switch (Helper.charAt(json,position)) {
				case '{':
					Frame frame = new Frame();
					position = frame.from(json, position);
					this.values.add(frame);
					c = Helper.charAt(json,position);
					break;
				case ',':
					++position;
					break;
				case ']':
					finished = true;
					break;
				default:
					SingleValue single = new SingleValue();
					single.from(json, position);
					this.values.add(single);
					break ;
			}
		}
		return position;
	}

}
