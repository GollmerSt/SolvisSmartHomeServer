/************************************************************************
 * 
 * $Id$
 *
 * 
 ************************************************************************/

package de.sgollmer.solvismax.model.update;

import javax.xml.namespace.QName;

import de.sgollmer.solvismax.model.Solvis;
import de.sgollmer.solvismax.model.update.UpdateStrategies.Strategy;
import de.sgollmer.solvismax.model.update.UpdateStrategies.UpdateCreator;
import de.sgollmer.xmllibrary.BaseCreator;
import de.sgollmer.xmllibrary.CreatorByXML;
import de.sgollmer.xmllibrary.XmlException;

public class HumanAccess extends Strategy<HumanAccess> {

	private static HumanAccess getInstance() {
		return ByScreenChangeHolder.INSTANCE;
	}

	private static class ByScreenChangeHolder {
		private static final HumanAccess INSTANCE = new HumanAccess();
	}

	@Override
	public void instantiate(final Solvis solvis) {
	}

	@Override
	public boolean isHumanAccessDependend() {
		return true;
	}

	static class Creator extends UpdateCreator<HumanAccess> {

		Creator() {
			super(null, null);
		}

		private Creator(final String id, final BaseCreator<?> creator) {
			super(id, creator);
		}

		@Override
		public void setAttribute(final QName name, final String value) {

		}

		@Override
		public HumanAccess create() throws XmlException {
			return HumanAccess.getInstance();
		}

		@Override
		public CreatorByXML<?> getCreator(final QName name) {
			return null;
		}

		@Override
		public void created(final CreatorByXML<?> creator, final Object created) {

		}

		@Override
		public UpdateCreator<HumanAccess> createCreator(final String id, final BaseCreator<?> creator) {
			return new Creator(id, creator);
		}

	}

}
