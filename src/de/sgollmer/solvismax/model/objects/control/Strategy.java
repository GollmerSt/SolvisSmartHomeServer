/************************************************************************
 * 
 * $Id: 73_SolvisClient.pm 78 2020-01-03 17:50:08Z stefa $
 *
 * 
 ************************************************************************/

package de.sgollmer.solvismax.model.objects.control;

import java.io.IOException;
import java.util.Collection;

import de.sgollmer.solvismax.error.TerminationException;
import de.sgollmer.solvismax.imagepatternrecognition.image.MyImage;
import de.sgollmer.solvismax.model.Solvis;
import de.sgollmer.solvismax.model.objects.Assigner;
import de.sgollmer.solvismax.model.objects.ChannelSourceI.UpperLowerStep;
import de.sgollmer.solvismax.model.objects.data.ModeI;
import de.sgollmer.solvismax.model.objects.data.SingleData;
import de.sgollmer.solvismax.model.objects.data.SolvisData;
import de.sgollmer.solvismax.objects.Rectangle;

public interface Strategy extends Assigner {

	public SingleData<?> getValue(MyImage currentImage, Rectangle valueRectangle, Solvis solvis) throws TerminationException, IOException;

	public boolean setValue(Solvis solvis, Rectangle rectangle, SolvisData value) throws IOException, TerminationException;

	public boolean isWriteable();

	public Integer getDivisor() ;
	
	public String getUnit() ;

	public Float getAccuracy();

	public Collection<? extends ModeI> getModes();

	public UpperLowerStep getUpperLowerStep();
	
	public void setCurrentRectangle( Rectangle rectangle ) ;
	
	public boolean mustBeLearned() ;
	
	public boolean learn(Solvis solvis) throws IOException ;

}
