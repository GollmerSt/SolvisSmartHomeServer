package de.sgollmer.solvismax.model.objects.calculation;

import de.sgollmer.solvismax.error.AssignmentError;
import de.sgollmer.solvismax.model.Solvis;
import de.sgollmer.solvismax.model.objects.AllSolvisData;
import de.sgollmer.solvismax.model.objects.Dependencies;
import de.sgollmer.solvismax.model.objects.Observer.ObserverI;
import de.sgollmer.solvismax.model.objects.SolvisDescription;
import de.sgollmer.solvismax.model.objects.calculation.Strategies.Strategy;
import de.sgollmer.solvismax.model.objects.data.SolvisData;

public class Starts extends Strategy<Starts> {
	public Starts(Calculation calculation) {
		super(calculation);
	}

	public Starts() {
		super(null);
	}

	@Override
	public Starts create(Calculation calculation) {
		return new Starts(calculation);
	}

	@Override
	public String getUnit() {
		return null;
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

		public Executable(SolvisData result, SolvisData burnerOn) {
			this.result = result;
			this.burnerOn = burnerOn;
			this.burnerOn.register(this);
		}

		@Override
		public void update(SolvisData data, Object source ) {
			if (result == null || burnerOn == null) {
				throw new AssignmentError("Assignment error: Dependencies not assigned");
			}

			boolean burnerOn = data.getBool();

			if (burnerOn) {
				int result = this.result.getInt();
				++result;
				this.result.setInteger(result);
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

	@Override
	public boolean isBoolean() {
		return false;
	}

}
