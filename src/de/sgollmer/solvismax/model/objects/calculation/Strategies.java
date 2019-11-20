package de.sgollmer.solvismax.model.objects.calculation;

import de.sgollmer.solvismax.model.Solvis;
import de.sgollmer.solvismax.model.objects.AllDataDescriptions;
import de.sgollmer.solvismax.model.objects.data.SolvisData;

public enum Strategies {
	RUNTIME(new RunTime()), STARTS( new Starts()), BURNER_STATUS( new BurnerStatus()), MIXER_POSITION_0( new MixerPosition0());

	private final Strategy<?> strategy;

	private Strategies(Strategy<?> strategy) {
		this.strategy = strategy;
	}

	public static abstract class Strategy<T extends Strategy<?>> {

		protected final Calculation calculation;

		public Strategy(Calculation calculation) {
			this.calculation = calculation;
		}

		public abstract T create(Calculation calculation);

		public abstract String getUnit();;

		public abstract boolean isWriteable();

		public boolean setValue(Solvis solvis, SolvisData value) {
			return true ;		// i.g. is directly set via solvis data
		}

		public boolean getValue(SolvisData dest, Solvis solvis) {
			return true ;		// i.g. solvis data contains the current value
		}

		public abstract void assign(AllDataDescriptions descriptions );
		public abstract void instantiate( Solvis solvis );

	}

	@SuppressWarnings("rawtypes")
	public Strategy create(Calculation calculation) {
		return this.strategy.create(calculation);
	}

}
