/************************************************************************
 * 
 * $Id$
 *
 * 
 ************************************************************************/

package de.sgollmer.solvismax.model.objects;

import java.io.IOException;
import java.util.Collection;

import de.sgollmer.solvismax.error.TypeError;
import de.sgollmer.solvismax.model.Solvis;
import de.sgollmer.solvismax.model.objects.data.IMode;
import de.sgollmer.solvismax.model.objects.data.SingleData;
import de.sgollmer.solvismax.model.objects.data.SolvisData;
import de.sgollmer.solvismax.model.objects.screen.IGraficsLearnable;
import de.sgollmer.solvismax.model.objects.screen.Screen;

public interface IChannelSource extends IAssigner, IGraficsLearnable {
	
	public ChannelDescription getRestoreChannel( Solvis solvis) ;

	public boolean getValue(SolvisData dest, Solvis solvis) throws IOException;

	public SingleData<?> setValue(Solvis solvis, SolvisData value) throws IOException;

	public SingleData<?> interpretSetData(SingleData<?> singleData) throws TypeError;

	public boolean isWriteable();

	public boolean isAverage();

	public Integer getDivisor();

	public Double getAccuracy();

	public boolean isBoolean();

	public void instantiate(Solvis solvis);

	public Type getType();

	public enum Type {
		CONTROL, CALCULATION, MEASUREMENT
	}

	public Collection<? extends IMode> getModes();

	public Screen getScreen(int configurationMask);

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
