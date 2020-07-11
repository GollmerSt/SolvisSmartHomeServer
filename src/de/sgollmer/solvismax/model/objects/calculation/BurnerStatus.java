/************************************************************************
 * 
 * $Id$
 *
 * 
 ************************************************************************/

package de.sgollmer.solvismax.model.objects.calculation;

import java.util.Arrays;
import java.util.Collection;

import de.sgollmer.solvismax.error.AssignmentError;
import de.sgollmer.solvismax.model.Solvis;
import de.sgollmer.solvismax.model.objects.AllSolvisData;
import de.sgollmer.solvismax.model.objects.Dependencies;
import de.sgollmer.solvismax.model.objects.Observer.IObserver;
import de.sgollmer.solvismax.model.objects.SolvisDescription;
import de.sgollmer.solvismax.model.objects.calculation.Strategies.Strategy;
import de.sgollmer.solvismax.model.objects.data.IMode;
import de.sgollmer.solvismax.model.objects.data.SolvisData;

public class BurnerStatus extends Strategy<BurnerStatus> {
	
	public BurnerStatus() {
		super(null) ;
	}


	public BurnerStatus(Calculation calculation) {
		super(calculation);
	}

	@Override
	public BurnerStatus create(Calculation calculation) {
		return new BurnerStatus(calculation);
	}

	public enum Status implements IMode {
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
	public boolean isWriteable() {
		return false;
	}

	@Override
	public void instantiate(Solvis solvis) {
		AllSolvisData allData = solvis.getAllSolvisData();
		Dependencies dependencies = this.calculation.getDependencies();

		SolvisData result = allData.get(this.calculation.getDescription().getId());
		
		if ( result.getMode() == null ) {
			result.setMode(Status.OFF, -1);
		}

		SolvisData burnerLevel1On = dependencies.get(allData, "burnerLevel1On");
		SolvisData burnerLevel2On = dependencies.get(allData, "burnerLevel2On");

		Executable executable = new Executable(result, burnerLevel1On, burnerLevel2On);

		executable.update(burnerLevel1On, this );

	}

	private class Executable implements IObserver<SolvisData> {

		private final SolvisData result;
		private final SolvisData burnerLevel1On;
		private final SolvisData burnerLevel2On;

		public Executable(SolvisData result, SolvisData burnerLevel1On, SolvisData burnerLevel2On) {
			this.result = result;
			this.burnerLevel1On = burnerLevel1On;
			this.burnerLevel2On = burnerLevel2On;
			this.burnerLevel1On.register(this);
			this.burnerLevel2On.register(this);
		}

		@Override
		public void update(SolvisData data, Object source ) {
			if (this.result == null || this.burnerLevel1On == null || this.burnerLevel2On == null) {
				throw new AssignmentError("Assignment error: Dependencies not assigned");
			}

			Status result = null;

			boolean level1 = this.burnerLevel1On.getBool();
			boolean level2 = this.burnerLevel2On.getBool();

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
	public Collection<IMode> getModes() {
		return Arrays.asList(Status.values());
	}


	@Override
	public Double getAccuracy() {
		return null;
	}


	@Override
	public boolean isBoolean() {
		return false;
	}
	
}
