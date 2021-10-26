/************************************************************************
 * 
 * $Id$
 *
 * 
 ************************************************************************/

package de.sgollmer.solvismax.model.objects;

import java.io.IOException;
import java.util.Collection;

import de.sgollmer.solvismax.error.AliasException;
import de.sgollmer.solvismax.error.AssignmentException;
import de.sgollmer.solvismax.error.PowerOnException;
import de.sgollmer.solvismax.error.TerminationException;
import de.sgollmer.solvismax.error.TypeException;
import de.sgollmer.solvismax.model.Solvis;
import de.sgollmer.solvismax.model.objects.control.DependencyGroup;
import de.sgollmer.solvismax.model.objects.data.IMode;
import de.sgollmer.solvismax.model.objects.data.SingleData;
import de.sgollmer.solvismax.model.objects.data.SolvisData;
import de.sgollmer.solvismax.model.objects.screen.AbstractScreen;
import de.sgollmer.solvismax.model.objects.screen.IGraficsLearnable;

public interface IChannelSource extends IAssigner, IGraficsLearnable {

	public ChannelDescription getRestoreChannel(final Solvis solvis);

	public DependencyGroup getDependencyGroup();

	public boolean mustBackuped();

	public boolean getValue(final SolvisData dest, final Solvis solvis, final long executionStartTime)
			throws IOException, PowerOnException, TerminationException, TypeException;

	public static class SetResult {
		private final ResultStatus status;
		private final SingleData<?> data;
		private final boolean forceTransmit;

		public SetResult(final ResultStatus status, final SingleData<?> data, final boolean forceTransmit) {
			this.status = status;
			this.data = data;
			this.forceTransmit = forceTransmit;
		}

		public ResultStatus getStatus() {
			return this.status;
		}

		public SingleData<?> getData() {
			return this.data;
		}

		public boolean isForceTransmit() {
			return this.forceTransmit;
		}
	}

	public SetResult setValue(final Solvis solvis, final SolvisData value) throws IOException, TerminationException;

	public SetResult setDebugValue(final Solvis solvis, final SingleData<?> value)
			throws IOException, TerminationException, TypeException;

	public SetResult setValueFast(final Solvis solvis, final SolvisData value) throws IOException, TerminationException;

	/**
	 * 
	 * @param realData Datum, wie es auf der Smarthome-Seite dargestellt wird
	 * @return
	 * @throws TypeException
	 */
	public SingleData<?> interpretSetData(final SingleData<?> realData, final boolean debug) throws TypeException;

	public boolean isWriteable();

	public boolean isAverage();

	public Integer getDivisor();

	public Double getAccuracy();

	public boolean isBoolean();

	public boolean isFast();

	public boolean inhibitGuiReadAfterWrite();

	/**
	 * Get the maximum interval of the scanning
	 * 
	 * @param solvis
	 * @return interval if defined, otherwise null
	 */
	public Integer getScanInterval_ms(final Solvis solvis);

	public IInstance instantiate(final Solvis solvis) throws AssignmentException, AliasException, TypeException;

	public Type getType();

	public enum Type {
		CONTROL, CALCULATION, MEASUREMENT
	}

	public Collection<? extends IMode<?>> getModes();

	public AbstractScreen getScreen(final Solvis solvis);

	public UpperLowerStep getUpperLowerStep();

	public boolean isDelayed(final Solvis solvis);

	public boolean isHumanAccessDependend();

	public static class UpperLowerStep {
		private final double upper;
		private final double lower;
		private final double step;
		private final Double incrementChange;
		private final Double changedIncrement;

		public UpperLowerStep(final double upper, final double lower, final double step, final Double incrementChange,
				final Double changedIncrement) {
			this.upper = upper;
			this.lower = lower;
			this.step = step;
			this.incrementChange = incrementChange;
			this.changedIncrement = changedIncrement;
		}

		public double getUpper() {
			return this.upper;
		}

		public double getLower() {
			return this.lower;
		}

		public double getStep() {
			return this.step;
		}

		public Double getIncrementChange() {
			return this.incrementChange;
		}

		public Double getChangedIncrement() {
			return this.changedIncrement;
		}

	}

	public String getCsvMeta(final String column, final boolean semicolon);

	boolean mustPolling();
}
