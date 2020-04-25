/************************************************************************
 * 
 * $Id$
 *
 * 
 ************************************************************************/

package de.sgollmer.solvismax.modbus;

public enum RegisterType {
	INPUT("Input"), HOLDING("Holding");

	private final String xmlName ;
	
	private RegisterType( String xmlName ) {
		this.xmlName = xmlName ;
	}
	
	public static RegisterType get( String xmlName) {
		for ( RegisterType type : RegisterType.values()) {
			if ( type.xmlName.equals(xmlName)) {
				return type;
			}
		}
		return null ;
	}
}
