/************************************************************************
 * 
 * $Id$
 *
 * 
 ************************************************************************/

package de.sgollmer.solvismax.connection.transfer;

import de.sgollmer.solvismax.error.JsonError;

public class Helper {

	public static char charAt(String json, int position) throws JsonError {
		try {
			return json.charAt(position);
		} catch (IndexOutOfBoundsException e) {
			throw new JsonError("Unespected end of json string reached");
		}
	}

}
