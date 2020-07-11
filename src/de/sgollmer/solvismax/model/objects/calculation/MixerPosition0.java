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

public class MixerPosition0 extends Strategy<MixerPosition0> {

	public MixerPosition0(Calculation calculation) {
		super(calculation);
	}

	public MixerPosition0() {
		super(null);
	}

	@Override
	public MixerPosition0 create(Calculation calculation) {
		return new MixerPosition0(calculation);
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
			result.setBoolean(false, -1);
		}

		Dependencies dependencies = this.calculation.getDependencies();

		SolvisData pumpOn = dependencies.get(allData, "pumpOn");
		SolvisData mixerClosing = dependencies.get(allData, "mixerClosing");

		Executable executable = new Executable(result, pumpOn, mixerClosing);

		executable.update(pumpOn, this);
	}

	private class Executable implements IObserver<SolvisData> {

		private final SolvisData result;
		private final SolvisData pumpOn;
		private final SolvisData mixerClosing;

		public Executable(SolvisData result, SolvisData pumpOn, SolvisData mixerClosing) {
			this.result = result;
			this.pumpOn = pumpOn;
			this.mixerClosing = mixerClosing;
			this.pumpOn.register(this);
			this.mixerClosing.register(this);
		}

		@Override
		public void update(SolvisData data, Object source) {
			if (this.result == null || this.pumpOn == null || this.mixerClosing == null) {
				throw new AssignmentError("Assignment error: Dependencies not assigned");
			}

			boolean result = this.result.getBool();
			boolean mixer = this.mixerClosing.getBool();
			boolean pump = this.pumpOn.getBool();

			result = !pump && !mixer;

			this.result.setBoolean(result, data.getTimeStamp());

		}

	}

	@Override
	public void assign(SolvisDescription description) {

	}

	@Override
	public Double getAccuracy() {
		return null;
	}

	@Override
	public boolean isBoolean() {
		return true;
	}

}
