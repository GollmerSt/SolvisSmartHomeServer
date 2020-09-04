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
import de.sgollmer.solvismax.error.DependencyException;
import de.sgollmer.solvismax.error.TypeException;
import de.sgollmer.solvismax.log.LogManager;
import de.sgollmer.solvismax.log.LogManager.ILogger;
import de.sgollmer.solvismax.model.Solvis;
import de.sgollmer.solvismax.model.objects.AllSolvisData;
import de.sgollmer.solvismax.model.objects.Dependencies;
import de.sgollmer.solvismax.model.objects.Observer.IObserver;
import de.sgollmer.solvismax.model.objects.SolvisDescription;
import de.sgollmer.solvismax.model.objects.calculation.Strategies.Strategy;
import de.sgollmer.solvismax.model.objects.data.IMode;
import de.sgollmer.solvismax.model.objects.data.SolvisData;

public class BurnerStatus extends Strategy<BurnerStatus> {

	private static final ILogger logger = LogManager.getInstance().getLogger(BurnerStatus.class);

	BurnerStatus() {
		super(null);
	}

	private BurnerStatus(Calculation calculation) {
		super(calculation);
	}

	@Override
	protected BurnerStatus create(Calculation calculation) {
		return new BurnerStatus(calculation);
	}

	private enum Status implements IMode {
		OFF("off"), LEVEL1("Stufe1"), LEVEL2("Stufe2");

		private String name;

		private Status(String name) {
			this.name = name;
		}

		@Override
		public String getName() {
			return this.name;
		}
	}

	@Override
	boolean isWriteable() {
		return false;
	}

	@Override
	void instantiate(Solvis solvis) throws AssignmentException, DependencyException {
		AllSolvisData allData = solvis.getAllSolvisData();
		Dependencies dependencies = this.calculation.getDependencies();

		SolvisData result = allData.get(this.calculation.getDescription().getId());

		if (result.getMode() == null) {
			result.setMode(Status.OFF, -1);
		}

		SolvisData burnerLevel1On = dependencies.get(allData, "burnerLevel1On");
		SolvisData burnerLevel2On = dependencies.get(allData, "burnerLevel2On");

		if (result == null || burnerLevel1On == null || burnerLevel2On == null) {
			throw new AssignmentException("Assignment error: Dependencies not assigned");
		}
		Executable executable = new Executable(result, burnerLevel1On, burnerLevel2On);

		executable.update(burnerLevel1On, this);

	}

	private class Executable implements IObserver<SolvisData> {

		private final SolvisData result;
		private final SolvisData burnerLevel1On;
		private final SolvisData burnerLevel2On;

		private Executable(SolvisData result, SolvisData burnerLevel1On, SolvisData burnerLevel2On) {
			this.result = result;
			this.burnerLevel1On = burnerLevel1On;
			this.burnerLevel2On = burnerLevel2On;
			this.burnerLevel1On.registerContinuousObserver(this);
			this.burnerLevel2On.registerContinuousObserver(this);
		}

		@Override
		public void update(SolvisData data, Object source) {

			Status result = null;

			boolean level1;
			boolean level2;
			try {
				level1 = this.burnerLevel1On.getBool();
				level2 = this.burnerLevel2On.getBool();
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
	public void assign(SolvisDescription description) {

	}

	@Override
	Collection<IMode> getModes() {
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

}
