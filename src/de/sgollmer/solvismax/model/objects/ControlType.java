package de.sgollmer.solvismax.model.objects;

import de.sgollmer.solvismax.model.Solvis;

public interface ControlType {
	public String getValue( Solvis solvis ) ;
	public void setValue( Solvis solvis, String value ) ;
	public boolean isWriteable() ;
}
