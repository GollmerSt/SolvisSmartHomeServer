package de.sgollmer.solvismax.model.objects.control;

import de.sgollmer.solvismax.model.Solvis;
import de.sgollmer.solvismax.model.objects.Assigner;

public enum UpdateStrategies {
	SCREEN_CHANGED(new ByScreenChange()), BURNER_OFF_SOMETIMES(new BurnerSometimes());

	private final Strategy<?> strategy;

	private UpdateStrategies(Strategy<?> strategy) {
		this.strategy = strategy;
	}

	public Strategy<?> getStrategy() {
		return strategy;
	}

	public static abstract class Strategy<S extends Strategy<?>> implements Assigner {

		protected Control control;

		public abstract S create(String irgendwasWieXML); // TODO beim XML-Lesen

		public abstract void instantiate(Solvis solvis);

		/**
		 * @param control
		 *            the control to set
		 */
		public void setControl(Control control) {
			this.control = control;
		}

	}
}
