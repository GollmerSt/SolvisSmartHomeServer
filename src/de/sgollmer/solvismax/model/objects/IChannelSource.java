/************************************************************************
 * 
 * $Id$
 *
 * 
 ************************************************************************/

package de.sgollmer.solvismax.model.objects;

import java.io.IOException;
import java.util.Collection;

import de.sgollmer.solvismax.error.AssignmentException;
import de.sgollmer.solvismax.error.DependencyException;
import de.sgollmer.solvismax.error.ModbusException;
import de.sgollmer.solvismax.error.PowerOnException;
import de.sgollmer.solvismax.error.TerminationException;
import de.sgollmer.solvismax.error.TypeException;
import de.sgollmer.solvismax.model.Solvis;
import de.sgollmer.solvismax.model.objects.data.IMode;
import de.sgollmer.solvismax.model.objects.data.SingleData;
import de.sgollmer.solvismax.model.objects.data.SolvisData;
import de.sgollmer.solvismax.model.objects.screen.IGraficsLearnable;
import de.sgollmer.solvismax.model.objects.screen.AbstractScreen;

public interface IChannelSource extends IAssigner, IGraficsLearnable {

	public ChannelDescription getRestoreChannel(Solvis solvis);

	public boolean getValue(SolvisData dest, Solvis solvis)
			throws IOException, PowerOnException, TerminationException, ModbusException;

	public enum Status {
		SUCCESS, INTERRUPTED, NO_SUCCESS, UNKNOWN, VALUE_VIOLATION
	}

	public static class SetResult {
		private final Status status;
		private final SingleData<?> data;

		public SetResult(Status status, SingleData<?> data) {
			this.status = status;
			this.data = data;
		}

		public Status getStatus() {
			return this.status;
		}

		public SingleData<?> getData() {
			return this.data;
		}
	}

	public SetResult setValue(Solvis solvis, SolvisData value)
			throws IOException, TerminationException, ModbusException;

	public SingleData<?> interpretSetData(SingleData<?> singleData) throws TypeException;

	public boolean isWriteable();

	public boolean isAverage();

	public Integer getDivisor();

	public Double getAccuracy();

	public boolean isBoolean();

	public void instantiate(Solvis solvis) throws AssignmentException, DependencyException;

	public Type getType();

	public enum Type {
		CONTROL, CALCULATION, MEASUREMENT
	}

	public Collection<? extends IMode> getModes();

	public AbstractScreen getScreen(int configurationMask);

	public AbstractScreen getScreen(Solvis solvis);

	public UpperLowerStep getUpperLowerStep();

	public boolean isModbus(Solvis solvis);

	public boolean isScreenChangeDependend();

	public static class UpperLowerStep {
		private final double upper;
		private final double lower;
		private final double step;

		public UpperLowerStep(float upper, float lower, float step) {
			this.upper = upper;
			this.lower = lower;
			this.step = step;
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
	}

}
