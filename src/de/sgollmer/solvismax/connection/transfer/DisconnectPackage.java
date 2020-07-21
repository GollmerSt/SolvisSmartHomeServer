/************************************************************************
 * 
 * $Id$
 *
 * 
 ************************************************************************/

package de.sgollmer.solvismax.connection.transfer;

import de.sgollmer.solvismax.connection.ITransferedData;

public class DisconnectPackage extends JsonPackage implements ITransferedData {

	DisconnectPackage() {
		this.command = Command.DISCONNECT;
		this.data = null;
	}
}
