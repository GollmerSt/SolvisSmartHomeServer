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
import de.sgollmer.solvismax.model.Solvis;
import de.sgollmer.solvismax.model.command.Command;
import de.sgollmer.solvismax.model.command.Handling;
import de.sgollmer.solvismax.model.objects.ResultStatus;
import de.sgollmer.solvismax.model.objects.clock.ClockMonitor.NextAdjust;

public class CommandClock extends Command {

	private final ClockMonitor.IAdjustStrategy strategy;
	private final NextAdjust nextAdjust;

	CommandClock(final ClockMonitor.IAdjustStrategy strategy, final NextAdjust nextAdjust) {
		this.strategy = strategy;
		this.nextAdjust = nextAdjust;
	}

	@Override
	public ResultStatus execute(final Solvis solvis, final Handling.QueueStatus queueStatus)
			throws IOException, TerminationException, PowerOnException {
		this.strategy.execute(this.nextAdjust);
		return ResultStatus.SUCCESS;
	}

	@Override
	public void notExecuted() {
		this.strategy.notExecuted();
	}

	@Override
	public Handling handle(final Command queueEntry, final Solvis solvis) {
		return new Handling( //
				false, // inQueueInhibited
				queueEntry instanceof CommandClock, // inhibitAppend
				false // insert
		);
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
	
	@Override
	public Type getType() {
		return Type.CONTROL_WRITE;
	}



}
