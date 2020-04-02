/************************************************************************
 * 
 * $Id$
 *
 * 
 ************************************************************************/

package de.sgollmer.solvismax.model.objects;

public abstract class ChannelSource implements ChannelSourceI {

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
	public void setDescription(ChannelDescription description) {
		this.description = description;
	}
}
