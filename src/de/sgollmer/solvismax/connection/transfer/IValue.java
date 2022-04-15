/************************************************************************
 * 
 * $Id$
 *
 * 
 ************************************************************************/

package de.sgollmer.solvismax.connection.transfer;

import de.sgollmer.solvismax.error.JsonException;
import de.sgollmer.solvismax.error.PackageException;
import de.sgollmer.solvismax.model.objects.data.SingleData;

public interface IValue {
	public void addTo(final StringBuilder builder);

	public int from(final String json, final int position, final long timeStamp) throws JsonException;

	public SingleData<?> getSingleData() throws PackageException;

	public Frame getFrame();
}
