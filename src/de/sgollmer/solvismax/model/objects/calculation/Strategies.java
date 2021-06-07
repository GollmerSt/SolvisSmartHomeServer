/************************************************************************
 * 
 * $Id$
 *
 * 
 ************************************************************************/

package de.sgollmer.solvismax.model.objects.calculation;

import java.util.Collection;

import de.sgollmer.solvismax.error.AssignmentException;
import de.sgollmer.solvismax.error.TypeException;
import de.sgollmer.solvismax.error.AliasException;
import de.sgollmer.solvismax.model.Solvis;
import de.sgollmer.solvismax.model.objects.IAssigner;
import de.sgollmer.solvismax.model.objects.ResultStatus;
import de.sgollmer.solvismax.model.objects.IChannelSource.SetResult;
import de.sgollmer.solvismax.model.objects.data.IMode;
import de.sgollmer.solvismax.model.objects.data.SingleData;
import de.sgollmer.solvismax.model.objects.data.SolvisData;

public enum Strategies {
	RUNTIME(new RunTime(), "runtime"), STARTS(new Starts(), "starts"),
	BURNER_STATUS(new BurnerStatus(), "burnerStatus"), MIXER_POSITION_0(new MixerPosition0(), "mixerPosition0");

	private final Strategy<?> strategy;
	private final String name;

	private Strategies(final Strategy<?> strategy, final String name) {
		this.strategy = strategy;
		this.name = name;
	}

	static Strategies getByName(final String name) {
		for (Strategies strategy : Strategies.values()) {
			if (strategy.name.equals(name)) {
				return strategy;
			}
		}
		return null;
	}

	static abstract class Strategy<T extends Strategy<?>> implements IAssigner {

		protected final Calculation calculation;

		protected Strategy(final Calculation calculation) {
			this.calculation = calculation;
		}

		protected abstract T create(final Calculation calculation);

		abstract boolean isWriteable();

		SetResult setValue(Solvis solvis, SolvisData value) {
			return null; // i.g. is directly set via solvis data
		}

		protected SetResult setDebugValue(final Solvis solvis, final SingleData<?> value) {
			return new SetResult(ResultStatus.SUCCESS, value, false);
		}

		boolean getValue(final SolvisData dest, final Solvis solvis) {
			return true; // i.g. solvis data contains the current value
		}

		abstract void instantiate(final Solvis solvis) throws AssignmentException, AliasException;

		Collection<IMode<?>> getModes() {
			return null;
		}

		abstract Double getAccuracy();

		abstract boolean isBoolean();

		String getCsvMeta(final String column, final boolean semicolon) {
			return null;
		}

		protected abstract SingleData<?> interpretSetData(SingleData<?> singleData) throws TypeException;


	}

	@SuppressWarnings("rawtypes")
	public Strategy create(final Calculation calculation) {
		return this.strategy.create(calculation);
	}

}
