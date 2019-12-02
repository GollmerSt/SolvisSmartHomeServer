package de.sgollmer.solvismax.model.objects.control;

import java.io.IOException;

import de.sgollmer.solvismax.imagepatternrecognition.image.MyImage;
import de.sgollmer.solvismax.model.Solvis;
import de.sgollmer.solvismax.model.objects.Assigner;
import de.sgollmer.solvismax.model.objects.data.SingleData;
import de.sgollmer.solvismax.model.objects.data.SolvisData;
import de.sgollmer.solvismax.objects.Rectangle;

public interface Strategy extends Assigner {

	public SingleData getValue(MyImage currentImage, Rectangle valueRectangle, Solvis solvis);

	public Boolean setValue(Solvis solvis, Rectangle rectangle, SolvisData value) throws IOException;

	public boolean isWriteable();

	public Integer getDivisor() ;
	
	public String getUnit() ;


}
