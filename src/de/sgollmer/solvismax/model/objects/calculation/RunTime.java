package de.sgollmer.solvismax.model.objects.calculation;

import de.sgollmer.solvismax.error.AssignmentError;
import de.sgollmer.solvismax.model.Solvis;
import de.sgollmer.solvismax.model.objects.AllDataDescriptions;
import de.sgollmer.solvismax.model.objects.AllSolvisData;
import de.sgollmer.solvismax.model.objects.Dependencies;
import de.sgollmer.solvismax.model.objects.Observer.ObserverI;
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
		return true;
	}

	@Override
	public void instantiate(Solvis solvis) {
		AllSolvisData allData = solvis.getAllSolvisData();
		SolvisData result = allData.get(this.calculation.getId());

		Dependencies dependencies = this.calculation.getDependencies();

		SolvisData burnerOn = dependencies.get(allData, "burnerOn");

		Executable executable = new Executable(result, burnerOn);

		executable.update(burnerOn);
	}

	@Override
	public void assign(AllDataDescriptions descriptions) {
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
		}

		@Override
		public void update(SolvisData data) {
			if (result == null || burnerOn == null) {
				throw new AssignmentError("Assignment error: Dependencies not assigned");
			}

			boolean burnerOn = data.getBool();

			if (burnerOn || this.lastStartTime >= 0) {

				long time = System.currentTimeMillis();

				int former = this.result.getInt();

				if (burnerOn) {
					if (this.lastStartTime < 0) {
						this.lastStartTime = time;
						this.formerRunTime_s = former;
					}
				} else {
					this.lastStartTime = -1;
				}
				int result = this.formerRunTime_s + (int) ((time - this.lastStartTime + 500) / 1000);

				if (result / 60 - former / 60 > 0 || this.lastStartTime < 0 ) {
					this.result.setInteger(result);
				}
			}
		}

	}

}