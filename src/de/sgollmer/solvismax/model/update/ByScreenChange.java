package de.sgollmer.solvismax.model.update;

import javax.xml.namespace.QName;

import de.sgollmer.solvismax.error.XmlError;
import de.sgollmer.solvismax.model.Command;
import de.sgollmer.solvismax.model.Solvis;
import de.sgollmer.solvismax.model.objects.Observer;
import de.sgollmer.solvismax.model.objects.Screen;
import de.sgollmer.solvismax.model.objects.SolvisDescription;
import de.sgollmer.solvismax.model.objects.control.Control;
import de.sgollmer.solvismax.model.update.UpdateStrategies.Strategy;
import de.sgollmer.solvismax.model.update.UpdateStrategies.UpdateCreator;
import de.sgollmer.solvismax.xml.BaseCreator;
import de.sgollmer.solvismax.xml.CreatorByXML;

public class ByScreenChange extends Strategy<ByScreenChange> {

	@Override
	public void instantiate(Solvis solvis) {
		solvis.registerScreenChangedByUserObserver(new Execute(solvis));

	}

	private class Execute implements Observer.ObserverI<Screen> {

		private final Solvis solvis;

		public Execute(Solvis solvis) {
			this.solvis = solvis;
		}

		@Override
		public void update(Screen data) {
			if ( source instanceof Control ) {
				solvis.execute(new Command(((Control)source).getDescription()));
			}

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
