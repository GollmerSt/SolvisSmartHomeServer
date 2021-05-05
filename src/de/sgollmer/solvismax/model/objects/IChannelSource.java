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

	public ChannelDescription getRestoreChannel(Solvis solvis);

	public DependencyGroup getDependencyGroup();

	public boolean mustBackuped();

	public boolean getValue(SolvisData dest, Solvis solvis, long executionStartTime)
			throws IOException, PowerOnException, TerminationException;

	public static class SetResult {
		private final ResultStatus status;
		private final SingleData<?> data;

		public SetResult(ResultStatus status, SingleData<?> data) {
			this.status = status;
			this.data = data;
		}

		public ResultStatus getStatus() {
			return this.status;
		}

		public SingleData<?> getData() {
			return this.data;
		}
	}

	public SetResult setValue(Solvis solvis, SolvisData value) throws IOException, TerminationException;

	public SingleData<?> interpretSetData(SingleData<?> singleData) throws TypeException;

	public boolean isWriteable();

	public boolean isAverage();

	public Integer getDivisor();

	public Double getAccuracy();

	public boolean isBoolean();

	public boolean isFast();

	public Integer getScanInterval_ms(Solvis solvis);

	public IInstance instantiate(Solvis solvis) throws AssignmentException, AliasException;

	public Type getType();

	public enum Type {
		CONTROL, CALCULATION, MEASUREMENT
	}

	public Collection<? extends IMode<?>> getModes();

	public AbstractScreen getScreen(Solvis solvis);

	public UpperLowerStep getUpperLowerStep();

	public boolean isDelayed(Solvis solvis);

	public boolean isHumanAccessDependend();

	public static class UpperLowerStep {
		private final double upper;
		private final double lower;
		private final double step;
		private final Double incrementChange;
		private final Double changedIncrement;

		public UpperLowerStep(double upper, double lower, double step, Double incrementChange,
				Double changedIncrement) {
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

	public String getCsvMeta(String column, boolean semicolon);
}
