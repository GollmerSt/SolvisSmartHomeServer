package de.sgollmer.solvismax.model.objects.control;

import de.sgollmer.solvismax.imagepatternrecognition.image.MyImage;
import de.sgollmer.solvismax.model.Solvis;
import de.sgollmer.solvismax.model.objects.data.SingleData;
import de.sgollmer.solvismax.model.objects.data.SolvisData;
import de.sgollmer.solvismax.objects.Rectangle;

public interface Strategy {
	public Boolean setValue(Solvis solvis, Rectangle rectangle, SolvisData value);

	public boolean isWriteable();

	public SingleData getValue(MyImage image, Rectangle rectangle);

	public Integer getDivisor() ;
	
	public String getUnit() ;

}
