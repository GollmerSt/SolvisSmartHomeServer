package de.sgollmer.solvismax.model.objects.control;

import de.sgollmer.solvismax.model.Command;
import de.sgollmer.solvismax.model.Solvis;
import de.sgollmer.solvismax.model.objects.AllDataDescriptions;
import de.sgollmer.solvismax.model.objects.Observer;
import de.sgollmer.solvismax.model.objects.Screen;
import de.sgollmer.solvismax.model.objects.control.UpdateStrategies.Strategy;

public class ByScreenChange extends Strategy<ByScreenChange> {

	public ByScreenChange(Control control) {
		super(control);
	}
	
	public ByScreenChange() {
		this( null ) ;
	}

	@Override
	public ByScreenChange create(Control control, String irgendwasWieXML) {
		return new ByScreenChange(control);
	}

	@Override
	public void assign(AllDataDescriptions descriptions) {

	}

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
			solvis.execute(new Command(control.getDescription()));

		}
	}

}
