/************************************************************************
 * 
 * $Id$
 *
 * 
 ************************************************************************/

package de.sgollmer.solvismax.xml;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;

import javax.xml.namespace.QName;

import de.sgollmer.solvismax.error.XmlError;

public class ArrayXml<A extends ArrayXml.IElement<A>> {

	public interface IElement<C> {
		public CreatorByXML<C> getCreator(String name, BaseCreator<?> creator);
	}

	private final Collection<A> array;

	private ArrayXml(Collection<A> array) {
		this.array = array;
	}

	public Collection<A> getArray() {
		return this.array;
	}

	public static class Creator<B extends IElement<B>> extends CreatorByXML<ArrayXml<B>> {

		private final Collection<B> array = new ArrayList<>();
		private final B parent;
		private final String xmlName;

		public Creator(String id, BaseCreator<?> creator, B parent, String xmlName) {
			super(id, creator);
			this.parent = parent;
			this.xmlName = xmlName;
		}

		@Override
		public void setAttribute(QName name, String value) {
		}

		@Override
		public ArrayXml<B> create() throws XmlError, IOException {
			return new ArrayXml<B>(this.array);
		}

		@Override
		public CreatorByXML<?> getCreator(QName name) {
			String xName = name.getLocalPart();
			if (xName.equals(this.xmlName)) {
				return this.parent.getCreator(xName, this.getBaseCreator());
			} else {
				return null;
			}
		}

		@SuppressWarnings("unchecked")
		@Override
		public void created(CreatorByXML<?> creator, Object created) {
			this.array.add((B) created);
		}

	}

}
