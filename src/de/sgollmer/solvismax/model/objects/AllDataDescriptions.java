package de.sgollmer.solvismax.model.objects;

import java.util.HashMap;
import java.util.Map;

import javax.xml.namespace.QName;

import de.sgollmer.solvismax.error.XmlError;
import de.sgollmer.solvismax.xml.CreatorByXML;
import de.sgollmer.solvismax.xml.BaseCreator;

public class AllDataDescriptions implements Assigner {

	private Map<String, DataDescription> descriptions = new HashMap<>();

	public void addDescription(DataDescription description) {
		this.descriptions.put(description.getId(), description);
	}

	public DataDescription get(String descriptionString) {
		return this.descriptions.get(descriptionString);
	}

	@Override
	public void assign(SolvisDescription description) {
		for (DataDescription data : descriptions.values()) {
			data.assign( description ) ;
		}
	}

	public static class Creator extends CreatorByXML<AllDataDescriptions> {

		private final AllDataDescriptions descriptions = new AllDataDescriptions();

		public Creator(String id, BaseCreator<?> creator) {
			super(id, creator);
		}

		@Override
		public void setAttribute(QName name, String value) {

		}

		@Override
		public AllDataDescriptions create() throws XmlError {
			return descriptions;
		}

		@Override
		public CreatorByXML<?> getCreator(QName name) {
			String id = name.getLocalPart();
			if (id.equals("DataDescription")) {
				return new DataDescription.Creator(id, this.getBaseCreator());
			}
			return null;
		}

		@Override
		public void created(CreatorByXML<?> creator, Object created) {
			this.descriptions.addDescription((DataDescription) created);

		}
	}

}
