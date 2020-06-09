package de.sgollmer.solvismax.model.objects.control;

import java.io.IOException;

import de.sgollmer.solvismax.error.TerminationException;
import de.sgollmer.solvismax.helper.ImageHelper;
import de.sgollmer.solvismax.imagepatternrecognition.image.MyImage;
import de.sgollmer.solvismax.imagepatternrecognition.pattern.Pattern;
import de.sgollmer.solvismax.model.Solvis;
import de.sgollmer.solvismax.objects.Coordinate;
import de.sgollmer.solvismax.objects.Rectangle;

public class Button extends Pattern {

	private final int pushTime;
	private final int releaseTime;

	public Button(MyImage image, Rectangle rectangle, int pushTime, int releaseTime) {
		super(image, rectangle);
		this.pushTime = pushTime;
		this.releaseTime = releaseTime;
	}

	private Boolean selected = null;

	public boolean isSelected() {
		if (this.selected == null) {

			int y = this.getHeight() / 2;

			int changeCnt = 0 ;
			
			boolean frame = false ;
			int former = ImageHelper.getBrightness(this.getRGB(0, 0));

			for (int x = 0; x < this.getWidth()/2 && this.selected == null ; ++x) {
				int brightness = ImageHelper.getBrightness(this.getRGB(x, y));
				if (brightness != former) {
					++changeCnt;
					if ( changeCnt == 2 ) {
						if ( x < 2 ) {
							frame = true ;
						}
					}
					if ( frame && changeCnt == 3 || !frame && changeCnt == 2) {
						this.selected = brightness > former ;
					}
					former = brightness;
				}
			}
			if ( this.selected == null ) {
				this.selected = !frame ;
			}
		}
		return this.selected;
	}

	public void set(Solvis solvis, boolean value) throws IOException, TerminationException {
		if (this.isSelected() != value) {
			Coordinate coord = new Coordinate( //
					this.getOrigin().getX() + this.getWidth() / 2, //
					this.getOrigin().getY() + this.getHeight() / 2);
			solvis.send(coord, this.pushTime, this.releaseTime);
		}
	}
}