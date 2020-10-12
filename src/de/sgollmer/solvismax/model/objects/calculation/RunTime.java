/************************************************************************
 * 
 * $Id$
 *
 * 
 ************************************************************************/

package de.sgollmer.solvismax.model.objects.calculation;

import de.sgollmer.solvismax.error.AssignmentException;
import de.sgollmer.solvismax.error.AliasException;
import de.sgollmer.solvismax.error.TypeException;
import de.sgollmer.solvismax.log.LogManager;
import de.sgollmer.solvismax.log.LogManager.ILogger;
import de.sgollmer.solvismax.model.Solvis;
import de.sgollmer.solvismax.model.objects.AllSolvisData;
import de.sgollmer.solvismax.model.objects.Observer.IObserver;
import de.sgollmer.solvismax.model.objects.SolvisDescription;
import de.sgollmer.solvismax.model.objects.calculation.Strategies.Strategy;
import de.sgollmer.solvismax.model.objects.data.SolvisData;

public class RunTime extends Strategy<RunTime> {

	private static final ILogger logger = LogManager.getInstance().getLogger(RunTime.class);

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
	void instantiate(Solvis solvis) throws AssignmentException, AliasException {
		AllSolvisData allData = solvis.getAllSolvisData();
		SolvisData result = allData.get(this.calculation.getDescription().getId());

		AliasGroup dependencies = this.calculation.getCalculationDependencies();

		SolvisData equipmentOn = dependencies.get(allData, "equipmentOn");

		if (result == null || equipmentOn == null) {
			throw new AssignmentException("Assignment error: DependencyGroup not assigned");
		}

		if (result.getSingleData() == null) {
			result.setInteger(0, -1);
		}

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

			try {

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
			} catch (TypeException e) {
				logger.error("Type error, update ignored", e);
				return;
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
