package de.sgollmer.solvismax.model;

import de.sgollmer.solvismax.imagepatternrecognition.image.MyImage;
import de.sgollmer.solvismax.model.objects.ScreenSaver;
import de.sgollmer.solvismax.objects.Coordinate;

public class WatchDog {

	private final Solvis solvis;
	private final ScreenSaver saver = new ScreenSaver(new Coordinate(75, 0), new Coordinate(75, 20),
			new Coordinate(75, 21), new Coordinate(75, 33));

	private MyImage formerImage = null;
	private long updateTimeAfterNoChange = -1;
	private long changedTime = -1;

	public WatchDog(Solvis solvis) {
		this.solvis = solvis;
	}

	public void execute() {
		long time = System.currentTimeMillis();

		MyImage solvisImage = this.solvis.getRealImage();

		boolean changed = false;

		if (this.saver.is(solvisImage)) {
			this.solvis.setScreenSaverActive(true);
		} else {

			if (this.formerImage == null) {
				this.formerImage = this.solvis.getCurrentImage();
			}

			if (!solvisImage.equals(this.formerImage)) {

				this.formerImage = solvisImage;
				this.solvis.clearCurrentImage();
				this.changedTime = time;
				changed = true;
			}
		}
		if (!changed) {
			if (changedTime >= 0 && time > changedTime + this.getUpdateTimeAfterNoChange()) {
				solvis.notifyScreenChangedByUserObserver(this.solvis.getCurrentScreen());
			}
		}
	}

	/**
	 * @return the updateTimeAfterNoChange
	 */
	private long getUpdateTimeAfterNoChange() {
		if (this.updateTimeAfterNoChange < 0) {
			this.updateTimeAfterNoChange = this.solvis.getDuration("updateTimeAfterNoChange").getTime_ms();
		}
		return this.updateTimeAfterNoChange;
	}

	public void clear() {
		this.formerImage = null;
		this.changedTime = -1;
	}
}
