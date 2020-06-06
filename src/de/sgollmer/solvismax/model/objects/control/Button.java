package de.sgollmer.solvismax.model.objects.control;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

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

	private static class Change {
		private final int y;
		private final int brightness;

		public Change(int y, int brightness) {
			this.y = y;
			this.brightness = brightness;
		}
		
		public String toString() {
			
		}
	}

	public boolean isSelected() {
		if (this.selected == null) {

			int x = this.getWidth() / 2;

			List<Change> changes = new ArrayList();

			int former = ImageHelper.getBrightness(this.getRGB(0, 0));

			for (int y = 0; y < this.getHeight(); ++y) {
				int brightness = ImageHelper.getBrightness(this.getRGB(x, y));
				if (brightness != former) {
					changes.add(new Change(y, brightness));
					former = brightness;
				}
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
