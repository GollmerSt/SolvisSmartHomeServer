/************************************************************************
 * 
 * $Id$
 *
 * 
 ************************************************************************/

package de.sgollmer.solvismax.model.objects;

import javax.xml.namespace.QName;

import de.sgollmer.solvismax.error.XmlError;
import de.sgollmer.solvismax.xml.CreatorByXML;
import de.sgollmer.solvismax.xml.BaseCreator;

public class Duration {
	private final String id;
	private final int time_ms;

	private Duration(String id, int time_ms) {
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

		Creator(String id, BaseCreator<?> creator) {
			super(id, creator);
		}

		@Override
		public void setAttribute(QName name, String value) {
			switch (name.getLocalPart()) {
				case "id":
					this.id = value;
					break;
				case "time_ms":
					this.time_ms = Integer.parseInt(value);
			}

		}

		@Override
		public Duration create() throws XmlError {
			return new Duration(this.id, this.time_ms);
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
