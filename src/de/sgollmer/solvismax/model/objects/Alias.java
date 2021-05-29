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

public class Alias implements IAssigner {
	private final String id;
	private final String dataId;

	private Alias(final String id, final String dataId) {
		this.id = id;
		this.dataId = dataId;
	}

	public String getDataId() {
		return this.dataId;
	}

	public String getId() {
		return this.id;
	}

	public static class Creator extends CreatorByXML<Alias> {

		private String id;
		private String dataId;

		public Creator(final String id, final BaseCreator<?> creator) {
			super(id, creator);
		}

		@Override
		public void setAttribute(final QName name, final String value) {
			switch (name.getLocalPart()) {
				case "id":
					this.id = value;
					break;
				case "dataId":
					this.dataId = value;
					break;
			}

		}

		@Override
		public Alias create() throws XmlException {
			return new Alias(this.id, this.dataId);
		}

		@Override
		public CreatorByXML<?> getCreator(final QName name) {
			return null;
		}

		@Override
		public void created(final CreatorByXML<?> creator, final Object created) {
		}

	}

	@Override
	public void assign(final SolvisDescription description) {

	}

}
