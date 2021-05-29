/************************************************************************
 * 
 * $Id$
 *
 * 
 ************************************************************************/

package de.sgollmer.solvismax.model.objects;

import javax.xml.namespace.QName;

import de.sgollmer.xmllibrary.BaseCreator;
import de.sgollmer.xmllibrary.CreatorByXML;
import de.sgollmer.xmllibrary.XmlException;

public class Duration {
	private final String id;
	private final int time_ms;

	private Duration(final String id, final int time_ms) {
		this.id = id;
		this.time_ms = time_ms;
	}

	/**
	 * @return the id
	 */
	String getId() {
		return this.id;
	}

	/**
	 * @return the time_ms
	 */
	public int getTime_ms() {
		return this.time_ms;
	}

	static class Creator extends CreatorByXML<Duration> {

		private String id;
		private int time_ms;

		Creator(final String id, final BaseCreator<?> creator) {
			super(id, creator);
		}

		@Override
		public void setAttribute(final QName name, final String value) {
			switch (name.getLocalPart()) {
				case "id":
					this.id = value;
					break;
				case "time_ms":
					this.time_ms = Integer.parseInt(value);
			}

		}

		@Override
		public Duration create() throws XmlException {
			return new Duration(this.id, this.time_ms);
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
