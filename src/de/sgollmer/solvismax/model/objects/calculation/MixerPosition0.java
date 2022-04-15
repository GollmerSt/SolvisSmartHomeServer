/************************************************************************
 * 
 * $Id$
 *
 * 
 ************************************************************************/

package de.sgollmer.solvismax.model.objects.calculation;

import de.sgollmer.solvismax.error.AliasException;
import de.sgollmer.solvismax.error.AssignmentException;
import de.sgollmer.solvismax.error.TypeException;
import de.sgollmer.solvismax.helper.SolvisDataHelper;
import de.sgollmer.solvismax.log.LogManager;
import de.sgollmer.solvismax.log.LogManager.ILogger;
import de.sgollmer.solvismax.model.Solvis;
import de.sgollmer.solvismax.model.objects.AllSolvisData;
import de.sgollmer.solvismax.model.objects.Observer.IObserver;
import de.sgollmer.solvismax.model.objects.calculation.Strategies.Strategy;
import de.sgollmer.solvismax.model.objects.data.SingleData;
import de.sgollmer.solvismax.model.objects.data.SolvisData;

public class MixerPosition0 extends Strategy<MixerPosition0> {

	private static final ILogger logger = LogManager.getInstance().getLogger(MixerPosition0.class);

	private MixerPosition0(final Calculation calculation) {
		super(calculation);
	}

	MixerPosition0() {
		super(null);
	}

	@Override
	public MixerPosition0 create(final Calculation calculation) {
		return new MixerPosition0(calculation);
	}

	@Override
	boolean isWriteable() {
		return false;
	}

	@Override
	void instantiate(final Solvis solvis) throws AssignmentException, AliasException, TypeException {
		AllSolvisData allData = solvis.getAllSolvisData();
		SolvisData result = allData.get(this.calculation.getDescription().getId());

		AliasGroup aliasGroup = this.calculation.getAliasGroup();

		SolvisData pumpOn = aliasGroup.get(allData, "pumpOn");
		SolvisData mixerClosing = aliasGroup.get(allData, "mixerClosing");

		if (result == null || pumpOn == null || mixerClosing == null) {
			throw new AssignmentException("Assignment error: AliasGroup not assigned");
		}

		if (result.getSingleData() == null) {
			result.setBoolean(false, -1);
		}

		Executable executable = new Executable(result, pumpOn, mixerClosing);

		executable.update(pumpOn, this);
	}

	private class Executable implements IObserver<SolvisData> {

		private final SolvisData result;
		private final SolvisData pumpOn;
		private final SolvisData mixerClosing;

		private Executable(final SolvisData result, final SolvisData pumpOn, final SolvisData mixerClosing) {
			this.result = result;
			this.pumpOn = pumpOn;
			this.mixerClosing = mixerClosing;
			this.pumpOn.registerContinuousObserver(this);
			this.mixerClosing.registerContinuousObserver(this);
		}

		@Override
		public void update(final SolvisData data, final Object source) {

			boolean mixer;
			boolean pump;
			try {
				this.result.getBool();
				mixer = this.mixerClosing.getBool();
				pump = this.pumpOn.getBool();
			} catch (TypeException e) {
				logger.error("Type error, update ignored", e);
				return;
			}

			boolean result = !pump && !mixer;

			try {
				this.result.setBoolean(result, data.getTimeStamp());
			} catch (TypeException e) {
				logger.error("Type exception of " + data.getName() + ", ignored");
			}

		}

	}

	@Override
	Double getAccuracy() {
		return null;
	}

	@Override
	boolean isBoolean() {
		return true;
	}

	@Override
	protected SingleData<?> interpretSetData(final SingleData<?> singleData) {
		return SolvisDataHelper.toBoolean(singleData);
	}

}
