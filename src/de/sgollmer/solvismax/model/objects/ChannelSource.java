/************************************************************************
 * 
 * $Id$
 *
 * 
 ************************************************************************/

package de.sgollmer.solvismax.model.objects;

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
	void setDescription(final ChannelDescription description) {
		this.description = description;
	}

	@Override
	public boolean isHumanAccessDependend() {
		return false;
	}

	public boolean isGlitchDetectionAllowed() {
		return true;
	}

	@Override
	public boolean isFast() {
		return false;
	}

}
