/************************************************************************
 * 
 * $Id$
 *
 * 
 ************************************************************************/

package de.sgollmer.solvismax.model.objects.calculation;

import de.sgollmer.solvismax.error.AssignmentError;
import de.sgollmer.solvismax.model.Solvis;
import de.sgollmer.solvismax.model.objects.AllSolvisData;
import de.sgollmer.solvismax.model.objects.Dependencies;
import de.sgollmer.solvismax.model.objects.Observer.IObserver;
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
	public boolean isWriteable() {
		return false;
	}

	@Override
	public void instantiate(Solvis solvis) {
		AllSolvisData allData = solvis.getAllSolvisData();
		SolvisData result = allData.get(this.calculation.getDescription().getId());
		
		if ( result.getSingleData() == null ) {
			result.setInteger(0, -1);
		}

		Dependencies dependencies = this.calculation.getDependencies();

		SolvisData equipmentOn = dependencies.get(allData, "equipmentOn");

		Executable executable = new Executable(result, equipmentOn);

		executable.update(equipmentOn, this);
	}

	private class Executable implements IObserver<SolvisData> {
		private final SolvisData result;
		private final SolvisData equipmentOn;

		public Executable(SolvisData result, SolvisData equipmentOn) {
			this.result = result;
			this.equipmentOn = equipmentOn;
			this.equipmentOn.register(this);
		}

		@Override
		public void update(SolvisData data, Object source ) {
			if (this.result == null || this.equipmentOn == null) {
				throw new AssignmentError("Assignment error: Dependencies not assigned");
			}

			boolean equipmentOn = data.getBool();

			if (equipmentOn) {
				int result = this.result.getInt();
				++result;
				this.result.setInteger(result, data.getTimeStamp());
			}

		}

	}

	@Override
	public void assign(SolvisDescription description) {
	}

	@Override
	public Double getAccuracy() {
		return (double) 1;
	}

	@Override
	public boolean isBoolean() {
		return false;
	}

}
