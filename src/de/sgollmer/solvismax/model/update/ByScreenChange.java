/************************************************************************
 * 
 * $Id$
 *
 * 
 ************************************************************************/

package de.sgollmer.solvismax.model.update;

import javax.xml.namespace.QName;

import de.sgollmer.solvismax.error.XmlException;
import de.sgollmer.solvismax.model.Solvis;
import de.sgollmer.solvismax.model.objects.SolvisDescription;
import de.sgollmer.solvismax.model.update.UpdateStrategies.Strategy;
import de.sgollmer.solvismax.model.update.UpdateStrategies.UpdateCreator;
import de.sgollmer.solvismax.xml.BaseCreator;
import de.sgollmer.solvismax.xml.CreatorByXML;

public class ByScreenChange extends Strategy<ByScreenChange> {

	private static ByScreenChange getInstance() {
		return ByScreenChangeHolder.INSTANCE;
	}

	private static class ByScreenChangeHolder {
		private static final ByScreenChange INSTANCE = new ByScreenChange();
	}

	@Override
	public void instantiate(Solvis solvis) {
	}

	@Override
	public boolean isScreenChangeDependend() {
		return true;
	}

	static class Creator extends UpdateCreator<ByScreenChange> {

		Creator() {
			super(null, null);
		}

		private Creator(String id, BaseCreator<?> creator) {
			super(id, creator);
		}

		@Override
		public void setAttribute(QName name, String value) {

		}

		@Override
		public ByScreenChange create() throws XmlException {
			return ByScreenChange.getInstance();
		}

		@Override
		public CreatorByXML<?> getCreator(QName name) {
			return null;
		}

		@Override
		public void created(CreatorByXML<?> creator, Object created) {

		}

		@Override
		public UpdateCreator<ByScreenChange> createCreator(String id, BaseCreator<?> creator) {
			return new Creator(id, creator);
		}

	}

	@Override
	public void assign(SolvisDescription description) {

	}

}
