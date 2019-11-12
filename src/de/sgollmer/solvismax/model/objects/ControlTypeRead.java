package de.sgollmer.solvismax.model.objects;

import de.sgollmer.solvismax.model.Solvis;

public class ControlTypeRead implements ControlType {
	protected final String format ;
	
	public ControlTypeRead( String format ) {
		this.format = format ;
	}

	@Override
	public String getValue( Solvis solvis ) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public void setValue( Solvis solvis, String value) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public boolean isWriteable() {
		return false;
	}

}
