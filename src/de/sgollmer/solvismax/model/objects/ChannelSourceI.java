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
import de.sgollmer.solvismax.model.objects.data.ModeI;
import de.sgollmer.solvismax.model.objects.data.SingleData;
import de.sgollmer.solvismax.model.objects.data.SolvisData;
import de.sgollmer.solvismax.model.objects.screen.GraficsLearnable;
import de.sgollmer.solvismax.model.objects.screen.Screen;

public interface ChannelSourceI extends Assigner, GraficsLearnable {

	public boolean getValue(SolvisData dest, Solvis solvis, int timeAfterLastSwitchingOn) throws IOException;

	public boolean setValue(Solvis solvis, SolvisData value) throws IOException;

	public SingleData<?> interpretSetData(SingleData<?> singleData)  throws TypeError;

	public boolean isWriteable();

	public boolean isAverage();

	public Integer getDivisor();

	public String getUnit();
	
	public Float getAccuracy() ;
	
	public boolean isBoolean() ;

	public void instantiate(Solvis solvis);
	
	public Type getType() ;
	
	public enum Type {
		CONTROL, CALCULATION, MEASUREMENT
	}
	
	public Collection< ? extends ModeI > getModes() ;
	
	public Screen getScreen( int configurationMask ) ;
	
	public UpperLowerStep getUpperLowerStep() ;

	public static class UpperLowerStep{
		private final float upper ;
		private final float lower ;
		private final float step ;
		
		public UpperLowerStep( float upper, float lower, float step ) {
			this.upper = upper ;
			this.lower = lower ;
			this.step = step ;
		}

		public float getUpper() {
			return upper;
		}

		public float getLower() {
			return lower;
		}

		public float getStep() {
			return step;
		}
	}


}
