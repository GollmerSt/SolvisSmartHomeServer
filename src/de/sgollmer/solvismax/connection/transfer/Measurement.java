/************************************************************************
 * 
 * $Id$
 *
 * 
 ************************************************************************/

package de.sgollmer.solvismax.connection.transfer;

import de.sgollmer.solvismax.model.objects.data.SolvisData;

public class Measurement extends Element {
	
	public Measurement( SolvisData data ) {
		this.name = data.getId() ;
		this.value = data.toSingleValue() ;
	}
}
