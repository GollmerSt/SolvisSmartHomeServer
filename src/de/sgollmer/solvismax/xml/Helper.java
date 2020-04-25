/************************************************************************
 * 
 * $Id$
 *
 * 
 ************************************************************************/

package de.sgollmer.solvismax.xml;

import java.io.IOException;

import javax.xml.namespace.QName;

import de.sgollmer.solvismax.error.XmlError;

public class Helper {

	public static class IntegerValue {
		private final java.lang.Integer integer;

		public IntegerValue(java.lang.Integer i) {
			this.integer = i;
		}

		public java.lang.Integer toInteger() {
			return this.integer;
		}

		public static class Creator extends CreatorByXML<IntegerValue> {

			StringBuilder builder = new StringBuilder();

			public Creator(String id, BaseCreator<?> creator) {
				super(id, creator);
			}

			@Override
			public void setAttribute(QName name, String value) {
			}

			@Override
			public IntegerValue create() throws XmlError, IOException {
				if (this.builder.length() > 0) {
					return new IntegerValue(Integer.decode(this.builder.toString()));
				} else {
					return new IntegerValue(null);
				}
			}

			@Override
			public CreatorByXML<?> getCreator(QName name) {
				return null;
			}

			@Override
			public void created(CreatorByXML<?> creator, Object created) {
			}

			@Override
			public void addCharacters(String chars) {
				this.builder.append(chars.trim());
			}

		}
	}
}
