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

import de.sgollmer.solvismax.log.LogManager;
import de.sgollmer.solvismax.log.LogManager.ILogger;
import de.sgollmer.xmllibrary.BaseCreator;
import de.sgollmer.xmllibrary.CreatorByXML;
import de.sgollmer.xmllibrary.XmlException;

public class AllPreparations {

	private static final String XML_PREPARATION = "Preparation";
	private static final ILogger logger = LogManager.getInstance().getLogger(AllPreparations.class);

	private final Collection<Preparation> preparations;

	private AllPreparations(final Collection<Preparation> preparations) {
		this.preparations = preparations;
	}

	public Preparation get(final String id) {
		if (id == null) {
			return null;
		}
		for (Preparation preparation : this.preparations) {
			if (preparation.getId().equals(id)) {
				return preparation;
			}
		}
		return null;
	}

	static class Creator extends CreatorByXML<AllPreparations> {

		private final Collection<Preparation> preparations = new ArrayList<>();

		Creator(final String id, final BaseCreator<?> creator) {
			super(id, creator);
		}

		@Override
		public void setAttribute(final QName name, final String value) {
		}

		@Override
		public AllPreparations create() throws XmlException, IOException {
			return new AllPreparations(this.preparations);
		}

		@Override
		public CreatorByXML<?> getCreator(final QName name) {
			String id = name.getLocalPart();
			switch (id) {
				case XML_PREPARATION:
					return new Preparation.Creator(id, getBaseCreator());
			}
			return null;
		}

		@Override
		public void created(final CreatorByXML<?> creator, final Object created) {
			switch (creator.getId()) {
				case XML_PREPARATION:
					Preparation p = (Preparation) created;
					if (this.preparations.contains(p)) {
						logger.error("Preparation <" + p.getId() + "> is not unique.");
					}
					this.preparations.add((Preparation) created);
					break;
			}

		}

	}

	public static class PreparationRef {
		private final String preparationId;

		private PreparationRef(final String preparationId) {
			this.preparationId = preparationId;
		}

		public String getPreparationId() {
			return this.preparationId;
		}

		public static class Creator extends CreatorByXML<PreparationRef> {

			private String preparationId = null;

			public Creator(final String id, final BaseCreator<?> creator) {
				super(id, creator);
			}

			@Override
			public void setAttribute(final QName name, final String value) {
				switch (name.getLocalPart()) {
					case "refId":
						this.preparationId = value;
				}

			}

			@Override
			public PreparationRef create() throws XmlException, IOException {
				return new PreparationRef(this.preparationId);
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

}
