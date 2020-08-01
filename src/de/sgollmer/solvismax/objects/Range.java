/************************************************************************
 * 
 * $Id$
 *
 * 
 ************************************************************************/

package de.sgollmer.solvismax.objects;

import java.io.IOException;

import javax.xml.namespace.QName;

import de.sgollmer.solvismax.error.XmlException;
import de.sgollmer.solvismax.xml.BaseCreator;
import de.sgollmer.solvismax.xml.CreatorByXML;

public class Range {
	private final int lower;
	private final int higher;

	public Range(int lower, int higher) {
		this.lower = lower;
		this.higher = higher;
	}

	public int getLower() {
		return this.lower;
	}

	public int getHigher() {
		return this.higher;
	}

	public static class Creator extends CreatorByXML<Range> {

		private int lower;
		private int higher;

		public Creator(String id, BaseCreator<?> creator) {
			super(id, creator);
		}

		@Override
		public void setAttribute(QName name, String value) {
			switch (name.getLocalPart()) {
				case "lowerLimit":
					this.lower = Integer.parseInt(value);
					break;
				case "higherLimit":
					this.higher = Integer.parseInt(value);
					break;
			}

		}

		@Override
		public Range create() throws XmlException, IOException {
			return new Range(this.lower, this.higher);
		}

		@Override
		public CreatorByXML<?> getCreator(QName name) {
			return null;
		}

		@Override
		public void created(CreatorByXML<?> creator, Object created) {
		}

	}
}
