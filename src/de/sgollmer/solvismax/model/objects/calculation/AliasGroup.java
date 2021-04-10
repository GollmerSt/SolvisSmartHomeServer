/************************************************************************
 * 
 * $Id$
 *
 * 
 ************************************************************************/

package de.sgollmer.solvismax.model.objects.calculation;

import java.util.ArrayList;
import java.util.Collection;

import de.sgollmer.solvismax.error.AliasException;
import de.sgollmer.solvismax.model.Solvis;
import de.sgollmer.solvismax.model.objects.Alias;
import de.sgollmer.solvismax.model.objects.AllSolvisData;
import de.sgollmer.solvismax.model.objects.ChannelDescription;
import de.sgollmer.solvismax.model.objects.IAssigner;
import de.sgollmer.solvismax.model.objects.SolvisDescription;
import de.sgollmer.solvismax.model.objects.data.SolvisData;

public class AliasGroup implements IAssigner, Cloneable {
	private final Collection<Alias> aliases = new ArrayList<>(2);

	public void add(Alias alias) {
		this.aliases.add(alias);
	}

	public Alias get(String id) {
		for (Alias alias : this.aliases) {
			if (alias.getId().equals(id)) {
				return alias;
			}
		}
		return null;
	}

	public SolvisData get(AllSolvisData allData, String id) throws AliasException {
		Alias alias = this.get(id);
		if (alias == null) {
			throw new AliasException("Alias error: <" + id + "> unknown");
		}

		ChannelDescription description = allData.getSolvis().getChannelDescription(alias.getDataId());
		if (description == null) {
			throw new AliasException("Alias error: <" + alias.getDataId() + "> unknown");
		}
		return allData.get(description);
	}
	
	public Collection<ChannelDescription> getChannelDescriptions( Solvis solvis) {
		Collection<ChannelDescription> descriptions = new ArrayList<>() ;
		for ( Alias alias : this.aliases) {
			ChannelDescription description = solvis.getChannelDescription(alias.getDataId()) ;
			if ( description != null ) {
				descriptions.add(description);
			}
		}
		return descriptions;
	}

	@Override
	public void assign(SolvisDescription description) {
		for (Alias alias : this.aliases) {
			alias.assign(description);
		}
	}
	
	@Override
	public AliasGroup clone() {
		try {
			return (AliasGroup) super.clone();
		} catch (CloneNotSupportedException e) {
		}
		return null;
	}

	public boolean isDelayed(Solvis solvis) {
		boolean delayed = false;
		for (ChannelDescription description : this.getChannelDescriptions(solvis)) {
			delayed |= description.isDelayed(solvis);
		}
		return delayed;
	}

	public Integer getScanInterval_ms(Solvis solvis) {
		Integer scanInterval = null;
		for (ChannelDescription description : this.getChannelDescriptions(solvis)) {
			Integer si = description.getScanInterval_ms(solvis);
			if ( si != null && ( scanInterval == null || scanInterval < si )) {
				scanInterval = si;
			}
		}
		return scanInterval;
	}

}
