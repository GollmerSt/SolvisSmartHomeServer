/************************************************************************
 * 
 * $Id$
 *
 * 
 ************************************************************************/

package de.sgollmer.solvismax.error;

import javax.xml.stream.XMLStreamException;

public class XmlException extends Exception {

	/**
	 * 
	 */
	private static final long serialVersionUID = -6034730869461501021L;

	public XmlException(String message) {
		super(message);
	}

	public XmlException(XMLStreamException e, String message) {
		super(message, e);
	}

}
