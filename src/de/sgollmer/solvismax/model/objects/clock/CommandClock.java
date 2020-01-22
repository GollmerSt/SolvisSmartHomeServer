/************************************************************************
 * 
 * $Id$
 *
 * 
 ************************************************************************/

package de.sgollmer.solvismax.model.objects.clock;

import java.io.IOException;

import de.sgollmer.solvismax.error.ErrorPowerOn;
import de.sgollmer.solvismax.error.TerminationException;
import de.sgollmer.solvismax.model.CommandI;
import de.sgollmer.solvismax.model.Solvis;
import de.sgollmer.solvismax.model.objects.clock.ClockMonitor.NextAdjust;
import de.sgollmer.solvismax.model.objects.screen.Screen;

public class CommandClock implements CommandI {

	private final ClockMonitor.AdjustStrategy strategy;
	private final NextAdjust nextAdjust;

	public CommandClock(ClockMonitor.AdjustStrategy strategy, NextAdjust nextAdjust) {
		this.strategy = strategy;
		this.nextAdjust = nextAdjust;
	}

	@Override
	public boolean execute(Solvis solvis) throws IOException, TerminationException, ErrorPowerOn {
		this.strategy.execute(this.nextAdjust);
		return true;
	}

	@Override
	public Screen getScreen(Solvis solvis) {
		return null;
	}

	@Override
	public boolean isInhibit() {
		return false;
	}

	@Override
	public void setInhibit(boolean inhibit) {
	}

	@Override
	public Boolean isScreenRestore() {
		return null;
	}

	@Override
	public Handling getHandling(CommandI queueEntry) {
		return new Handling(false, queueEntry instanceof CommandClock);
	}

	@Override
	public boolean first() {
		return true;
	}

	@Override
	public boolean equals(Object obj) {
		if (!(obj instanceof CommandClock)) {
			return false;
		} else {
			CommandClock cmp = (CommandClock) obj;
			return this.strategy == cmp.strategy && this.nextAdjust == cmp.nextAdjust;
		}
	}

	@Override
	public int hashCode() {
		return super.hashCode();
	}

	public NextAdjust getNextAdjust() {
		return nextAdjust;
	}

}
