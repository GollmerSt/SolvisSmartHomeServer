/************************************************************************
 * 
 * $Id$
 *
 * 
 ************************************************************************/

package de.sgollmer.solvismax.model.objects;

import de.sgollmer.solvismax.error.AssignmentException;
import de.sgollmer.solvismax.error.ReferenceException;
import de.sgollmer.solvismax.error.XmlException;

public interface IAssigner {
	public void assign(SolvisDescription description) throws XmlException, AssignmentException, ReferenceException;
}
