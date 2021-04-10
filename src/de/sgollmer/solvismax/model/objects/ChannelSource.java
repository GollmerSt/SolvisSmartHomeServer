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
	public boolean isHumanAccessDependend() {
		return false;
	}

	protected abstract SingleData<?> createSingleData(String value, long timeStamp) throws TypeException;
	
	public boolean isGlitchDetectionAllowed() {
		return true ;
	}
	
	@Override
	public boolean isFast() {
		return false;
	}


//	@Override
//	public Integer getScanInterval_ms(Solvis solvis) {
//		return null;
//	}

}
