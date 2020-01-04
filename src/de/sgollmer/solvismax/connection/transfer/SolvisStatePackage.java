/************************************************************************
 * 
 * $Id$
 *
 * 
 ************************************************************************/

package de.sgollmer.solvismax.connection.transfer;

import de.sgollmer.solvismax.model.SolvisState;

public class SolvisStatePackage extends JsonPackage {

	public SolvisStatePackage(SolvisState.State state) {
		this.command = Command.SOLVIS_STATE;
		this.data = new Frame();
		Element element = new Element();
		this.data.add(element);
		element.name = "SolvisState";
		element.value = new SingleValue(state.name());
	}
}
