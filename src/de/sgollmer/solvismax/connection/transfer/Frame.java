package de.sgollmer.solvismax.connection.transfer;

import java.util.ArrayList;
import java.util.List;

import de.sgollmer.solvismax.error.JsonError;

public class Frame implements Value {
	protected final List<Element> elements = new ArrayList<>();

	public void add(Element element) {
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
	public int from(String json, int position) throws JsonError {
		while (Character.isWhitespace(Helper.charAt(json, position))){
			++position ;
		}
		char c = Helper.charAt(json, position);
		if (c == '{') {
			++position;
		} else {
			throw new JsonError("Wrong character <" + c + "> at starting of a frame");
		}
		boolean finished = false;
		while (!finished) {
			Element element = new Element();
			position = element.from(json, position);
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
						throw new JsonError("Wrong character <" + c + "> at end of a frame");
					}
			}
		}

		return position;

	}

	public Element get(int i) {
		return this.elements.get(i);
	}

	public int size() {
		return this.elements.size();
	}

}
