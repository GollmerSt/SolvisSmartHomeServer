/************************************************************************
 * 
 * $Id$
 *
 * 
 ************************************************************************/

package de.sgollmer.solvismax.model;

import java.io.IOException;

import de.sgollmer.solvismax.error.PowerOnException;
import de.sgollmer.solvismax.model.objects.ResultStatus;

public class CommandScreenRestore extends Command {

	private final boolean enable;

	public CommandScreenRestore(boolean enable) {
		this.enable = enable;
	}

	@Override
	protected ResultStatus execute(Solvis solvis) throws IOException, PowerOnException {
		solvis.screenRestore(this.enable);
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
