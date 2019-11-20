package de.sgollmer.solvismax.model.objects.control;

import java.util.ArrayList;
import java.util.Collection;

import de.sgollmer.solvismax.imagepatternrecognition.image.MyImage;
import de.sgollmer.solvismax.imagepatternrecognition.pattern.Pattern;
import de.sgollmer.solvismax.model.Solvis;
import de.sgollmer.solvismax.model.objects.Mode;
import de.sgollmer.solvismax.model.objects.ScreenGrafic;
import de.sgollmer.solvismax.model.objects.data.SingleData;
import de.sgollmer.solvismax.model.objects.data.SolvisData;
import de.sgollmer.solvismax.model.objects.data.StringData;
import de.sgollmer.solvismax.objects.Rectangle;

public class StrategyMode implements Strategy {
	
	private final Collection< Mode > modes = new ArrayList<>() ;

	@Override
	public boolean isWriteable() {
		return true;
	}

	@Override
	public SingleData getValue( MyImage source, Rectangle rectangle) {
		Pattern pattern = null ;
		MyImage image = new MyImage(source, rectangle) ;
		for ( Mode mode : this.modes ) {
			ScreenGrafic cmp = mode.getGrafic() ;
			MyImage cmpImage = image ;
			if ( cmp.isExact() ) {
				if ( pattern == null ) {
					pattern = new Pattern(source.create(rectangle)) ;
				}
				cmpImage = pattern ;
			}
			if ( cmp.isElementOf(cmpImage) ) {
				return new StringData( mode.getId()) ; 
			}
		}
		return null ;
	}

	@Override
	public Boolean setValue(Solvis solvis, Rectangle rectangle, SolvisData value) {
		SingleData cmp  = this.getValue( solvis.getCurrentImage(), rectangle) ;
		if ( cmp != null && value.equals(cmp)) {
			return true ;
		}
		Mode mode = null ;
		for ( Mode m : modes ) {
			if ( value.equals(m.getId()) ) {
				mode = m ;
				break ;
			}
		}
		if ( mode == null ) {
			return null ;
		}
		solvis.send(mode.getTouch());
		return false;
	}

	@Override
	public Integer getDivisor() {
		return null;
	}

	@Override
	public String getUnit() {
		return null;
	}

}
