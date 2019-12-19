package de.sgollmer.solvismax.model;

import de.sgollmer.solvismax.model.objects.ChannelDescription;
import de.sgollmer.solvismax.model.objects.data.SingleData;

public class Command {
	private final ChannelDescription description ;
	private final SingleData<?> setValue ;
	private final boolean screenRestoreOn ;
	private final boolean screenRestoreOff ;
	private boolean inhibit = false ;
	
	public Command( ChannelDescription description ) {
		this( description, null, false, false ) ;
	}
	
	public Command( ChannelDescription description, SingleData<?> setValue ) {
		this( description, setValue, false, false ) ;
	}
	
	public Command( boolean screenRestore ) {
		this( null, null, !screenRestore, screenRestore ) ;
	}
	
	public Command( ChannelDescription description, SingleData<?> setValue, boolean screenRestoreOff, boolean screenRestoreOn ) {
		this.description = description ;
		this.setValue = setValue ;
		this.screenRestoreOff = screenRestoreOff ;
		this.screenRestoreOn = screenRestoreOn ;
	}

	public SingleData<?> getSetValue() {
		return setValue;
	}

	public ChannelDescription getDescription() {
		return description;
	}

	public boolean isScreenRestoreOn() {
		return screenRestoreOn;
	}

	public boolean isScreenRestoreOff() {
		return screenRestoreOff;
	}

	public boolean isInhibit() {
		return inhibit;
	}

	public void setInhibit(boolean inhibit) {
		this.inhibit = inhibit;
	}

}
