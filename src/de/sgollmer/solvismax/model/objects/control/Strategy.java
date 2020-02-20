/************************************************************************
 * 
 * $Id$
 *
 * 
 ************************************************************************/

package de.sgollmer.solvismax.model.objects.control;

import java.io.IOException;
import java.util.List;

import de.sgollmer.solvismax.error.TerminationException;
import de.sgollmer.solvismax.error.TypeError;
import de.sgollmer.solvismax.model.Solvis;
import de.sgollmer.solvismax.model.objects.Assigner;
import de.sgollmer.solvismax.model.objects.ChannelSourceI.UpperLowerStep;
import de.sgollmer.solvismax.model.objects.data.ModeI;
import de.sgollmer.solvismax.model.objects.data.SingleData;
import de.sgollmer.solvismax.model.objects.data.SolvisData;
import de.sgollmer.solvismax.model.objects.screen.SolvisScreen;
import de.sgollmer.solvismax.objects.Rectangle;

public interface Strategy extends Assigner {

	public SingleData<?> getValue(SolvisScreen solvisScreen, Rectangle valueRectangl) throws TerminationException, IOException;

	public SingleData<?> setValue(Solvis solvis, Rectangle rectangle, SolvisData value)
			throws IOException, TerminationException, TypeError;

	public boolean isWriteable();

	public Integer getDivisor() ;
	
	public String getUnit() ;

	public Float getAccuracy();

	public List<? extends ModeI> getModes();

	public UpperLowerStep getUpperLowerStep();
	
	public void setCurrentRectangle( Rectangle rectangle ) ;
	
	public boolean mustBeLearned() ;
	
	public boolean learn(Solvis solvis) throws IOException ;

	public SingleData<?> interpretSetData(SingleData<?> singleData)  throws TypeError;

}
