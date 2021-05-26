/************************************************************************
 * 
 * $Id$
 *
 * 
 ************************************************************************/

package de.sgollmer.solvismax.connection.transfer;

import de.sgollmer.solvismax.error.JsonException;

public class Element {
	protected String name = null;
	protected IValue value = null;

	public Element(final String name, final IValue value) {
		this.name = name;
		this.value = value;
	}

	public Element(final String name) {
		this.name = name;
	}

	public Element() {
	}

	void addTo(StringBuilder builder) {
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
		StringBuilder builder = new StringBuilder();
		this.addTo(builder);
		return builder.toString();
	}

	int from(String json, int position, long timeStamp) throws JsonException {
		while (Character.isWhitespace(Helper.charAt(json, position))) {
			++position;
		}
		char c = Helper.charAt(json, position);
		if (c != '"') {
			throw new JsonException("Wrong character <" + c + "> at starting of a element");
		}
		++position;
		boolean endFound = false;
		int end = -1;
		while (!endFound) {
			end = json.indexOf("\"", position);
			if (end < 0) {
				throw new JsonException("End of name not found");
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
			throw new JsonException("End of name not found");
		}
		++position;
		while (Character.isWhitespace(Helper.charAt(json, position))) {
			++position;
		}
		switch (Helper.charAt(json, position)) {
			case '{':
				Frame frame = new Frame();
				position = frame.from(json, position, timeStamp);
				this.value = frame;
				break;
			case '[':
				ArrayValue array = new ArrayValue();
				position = array.from(json, position, timeStamp);
				this.value = array;
				break;
			default:
				SingleValue single = new SingleValue();
				position = single.from(json, position, timeStamp);
				this.value = single;
				break;
		}
		return position;
	}

	String getName() {
		return this.name;
	}

	void setName(String name) {
		this.name = name;
	}

	public IValue getValue() {
		return this.value;
	}

	void setValue(IValue value) {
		this.value = value;
	}

}
