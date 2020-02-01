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
import de.sgollmer.solvismax.model.Command;
import de.sgollmer.solvismax.model.Solvis;
import de.sgollmer.solvismax.model.objects.clock.ClockMonitor.NextAdjust;

public class CommandClock extends Command {

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
	public Handling getHandling(Command queueEntry, Solvis solvis) {
		return new Handling(false, queueEntry instanceof CommandClock, false);
	}

	@Override
	public boolean first() {
		return true;
	}

	public NextAdjust getNextAdjust() {
		return nextAdjust;
	}

	public ClockMonitor.AdjustStrategy getStrategy() {
		return strategy;
	}

	
}
