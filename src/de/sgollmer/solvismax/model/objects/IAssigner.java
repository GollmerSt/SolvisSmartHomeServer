/************************************************************************
 * 
 * $Id$
 *
 * 
 ************************************************************************/

package de.sgollmer.solvismax.model.objects;

import de.sgollmer.solvismax.error.AssignmentException;
import de.sgollmer.solvismax.error.ReferenceException;
import de.sgollmer.xmllibrary.XmlException;

public interface IAssigner {
	public void assign(final SolvisDescription description)
			throws XmlException, AssignmentException, ReferenceException;
}
