package de.sgollmer.solvismax.model.objects;

import java.io.IOException;

import de.sgollmer.solvismax.error.TerminationException;
import de.sgollmer.solvismax.model.Solvis;
import de.sgollmer.solvismax.model.objects.screen.AbstractScreen;
import de.sgollmer.solvismax.model.objects.screen.ISelectScreenStrategy;

public class TouchPointStrategy implements ISelectScreenStrategy {

	private final TouchPoint touchPoint;

	public TouchPointStrategy(final TouchPoint touchPoint) {
		this.touchPoint = touchPoint;
	}

	@Override
	public boolean execute(final Solvis solvis, final AbstractScreen startingScreen)
			throws IOException, TerminationException {
		return this.touchPoint.execute(solvis, startingScreen);
	}

	@Override
	public int getSettingTime(final Solvis solvis) {
		return this.touchPoint.getSettingTime(solvis);
	}

	public TouchPoint getTouchPoint() {
		return this.touchPoint;
	}

}
