package de.sgollmer.solvismax.model.objects.calculation;

import de.sgollmer.solvismax.error.AssignmentError;
import de.sgollmer.solvismax.model.Solvis;
import de.sgollmer.solvismax.model.objects.AllDataDescriptions;
import de.sgollmer.solvismax.model.objects.AllSolvisData;
import de.sgollmer.solvismax.model.objects.Dependencies;
import de.sgollmer.solvismax.model.objects.Observer.ObserverI;
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
	public String getUnit() {
		return null;
	}

	@Override
	public boolean isWriteable() {
		return true;
	}

	@Override
	public void instantiate(Solvis solvis) {
		AllSolvisData allData = solvis.getAllSolvisData();
		SolvisData result = allData.get(this.calculation.getId());
		
		Dependencies dependencies = this.calculation.getDependencies() ;

		SolvisData pumpOn = dependencies.get(allData, "pumpOn") ;
		SolvisData mixerClosing = dependencies.get(allData, "mixerClosing") ;

		Executable executable = new Executable(result, pumpOn, mixerClosing);

		executable.update( pumpOn );
	}

	private class Executable implements ObserverI<SolvisData> {

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
		public void update(SolvisData data) {
			if (result == null || pumpOn == null || mixerClosing == null) {
				throw new AssignmentError("Assignment error: Dependencies not assigned");
			}

			boolean result = this.result.getBool();
			boolean mixer = this.mixerClosing.getBool();
			boolean pump = this.pumpOn.getBool();

			result = !pump && !mixer;

			this.result.setBoolean(result);

		}

	}

	@Override
	public void assign(AllDataDescriptions descriptions) {		
	}

}
