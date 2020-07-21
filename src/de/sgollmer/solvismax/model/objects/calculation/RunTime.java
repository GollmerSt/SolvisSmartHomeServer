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

public class RunTime extends Strategy<RunTime> {

	private RunTime(Calculation calculation) {
		super(calculation);
	}

	RunTime() {
		super(null);
	}

	@Override
	public RunTime create(Calculation calculation) {
		return new RunTime(calculation);
	}

	@Override
	boolean isWriteable() {
		return false;
	}

	@Override
	void instantiate(Solvis solvis) {
		AllSolvisData allData = solvis.getAllSolvisData();
		SolvisData result = allData.get(this.calculation.getDescription().getId());

		if (result.getSingleData() == null) {
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
		private long lastStartTime = -1;
		private int formerRunTime_s = -1;

		private Executable(SolvisData result, SolvisData equipmentOn) {
			this.result = result;
			this.equipmentOn = equipmentOn;
			this.equipmentOn.registerContinuousObserver(this);
			this.result.registerContinuousObserver(this);
		}

		@Override
		public void update(SolvisData data, Object source) {
			if (this.result == null || this.equipmentOn == null) {
				throw new AssignmentError("Assignment error: Dependencies not assigned");
			}

			Boolean equipmentOn = null;

			if (data.getDescription() == this.result.getDescription()) {
				if (source != this) {
					equipmentOn = this.equipmentOn.getBool();
					this.lastStartTime = -1;
				} else {
					return;
				}
			}

			if (equipmentOn == null) {
				equipmentOn = data.getBool();
			}

			if (equipmentOn || this.lastStartTime >= 0) {

				long time = System.currentTimeMillis();

				int former = this.result.getInt();

				if (equipmentOn) {
					if (this.lastStartTime < 0) {
						this.lastStartTime = time;
						this.formerRunTime_s = former;
					}
				}

				int result = this.formerRunTime_s + (int) ((time - this.lastStartTime + 500) / 1000);

				if (result - former > 60 || !equipmentOn) {
					this.result.setInteger(result, data.getTimeStamp(), this);
				}

				if (!equipmentOn) {
					this.lastStartTime = -1;
				}
			}
		}

	}

	@Override
	public void assign(SolvisDescription description) {
	}

	@Override
	Double getAccuracy() {
		return (double) 1;
	}

	@Override
	boolean isBoolean() {
		return false;
	}

}
