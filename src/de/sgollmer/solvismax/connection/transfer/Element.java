/************************************************************************
 * 
 * $Id$
 *
 * 
 ************************************************************************/

package de.sgollmer.solvismax.connection.transfer;

import de.sgollmer.solvismax.error.JsonError;

public class Element {
	protected String name;
	protected IValue value;

	public void addTo(StringBuilder builder) {
		builder.append('"');
		builder.append(this.name);
		builder.append("\":");
		if (this.value == null) {
			builder.append("null");
		} else {
			this.value.addTo(builder);
		}
	}
	
	@Override
	public String toString() {
		StringBuilder builder = new StringBuilder() ;
		this.addTo(builder);
		return builder.toString();
	}

	public int from(String json, int position) throws JsonError {
		while (Character.isWhitespace(Helper.charAt(json, position))) {
			++position;
		}
		char c = Helper.charAt(json, position);
		if (c != '"') {
			throw new JsonError("Wrong character <" + c + "> at starting of a element");
		}
		++position;
		boolean endFound = false;
		int end = -1;
		while (!endFound) {
			end = json.indexOf("\"", position);
			if (end < 0) {
				throw new JsonError("End of name not found");
			}
			if (Helper.charAt(json, end - 1) == '\\') {
				position = end + 1;
			} else {
				endFound = true;
			}
		}
		this.name = json.substring(position, end);
		position = end + 1;
		while (Character.isWhitespace(Helper.charAt(json, position))) {
			++position;
		}
		if (Helper.charAt(json, position) != ':') {
			throw new JsonError("End of name not found");
		}
		++position;
		while (Character.isWhitespace(Helper.charAt(json, position))) {
			++position;
		}
		switch (Helper.charAt(json, position)) {
			case '{':
				Frame frame = new Frame();
				position = frame.from(json, position);
				this.value = frame;
				break;
			case '[':
				ArrayValue array = new ArrayValue();
				position = array.from(json, position);
				this.value = array;
				break;
			default:
				SingleValue single = new SingleValue();
				position = single.from(json, position);
				this.value = single;
				break;
		}
		return position;
	}

	public String getName() {
		return this.name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public IValue getValue() {
		return this.value;
	}

	public void setValue(IValue value) {
		this.value = value;
	}

}
