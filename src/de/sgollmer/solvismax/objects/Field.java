/************************************************************************
 * 
 * $Id$
 *
 * 
 ************************************************************************/

package de.sgollmer.solvismax.objects;

import javax.xml.namespace.QName;

import de.sgollmer.solvismax.error.XmlException;
import de.sgollmer.solvismax.xml.CreatorByXML;
import de.sgollmer.solvismax.xml.BaseCreator;

public class Field {
	private final int position;
	private final int length;

	private Field(int position, int length) {
		this.position = position;
		this.length = length;
	}

	public int getLength() {
		return this.length;
	}

	public String subString(String data) {
		return data.substring(this.position, this.position + this.length);
	}

	public static class Creator extends CreatorByXML<Field> {

		private int position;
		private int length;

		public Creator(String id, BaseCreator<?> creator) {
			super(id, creator);
		}

		@Override
		public void setAttribute(QName name, String value) {
			switch (name.getLocalPart()) {
				case "position":
					this.position = Integer.parseInt(value);
					break;
				case "length":
					this.length = Integer.parseInt(value);
			}

		}

		@Override
		public Field create() throws XmlException {
			return new Field(this.position, this.length);
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
