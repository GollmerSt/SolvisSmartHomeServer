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
import de.sgollmer.solvismax.error.PackageException;
import de.sgollmer.solvismax.model.objects.data.SingleData;

public class Frame implements IValue {
	protected final List<Element> elements = new ArrayList<>();

	void add(final Element element) {
		this.elements.add(element);
	}

	@Override
	public void addTo(final StringBuilder builder) {
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
	public int from(final String json, final int startPosition, final long timeStamp) throws JsonException {
		int position = startPosition;
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
			position = element.from(json, position, timeStamp);
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

	@Override
	public SingleData<?> getSingleData() throws PackageException {
		throw new PackageException("Frame can't converted to SingleData");
	}

	Element get(String id) throws PackageException {
		Element result = null;
		for (Element e : this.elements) {
			if (id.equals(e.name)) {
				if (result == null) {
					result = e;
				} else {
					throw new PackageException("Double definition of <" + id + ">.");
				}
			}
		}
		if ( result == null ) {
			throw new PackageException("<" + id + " not found.");

		}
		return result;
	}

	@Override
	public Frame getFrame() {
		return this;
	}

}
