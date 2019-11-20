package de.sgollmer.solvismax.model;

import de.sgollmer.solvismax.model.objects.DataDescription;
import de.sgollmer.solvismax.model.objects.data.SingleData;

public class Command {
	private final DataDescription description ;
	private final SingleData setValue ;
	private final boolean screenRestoreOn ;
	private final boolean screenRestoreOff ;
	
	public Command( DataDescription description ) {
		this( description, null, false, false ) ;
	}
	
	public Command( DataDescription description, SingleData setValue ) {
		this( description, setValue, false, false ) ;
	}
	
	public Command( DataDescription description, boolean screenRestoreOff, boolean screenRestoreOn ) {
		this( description, null, screenRestoreOff, screenRestoreOn ) ;
	}
	
	public Command( DataDescription description, SingleData setValue, boolean screenRestoreOff, boolean screenRestoreOn ) {
		this.description = description ;
		this.setValue = setValue ;
		this.screenRestoreOff = screenRestoreOff ;
		this.screenRestoreOn = screenRestoreOn ;
	}

	public SingleData getSetValue() {
		return setValue;
	}

	public DataDescription getDescription() {
		return description;
	}

	public boolean isScreenRestoreOn() {
		return screenRestoreOn;
	}

	public boolean isScreenRestoreOff() {
		return screenRestoreOff;
	}

}
