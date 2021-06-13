/************************************************************************
 * 
 * $Id$
 *
 * 
 ************************************************************************/

package de.sgollmer.solvismax.model.command;

import java.io.IOException;

import de.sgollmer.solvismax.error.PowerOnException;
import de.sgollmer.solvismax.model.Solvis;
import de.sgollmer.solvismax.model.objects.ResultStatus;

public class CommandScreenRestore extends Command {

	private final boolean enable;
	private final Object service;

	public CommandScreenRestore(final boolean enable, final Object service) {
		this.enable = enable;
		this.service = service;
	}

	@Override
	public ResultStatus execute(final Solvis solvis, final Handling.QueueStatus queueStatus)
			throws IOException, PowerOnException {
		solvis.screenRestore(this.enable, this.service);
		return ResultStatus.SUCCESS;
	}

	@Override
	public String toString() {
		return "Screen restore is switched " + (this.enable ? "on." : "off.");

	}

	@Override
	protected void notExecuted() {
	}

}
