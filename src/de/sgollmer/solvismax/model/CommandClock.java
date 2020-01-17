/************************************************************************
 * 
 * $Id$
 *
 * 
 ************************************************************************/

package de.sgollmer.solvismax.model;

import java.io.IOException;

import de.sgollmer.solvismax.error.ErrorPowerOn;
import de.sgollmer.solvismax.model.objects.Clock;
import de.sgollmer.solvismax.model.objects.Screen;
import de.sgollmer.solvismax.model.objects.Clock.NextAdjust;

public class CommandClock implements CommandI {
	
	private final Clock.Executable executable ;
	private final NextAdjust nextAdjust ;
	
	public CommandClock( Clock.Executable executable, NextAdjust nextAdjust ) {
		this.executable = executable ;
		this.nextAdjust = nextAdjust ;
	}
	
	@Override
	public boolean execute(Solvis solvis) throws IOException, ErrorPowerOn {
		this.executable.adjust(this.nextAdjust );
		return true ;
	}

	@Override
	public Screen getScreen(Solvis solvis) {
		return null;
	}

	@Override
	public boolean isInhibit() {
		return false ;
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
		return new Handling(false, queueEntry instanceof CommandClock );
	}

	@Override
	public boolean first() {
		return true;
	}
	
	@Override
	public boolean equals( Object obj) {
		if ( !(obj instanceof CommandClock )) {
			return false ;
		} else {
			CommandClock cmp = (CommandClock) obj ;
			return this.executable == cmp.executable && this.nextAdjust == cmp.nextAdjust ;
		}
	}

	@Override
	public int hashCode() {
		return super.hashCode();
	}
}
