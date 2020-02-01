/************************************************************************
 * 
 * $Id$
 *
 * 
 ************************************************************************/

package de.sgollmer.solvismax.model;

import java.io.IOException;

import de.sgollmer.solvismax.error.ErrorPowerOn;

public class CommandScreenRestore extends Command {

	private final boolean enable;

	public CommandScreenRestore(boolean enable) {
		this.enable = enable;
	}

	@Override
	public boolean execute(Solvis solvis) throws IOException, ErrorPowerOn {
		solvis.screenRestore(this.enable);
		return true;
	}

	@Override
	public String toString() {
		return "Screen restore is switched " + (this.enable ? "on." : "off.");

	}
}
