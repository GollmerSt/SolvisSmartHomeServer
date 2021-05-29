/************************************************************************
 * 
 * $Id$
 *
 * 
 ************************************************************************/

package de.sgollmer.solvismax.objects;

import javax.xml.namespace.QName;

import de.sgollmer.xmllibrary.BaseCreator;
import de.sgollmer.xmllibrary.CreatorByXML;
import de.sgollmer.xmllibrary.XmlException;

public class Field {
	private final int position;
	private final int length;

	private Field(final int position, final int length) {
		this.position = position;
		this.length = length;
	}

	public int getLength() {
		return this.length;
	}

	public String subString(final String data) {
		return data.substring(this.position, this.position + this.length);
	}

	public static class Creator extends CreatorByXML<Field> {

		private int position;
		private int length;

		public Creator(final String id, final BaseCreator<?> creator) {
			super(id, creator);
		}

		@Override
		public void setAttribute(final QName name, final String value) {
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
		public CreatorByXML<?> getCreator(final QName name) {
			return null;
		}

		@Override
		public void created(final CreatorByXML<?> creator, final Object created) {
		}

	}

}
