/************************************************************************
 * 
 * $Id$
 *
 * 
 ************************************************************************/

package de.sgollmer.solvismax.model.objects;

import de.sgollmer.solvismax.error.TypeException;
import de.sgollmer.solvismax.model.objects.data.SingleData;

public abstract class ChannelSource implements IChannelSource {

	protected ChannelDescription description;

	/**
	 * @return the description
	 */
	public ChannelDescription getDescription() {
		return this.description;
	}

	/**
	 * @param description the description to set
	 */
	void setDescription(ChannelDescription description) {
		this.description = description;
	}

	@Override
	public boolean isScreenChangeDependend() {
		return false;
	}

	protected abstract SingleData<?> createSingleData(String value) throws TypeException;
}
