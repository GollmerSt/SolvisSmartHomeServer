package de.sgollmer.solvismax.model.objects;

import java.util.ArrayList;
import java.util.Collection;

import de.sgollmer.solvismax.model.Solvis;

public class ControlTypeMode implements ControlType {
	
	private final Collection< Mode > modes = new ArrayList<>() ;

	@Override
	public String getValue( Solvis solvis ) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public void setValue( Solvis solvis, String value ) {
		// TODO Auto-generated method stub

	}

	@Override
	public boolean isWriteable() {
		return true;
	}

}
