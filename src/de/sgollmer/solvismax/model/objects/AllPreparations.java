/************************************************************************
 * 
 * $Id$
 *
 * 
 ************************************************************************/

package de.sgollmer.solvismax.model.objects;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;

import javax.xml.namespace.QName;

import de.sgollmer.solvismax.error.XmlError;
import de.sgollmer.solvismax.xml.BaseCreator;
import de.sgollmer.solvismax.xml.CreatorByXML;

public class AllPreparations {

	private static final String XML_PREPARATION = "Preparation";

	private final Collection<Preparation> preparations;

	private AllPreparations(Collection<Preparation> preparations) {
		this.preparations = preparations;
	}

	public Preparation get(String id) {
		for (Preparation preparation : this.preparations) {
			if (preparation.getId().equals(id)) {
				return preparation;
			}
		}
		return null;
	}

	static class Creator extends CreatorByXML<AllPreparations> {

		private final Collection<Preparation> preparations = new ArrayList<>();

		Creator(String id, BaseCreator<?> creator) {
			super(id, creator);
		}

		@Override
		public void setAttribute(QName name, String value) {
		}

		@Override
		public AllPreparations create() throws XmlError, IOException {
			return new AllPreparations(this.preparations);
		}

		@Override
		public CreatorByXML<?> getCreator(QName name) {
			String id = name.getLocalPart();
			switch (id) {
				case XML_PREPARATION:
					return new Preparation.Creator(id, getBaseCreator());
			}
			return null;
		}

		@Override
		public void created(CreatorByXML<?> creator, Object created) {
			switch (creator.getId()) {
				case XML_PREPARATION:
					this.preparations.add((Preparation) created);
					break;
			}

		}

	}

	public static class PreparationRef {
		private final String preparationId;

		private PreparationRef(String preparationId) {
			this.preparationId = preparationId;
		}

		public String getPreparationId() {
			return this.preparationId;
		}

		public static class Creator extends CreatorByXML<PreparationRef> {

			private String preparationId = null;

			public Creator(String id, BaseCreator<?> creator) {
				super(id, creator);
			}

			@Override
			public void setAttribute(QName name, String value) {
				switch (name.getLocalPart()) {
					case "refId":
						this.preparationId = value;
				}

			}

			@Override
			public PreparationRef create() throws XmlError, IOException {
				return new PreparationRef(this.preparationId);
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

}
