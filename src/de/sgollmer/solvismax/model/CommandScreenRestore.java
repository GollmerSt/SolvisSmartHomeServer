/************************************************************************
 * 
 * $Id$
 *
 * 
 ************************************************************************/

package de.sgollmer.solvismax.model;

import java.io.IOException;

import de.sgollmer.solvismax.error.ErrorPowerOn;
import de.sgollmer.solvismax.model.objects.screen.Screen;

public class CommandScreenRestore implements CommandI {

	private final boolean enable;

	public CommandScreenRestore(boolean enable) {
		this.enable = enable;
	}

	@Override
	public boolean execute(Solvis solvis) throws IOException, ErrorPowerOn {
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
		return enable;
	}

	@Override
	public Handling getHandling(CommandI queueEntry) {
		return new Handling(false, false);
	}

	@Override
	public String toString() {
		return "Screen restore is switched " + (this.enable ? "on." : "off.");

	}

	@Override
	public boolean first() {
		return false;
	}
	
	@Override
	public boolean equals(Object obj) {
		if (!(obj instanceof CommandScreenRestore)) {
			return false;
		} else {
			CommandScreenRestore cmp = (CommandScreenRestore) obj;
			return this.enable == cmp.enable ;
		}
	}

	@Override
	public int hashCode() {
		return super.hashCode();
	}
}
