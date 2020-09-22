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
import de.sgollmer.solvismax.model.objects.Dependencies;
import de.sgollmer.solvismax.model.objects.Observer.IObserver;
import de.sgollmer.solvismax.model.objects.SolvisDescription;
import de.sgollmer.solvismax.model.objects.calculation.Strategies.Strategy;
import de.sgollmer.solvismax.model.objects.data.SolvisData;

public class Starts extends Strategy<Starts> {
	private static final ILogger logger = LogManager.getInstance().getLogger(Starts.class);

	private Starts(Calculation calculation) {
		super(calculation);
	}

	Starts() {
		super(null);
	}

	@Override
	public Starts create(Calculation calculation) {
		return new Starts(calculation);
	}

	@Override
	boolean isWriteable() {
		return false;
	}

	@Override
	void instantiate(Solvis solvis) throws AssignmentException, AliasException {
		AllSolvisData allData = solvis.getAllSolvisData();
		SolvisData result = allData.get(this.calculation.getDescription().getId());

		if (result.getSingleData() == null) {
			result.setInteger(0, -1);
		}

		Dependencies dependencies = this.calculation.getDependencies();

		SolvisData equipmentOn = dependencies.get(allData, "equipmentOn");

		if (result == null || equipmentOn == null) {
			throw new AssignmentException("Assignment error: Dependencies not assigned");
		}
		Executable executable = new Executable(result, equipmentOn);

		executable.update(equipmentOn, this);
	}

	private class Executable implements IObserver<SolvisData> {
		private final SolvisData result;
		private final SolvisData equipmentOn;
		private boolean former = false;

		private Executable(SolvisData result, SolvisData equipmentOn) {
			this.result = result;
			this.equipmentOn = equipmentOn;
			this.equipmentOn.registerContinuousObserver(this);
		}

		@Override
		public void update(SolvisData data, Object source) {

			try {
				boolean equipmentOn = data.getBool();
				if (equipmentOn && !this.former) {
					int result = this.result.getInt();
					++result;
					this.result.setInteger(result, data.getTimeStamp());
				}
				this.former = equipmentOn;
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
