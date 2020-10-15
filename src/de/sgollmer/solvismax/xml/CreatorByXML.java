/************************************************************************
 * 
 * $Id$
 *
 * 
 ************************************************************************/

package de.sgollmer.solvismax.xml;

import java.io.IOException;

import javax.xml.namespace.QName;

import de.sgollmer.solvismax.error.AssignmentException;
import de.sgollmer.solvismax.error.ReferenceException;
import de.sgollmer.solvismax.error.XmlException;

public abstract class CreatorByXML<T> {

	private final String id;
	private final BaseCreator<?> creator;

	public CreatorByXML(String id, BaseCreator<?> creator) {
		this.id = id;
		this.creator = creator;
	}

	public CreatorByXML(String id) {
		this.id = id;
		this.creator = (BaseCreator<?>) this;
	}

	/**
	 * Wird bei jedem Attribut aufgerufen
	 * 
	 * @param name
	 * @param value
	 */
	public abstract void setAttribute(QName name, String value) throws XmlException;

	/**
	 * Wird mit EndElement aufgerufen.
	 * 
	 * @return
	 * @throws XmlException
	 * @throws AssignmentException
	 * @throws ReferenceException
	 */

	public abstract T create() throws XmlException, IOException, AssignmentException, ReferenceException;

	/**
	 * Wird aufgerufen, wenn ein nested Tag erkannt wurde
	 * 
	 * @param name
	 * @return
	 */
	public abstract CreatorByXML<?> getCreator(QName name);

	public String getId() {
		return this.id;
	}

	/**
	 * Wird aufgerufen, wenn ein nested Element erzeugt wurde
	 * 
	 * @param creator
	 * @param created
	 * @throws XmlException
	 */
	public abstract void created(CreatorByXML<?> creator, Object created) throws XmlException;

	public BaseCreator<?> getBaseCreator() {
		return this.creator;
	}

	public void addCharacters(String data) {
	}

	public static class StringElement {

		private final String element;

		protected StringElement(String element) {
			this.element = element;

		}

		@Override
		public String toString() {
			return this.element;
		}

		public static class Creator extends CreatorByXML<StringElement> {
			private StringBuilder builder = new StringBuilder();

			public Creator(String id, BaseCreator<?> creator) {
				super(id, creator);
			}

			@Override
			public void setAttribute(QName name, String value) {

			}

			@Override
			public StringElement create() throws XmlException, IOException {
				return new StringElement(this.builder.toString());
			}

			@Override
			public CreatorByXML<?> getCreator(QName name) {
				return null;
			}

			@Override
			public void created(CreatorByXML<?> creator, Object created) {

			}

			@Override
			public void addCharacters(String data) {
				this.builder.append(data);
			}
		}
	}
}
