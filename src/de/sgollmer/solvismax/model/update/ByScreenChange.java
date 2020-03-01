/************************************************************************
 * 
 * $Id$
 *
 * 
 ************************************************************************/

package de.sgollmer.solvismax.model.update;

import javax.xml.namespace.QName;

import de.sgollmer.solvismax.error.XmlError;
import de.sgollmer.solvismax.model.CommandControl;
import de.sgollmer.solvismax.model.Solvis;
import de.sgollmer.solvismax.model.WatchDog.HumanAccess;
import de.sgollmer.solvismax.model.objects.ChannelSourceI;
import de.sgollmer.solvismax.model.objects.Observer;
import de.sgollmer.solvismax.model.objects.SolvisDescription;
import de.sgollmer.solvismax.model.objects.control.Control;
import de.sgollmer.solvismax.model.update.UpdateStrategies.Strategy;
import de.sgollmer.solvismax.model.update.UpdateStrategies.UpdateCreator;
import de.sgollmer.solvismax.xml.BaseCreator;
import de.sgollmer.solvismax.xml.CreatorByXML;

public class ByScreenChange extends Strategy<ByScreenChange> {

	@Override
	public void instantiate(Solvis solvis) {
		solvis.registerScreenChangedByHumanObserver(new Execute(solvis));

	}

	private class Execute implements Observer.ObserverI<HumanAccess> {

		private final Solvis solvis;
		private HumanAccess lastHumanAccess = HumanAccess.NONE;

		public Execute(Solvis solvis) {
			this.solvis = solvis;
		}

		@Override
		public void update(HumanAccess humanAccess, Object source) {
			if (!this.solvis.getUnit().getFeatures().isUpdateAfterUserAccess()) {
				return;
			}
			if (this.lastHumanAccess != HumanAccess.NONE && humanAccess != this.lastHumanAccess ) {
				ChannelSourceI channelSource = ByScreenChange.this.source;
				if (channelSource instanceof Control) {
					solvis.execute(new CommandControl(((Control) channelSource).getDescription(), this.solvis));
				}
			}
			this.lastHumanAccess = humanAccess ;
		}
	}

	public static class Creator extends UpdateCreator<ByScreenChange> {

		public Creator() {
			super(null, null);
		}

		public Creator(String id, BaseCreator<?> creator) {
			super(id, creator);
		}

		@Override
		public void setAttribute(QName name, String value) {

		}

		@Override
		public ByScreenChange create() throws XmlError {
			return new ByScreenChange();
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
