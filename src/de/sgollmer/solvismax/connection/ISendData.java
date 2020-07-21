/************************************************************************
 * 
 * $Id: Mqtt.java 277 2020-07-19 16:00:49Z stefa_000 $
 *
 * 
 ************************************************************************/

package de.sgollmer.solvismax.connection;

import de.sgollmer.solvismax.connection.transfer.JsonPackage;

public interface ISendData {
	public JsonPackage createJsonPackage();
}
