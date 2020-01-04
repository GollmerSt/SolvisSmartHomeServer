/************************************************************************
 * 
 * $Id: 73_SolvisClient.pm 78 2020-01-03 17:50:08Z stefa $
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
		return description;
	}

	/**
	 * @param description
	 *            the description to set
	 */
	public void setDescription(ChannelDescription description) {
		this.description = description;
	}
}
