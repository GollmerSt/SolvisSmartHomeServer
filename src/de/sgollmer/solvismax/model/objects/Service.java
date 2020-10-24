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

import de.sgollmer.solvismax.model.Solvis;
import de.sgollmer.solvismax.model.objects.screen.AbstractScreen;
import de.sgollmer.solvismax.model.objects.screen.ScreenRef;
import de.sgollmer.xmllibrary.BaseCreator;
import de.sgollmer.xmllibrary.CreatorByXML;
import de.sgollmer.xmllibrary.XmlException;

public class Service implements IAssigner {

	private static final String XML_SERVICE_SCREEN = "ServiceScreen";

	private final Collection<ScreenRef> serviceScreenRefs;

	private Service(Collection<ScreenRef> serviceScreenRefs) {
		this.serviceScreenRefs = serviceScreenRefs;
	}

	public boolean isServiceScreen(AbstractScreen screen, Solvis solvis) {
		if (screen == null) {
			return false;
		}
		for (ScreenRef ref : this.serviceScreenRefs) {
			AbstractScreen cmp = ref.getScreen().get(solvis);
			if (cmp != null && cmp == screen) {
				return true;
			}
		}
		return false;
	}

	static class Creator extends CreatorByXML<Service> {
		private final Collection<ScreenRef> serviceScreenRefs = new ArrayList<>();

		Creator(String id, BaseCreator<?> creator) {
			super(id, creator);
		}

		@Override
		public void setAttribute(QName name, String value) {
		}

		@Override
		public Service create() throws XmlException, IOException {
			return new Service(this.serviceScreenRefs);
		}

		@Override
		public CreatorByXML<?> getCreator(QName name) {
			String id = name.getLocalPart();
			switch (id) {
				case XML_SERVICE_SCREEN:
					return new ScreenRef.Creator(id, getBaseCreator());
			}
			return null;
		}

		@Override
		public void created(CreatorByXML<?> creator, Object created) {
			switch (creator.getId()) {
				case XML_SERVICE_SCREEN:
					this.serviceScreenRefs.add((ScreenRef) created);
			}
		}

	}

	@Override
	public void assign(SolvisDescription description) throws XmlException {
		for (ScreenRef ref : this.serviceScreenRefs) {
			ref.assign(description);
		}

	}
}
