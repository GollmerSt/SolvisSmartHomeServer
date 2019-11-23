package de.sgollmer.solvismax.error;

import javax.xml.stream.XMLStreamException;

public class XmlError extends Error {

	/**
	 * 
	 */
	private static final long serialVersionUID = -6034730869461501021L;
	
	public XmlError( String message ) {
		super(message) ;
	}
	
	public XmlError( XMLStreamException e, String message ) {
		super(message, e) ;
	}

}
