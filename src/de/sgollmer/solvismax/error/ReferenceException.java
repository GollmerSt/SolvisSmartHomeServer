/************************************************************************
 * 
 * $Id$
 *
 * 
 ************************************************************************/

package de.sgollmer.solvismax.error;

import de.sgollmer.xmllibrary.XmlException;

public class ReferenceException extends XmlException {
	/**
	 * 
	 */
	private static final long serialVersionUID = 5082138054873699405L;

	public ReferenceException(final String message) {
		super(message);
	}
}
