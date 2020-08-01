/************************************************************************
 * 
 * $Id$
 *
 * 
 ************************************************************************/

package de.sgollmer.solvismax.model.objects.clock;

import java.io.IOException;

import de.sgollmer.solvismax.error.PowerOnException;
import de.sgollmer.solvismax.error.TerminationException;
import de.sgollmer.solvismax.model.Command;
import de.sgollmer.solvismax.model.Solvis;
import de.sgollmer.solvismax.model.objects.clock.ClockMonitor.NextAdjust;

public class CommandClock extends Command {

	private final ClockMonitor.IAdjustStrategy strategy;
	private final NextAdjust nextAdjust;

	CommandClock(ClockMonitor.IAdjustStrategy strategy, NextAdjust nextAdjust) {
		this.strategy = strategy;
		this.nextAdjust = nextAdjust;
	}

	@Override
	public boolean execute(Solvis solvis) throws IOException, TerminationException, PowerOnException {
		this.strategy.execute(this.nextAdjust);
		return true;
	}

	@Override
	public void notExecuted() {
		this.strategy.notExecuted();
	}

	@Override
	public Handling getHandling(Command queueEntry, Solvis solvis) {
		return new Handling(false, queueEntry instanceof CommandClock, false);
	}

	@Override
	public boolean first() {
		return true;
	}

	NextAdjust getNextAdjust() {
		return this.nextAdjust;
	}

	@Override
	public String toString() {
		return "Setting of Clock";
	}

}
