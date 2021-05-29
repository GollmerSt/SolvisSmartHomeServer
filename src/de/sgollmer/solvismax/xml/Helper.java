/************************************************************************
 * 
 * $Id$
 *
 * 
 ************************************************************************/

package de.sgollmer.solvismax.xml;

import java.io.IOException;

import javax.xml.namespace.QName;

import de.sgollmer.xmllibrary.BaseCreator;
import de.sgollmer.xmllibrary.CreatorByXML;

public class Helper {

	public static class IntegerValue {
		private final java.lang.Integer integer;

		private IntegerValue(final java.lang.Integer i) {
			this.integer = i;
		}

		public Integer toInteger() {
			return this.integer;
		}

		public static class Creator extends CreatorByXML<IntegerValue> {

			StringBuilder builder = new StringBuilder();

			public Creator(final String id, final BaseCreator<?> creator) {
				super(id, creator);
			}

			@Override
			public void setAttribute(final QName name, final String value) {
			}

			@Override
			public IntegerValue create() throws IOException {
				if (this.builder.length() > 0) {
					return new IntegerValue(Integer.decode(this.builder.toString()));
				} else {
					return new IntegerValue(null);
				}
			}

			@Override
			public CreatorByXML<?> getCreator(final QName name) {
				return null;
			}

			@Override
			public void created(final CreatorByXML<?> creator, final Object created) {
			}

			@Override
			public void addCharacters(final String chars) {
				this.builder.append(chars.trim());
			}

		}
	}
}
