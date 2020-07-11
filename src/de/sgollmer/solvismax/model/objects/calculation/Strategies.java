/************************************************************************
 * 
 * $Id$
 *
 * 
 ************************************************************************/

package de.sgollmer.solvismax.model.objects.calculation;

import java.util.Collection;

import de.sgollmer.solvismax.model.Solvis;
import de.sgollmer.solvismax.model.objects.IAssigner;
import de.sgollmer.solvismax.model.objects.data.IMode;
import de.sgollmer.solvismax.model.objects.data.SingleData;
import de.sgollmer.solvismax.model.objects.data.SolvisData;

public enum Strategies {
	RUNTIME(new RunTime(), "runtime"),
	STARTS(new Starts(), "starts"),
	BURNER_STATUS(new BurnerStatus(), "burnerStatus"),
	MIXER_POSITION_0(new MixerPosition0(), "mixerPosition0");

	private final Strategy<?> strategy;
	private final String name;

	private Strategies(Strategy<?> strategy, String name) {
		this.strategy = strategy;
		this.name = name;
	}

	public static Strategies getByName(String name) {
		for (Strategies strategy : Strategies.values()) {
			if (strategy.name.equals(name)) {
				return strategy;
			}
		}
		return null;
	}

	public static abstract class Strategy<T extends Strategy<?>> implements IAssigner {

		protected final Calculation calculation;

		public Strategy( Calculation calculation ) {
			this.calculation = calculation ;
		}

		public abstract T create( Calculation calculation);

		public abstract boolean isWriteable();

		public SingleData<?> setValue(Solvis solvis, SolvisData value) {
			return null; // i.g. is directly set via solvis data
		}

		public boolean getValue(SolvisData dest, Solvis solvis) {
			return true; // i.g. solvis data contains the current value
		}

		public abstract void instantiate(Solvis solvis);
		
		public Collection< IMode > getModes() {
			return null ;
		}

		public abstract Double getAccuracy() ;

		public abstract boolean isBoolean() ;

	}

	@SuppressWarnings("rawtypes")
	public Strategy create( Calculation calculation ) {
		return this.strategy.create( calculation );
	}

}
