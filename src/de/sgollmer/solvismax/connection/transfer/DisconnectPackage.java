/************************************************************************
 * 
 * $Id$
 *
 * 
 ************************************************************************/

package de.sgollmer.solvismax.connection.transfer;

import de.sgollmer.solvismax.connection.IReceivedData;

public class DisconnectPackage extends JsonPackage implements IReceivedData {

	DisconnectPackage() {
		this.command = Command.DISCONNECT;
		this.data = null;
	}
}
