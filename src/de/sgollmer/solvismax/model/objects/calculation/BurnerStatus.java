/************************************************************************
 * 
 * $Id$
 *
 * 
 ************************************************************************/

package de.sgollmer.solvismax.model.objects.calculation;

import java.util.Arrays;
import java.util.Collection;

import de.sgollmer.solvismax.error.AssignmentException;
import de.sgollmer.solvismax.Constants.Csv;
import de.sgollmer.solvismax.error.AliasException;
import de.sgollmer.solvismax.error.TypeException;
import de.sgollmer.solvismax.log.LogManager;
import de.sgollmer.solvismax.log.LogManager.ILogger;
import de.sgollmer.solvismax.model.Solvis;
import de.sgollmer.solvismax.model.objects.AllSolvisData;
import de.sgollmer.solvismax.model.objects.Observer.IObserver;
import de.sgollmer.solvismax.model.objects.SolvisDescription;
import de.sgollmer.solvismax.model.objects.calculation.Strategies.Strategy;
import de.sgollmer.solvismax.model.objects.data.IMode;
import de.sgollmer.solvismax.model.objects.data.ModeValue;
import de.sgollmer.solvismax.model.objects.data.SolvisData;

public class BurnerStatus extends Strategy<BurnerStatus> {

	private static final ILogger logger = LogManager.getInstance().getLogger(BurnerStatus.class);

	BurnerStatus() {
		super(null);
	}

	private BurnerStatus(final Calculation calculation) {
		super(calculation);
	}

	@Override
	protected BurnerStatus create(final Calculation calculation) {
		return new BurnerStatus(calculation);
	}

	private enum Status implements IMode<Status> {
		OFF("off"), LEVEL1("Stufe1"), LEVEL2("Stufe2");

		private String name;

		private Status(final String name) {
			this.name = name;
		}

		@Override
		public String getName() {
			return this.name;
		}

		@Override
		public ModeValue<?> create(final long timeStamp) {
			return new ModeValue<>(this, timeStamp);
		}

		@Override
		public Handling getHandling() {
			return Handling.RO;
		}

		@Override
		public String getCvsMeta() {
			return this.name + this.getHandling().getCvsMeta();
		}
	}

	@Override
	boolean isWriteable() {
		return false;
	}

	@Override
	void instantiate(final Solvis solvis) throws AssignmentException, AliasException {
		AllSolvisData allData = solvis.getAllSolvisData();
		AliasGroup aliasGroup = this.calculation.getAliasGroup();

		SolvisData result = allData.get(this.calculation.getDescription().getId());
		SolvisData burnerLevel1On = aliasGroup.get(allData, "burnerLevel1On");
		SolvisData burnerLevel2On = aliasGroup.get(allData, "burnerLevel2On", true);

		if (result == null) {
			throw new AssignmentException("Assignment error: AliasGroup not assigned");
		}

		if (result.getMode() == null) {
			result.setMode(Status.OFF, -1);
		}

		Executable executable = new Executable(result, burnerLevel1On, burnerLevel2On);

		executable.update(burnerLevel1On, this);

	}

	private class Executable implements IObserver<SolvisData> {

		private final SolvisData result;
		private final SolvisData burnerLevel1On;
		private final SolvisData burnerLevel2On;

		private Executable(final SolvisData result, final SolvisData burnerLevel1On, final SolvisData burnerLevel2On) {
			this.result = result;
			this.burnerLevel1On = burnerLevel1On;
			this.burnerLevel2On = burnerLevel2On;
			this.burnerLevel1On.registerContinuousObserver(this);
			if (this.burnerLevel2On != null) {
				this.burnerLevel2On.registerContinuousObserver(this);
			}
		}

		@Override
		public void update(final SolvisData data, final Object source) {

			Status result = null;

			boolean level1;
			boolean level2 = false;
			;
			try {
				level1 = this.burnerLevel1On.getBool();
				if (this.burnerLevel2On != null) {
					level2 = this.burnerLevel2On.getBool();
				}
			} catch (TypeException e) {
				logger.error("Type error, update ignored", e);
				return;
			}

			if (level2) {
				result = Status.LEVEL2;
			} else if (level1) {
				result = Status.LEVEL1;
			} else {
				result = Status.OFF;
			}
			this.result.setMode(result, data.getTimeStamp());

		}
	}

	@Override
	public void assign(final SolvisDescription description) {

	}

	@Override
	Collection<IMode<?>> getModes() {
		return Arrays.asList(Status.values());
	}

	@Override
	Double getAccuracy() {
		return null;
	}

	@Override
	boolean isBoolean() {
		return false;
	}

	@Override
	String getCsvMeta(final String column, final boolean semicolon) {
		switch (column) {
			case Csv.MODES:
				StringBuilder builder = new StringBuilder();
				for (Status entry : Status.values()) {
					if (builder.length() != 0) {
						builder.append('|');
					}
					builder.append(entry.getCvsMeta());
				}
				return builder.toString();
		}
		return null;
	}
}
