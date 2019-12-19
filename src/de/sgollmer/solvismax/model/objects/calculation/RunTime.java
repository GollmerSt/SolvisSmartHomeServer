package de.sgollmer.solvismax.model.objects.calculation;

import de.sgollmer.solvismax.error.AssignmentError;
import de.sgollmer.solvismax.model.Solvis;
import de.sgollmer.solvismax.model.objects.AllSolvisData;
import de.sgollmer.solvismax.model.objects.Dependencies;
import de.sgollmer.solvismax.model.objects.Observer.ObserverI;
import de.sgollmer.solvismax.model.objects.SolvisDescription;
import de.sgollmer.solvismax.model.objects.calculation.Strategies.Strategy;
import de.sgollmer.solvismax.model.objects.data.SolvisData;

public class RunTime extends Strategy<RunTime> {

	public RunTime(Calculation calculation) {
		super(calculation);
	}

	public RunTime() {
		super(null);
	}

	@Override
	public RunTime create(Calculation calculation) {
		return new RunTime(calculation);
	}

	@Override
	public String getUnit() {
		return "s";
	}

	@Override
	public boolean isWriteable() {
		return false;
	}

	@Override
	public void instantiate(Solvis solvis) {
		AllSolvisData allData = solvis.getAllSolvisData();
		SolvisData result = allData.get(this.calculation.getDescription().getId());

		Dependencies dependencies = this.calculation.getDependencies();

		SolvisData burnerOn = dependencies.get(allData, "burnerOn");

		Executable executable = new Executable(result, burnerOn);

		executable.update(burnerOn, this);
	}

	private class Executable implements ObserverI<SolvisData> {

		private final SolvisData result;
		private final SolvisData burnerOn;
		private long lastStartTime = -1;
		private int formerRunTime_s = -1;

		public Executable(SolvisData result, SolvisData burnerOn) {
			this.result = result;
			this.burnerOn = burnerOn;
			this.burnerOn.registerContinuousObserver(this);
			this.result.registerContinuousObserver(this);
		}

		@Override
		public void update(SolvisData data, Object source ) {
			if (result == null || burnerOn == null) {
				throw new AssignmentError("Assignment error: Dependencies not assigned");
			}
			
			Boolean burnerOn = null ;

			if ( data.getDescription() == result.getDescription() ) {
				if ( source != this ) {
					burnerOn = this.burnerOn.getBool() ;
					this.lastStartTime = -1 ;
				}
				else {
					return ;
				}
			}
			
			if ( burnerOn == null ) {
				burnerOn = data.getBool() ;
			}


			if (burnerOn || this.lastStartTime >= 0) {

				long time = System.currentTimeMillis();

				int former = this.result.getInt();

				if (burnerOn) {
					if (this.lastStartTime < 0) {
						this.lastStartTime = time;
						this.formerRunTime_s = former;
					}
				}
				
				int result = this.formerRunTime_s + (int) ((time - this.lastStartTime + 500) / 1000);

				if (result - former > 60 || ! burnerOn ) {
					this.result.setInteger(result, this);
				}
				
				if ( !burnerOn ) {
					this.lastStartTime = -1;
				}
			}
		}

	}

	@Override
	public void assign(SolvisDescription description) {
	}

	@Override
	public Float getAccuracy() {
		return (float) 1;
	}

}
