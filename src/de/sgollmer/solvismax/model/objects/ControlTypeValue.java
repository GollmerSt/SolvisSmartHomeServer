package de.sgollmer.solvismax.model.objects;

import de.sgollmer.solvismax.model.Solvis;

public class ControlTypeValue extends ControlTypeRead{
	private final int increment ;
	private final int least ;
	private final int most ;
	private final boolean warpAround ;
	private final TouchPoint upper ;
	private final TouchPoint lower ;
	
	public ControlTypeValue(int increment, String format, int least, int most, boolean warpAround, TouchPoint upper,  TouchPoint lower ) {
		super( format ) ;
		this.increment = increment ;
		this.least = least ;
		this.most = most ;
		this.warpAround = warpAround ;
		this.upper = upper ;
		this.lower = lower ;
	}

	@Override
	public void setValue( Solvis solvis, String value) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public boolean isWriteable() {
		return true ;
	}

}
