package de.sgollmer.solvismax.model.objects.control;

import de.sgollmer.solvismax.model.Solvis;
import de.sgollmer.solvismax.model.objects.AllDataDescriptions;

public enum UpdateStrategies {
	SCREEN_CHANGED( new ByScreenChange()), BURNER_OFF_SOMETIMES( new BurnerSometimes() ) ;
	
	private final Strategy<?> strategy ;
	
	private UpdateStrategies( Strategy<?> strategy ) {
		this.strategy = strategy ;
	}

	public Strategy<?> getStrategy() {
		return strategy;
	}

	public static abstract class Strategy<S extends Strategy<?>> {

	protected final Control control;

		public Strategy(Control control) {
			this.control = control;
		}

		public abstract S create(Control control, String irgendwasWieXML); // TODO beim XML-Lesen

		public abstract void assign(AllDataDescriptions descriptions );
		
		public abstract void instantiate(Solvis solvis) ;

	}
}
