/************************************************************************
 * 
 * $Id$
 *
 * 
 ************************************************************************/

package de.sgollmer.solvismax.connection.transfer;

import java.util.ArrayList;
import java.util.List;

import de.sgollmer.solvismax.error.JsonException;

public class Frame implements IValue {
	protected final List<Element> elements = new ArrayList<>();

	void add(Element element) {
		this.elements.add(element);
	}

	@Override
	public void addTo(StringBuilder builder) {
		boolean first = true;
		for (Element element : this.elements) {
			if (!first) {
				builder.append(',');
			} else {
				builder.append('{');
				first = false;
			}
			element.addTo(builder);
		}
		if (first) {
			builder.append('{');
		}
		builder.append('}');

	}

	@Override
	public String toString() {
		StringBuilder builder = new StringBuilder();
		this.addTo(builder);
		return builder.toString();
	}

	@Override
	public int from(String json, int position, long timeStamp) throws JsonException {
		while (Character.isWhitespace(Helper.charAt(json, position))) {
			++position;
		}
		char c = Helper.charAt(json, position);
		if (c == '{') {
			++position;
		} else {
			throw new JsonException("Wrong character <" + c + "> at starting of a frame");
		}
		boolean finished = false;
		while (!finished) {
			Element element = new Element();
			position = element.from(json, position,timeStamp);
			this.elements.add(element);
			c = Helper.charAt(json, position);
			switch (c) {
				case ',':
					++position;
					break;
				case '}':
					finished = true;
					++position;
					break;
				default:
					if (Character.isWhitespace(c)) {
						++position;
					} else {
						throw new JsonException("Wrong character <" + c + "> at end of a frame");
					}
			}
		}

		return position;

	}

	Element get(int i) {
		return this.elements.get(i);
	}

	int size() {
		return this.elements.size();
	}

}
