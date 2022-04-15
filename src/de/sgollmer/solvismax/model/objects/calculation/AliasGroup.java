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
import de.sgollmer.solvismax.model.objects.data.SolvisData;

public class AliasGroup implements Cloneable {
	private final Collection<Alias> aliases = new ArrayList<>(2);

	public void add(final Alias alias) {
		this.aliases.add(alias);
	}

	public Alias get(final String id) {
		for (Alias alias : this.aliases) {
			if (alias.getId().equals(id)) {
				return alias;
			}
		}
		return null;
	}

	public SolvisData get(final AllSolvisData allData, final String id) throws AliasException {
		return this.get(allData, id, false);
	}

	public SolvisData get(final AllSolvisData allData, final String id, final boolean optional) throws AliasException {
		Alias alias = this.get(id);
		if (alias == null) {
			throw new AliasException("Alias error: <" + id + "> unknown");
		}

		SolvisData data = allData.get(alias.getDataId());

		if (data == null && !optional) {
			throw new AliasException("Alias error: <" + alias.getDataId() + "> unknown");
		}
		return data;
	}

	public Collection<ChannelDescription> getChannelDescriptions(final Solvis solvis) {
		Collection<ChannelDescription> descriptions = new ArrayList<>();
		for (Alias alias : this.aliases) {
			ChannelDescription description = solvis.getChannelDescription(alias.getDataId());
			if (description != null) {
				descriptions.add(description);
			}
		}
		return descriptions;
	}

	@Override
	public AliasGroup clone() {
		try {
			return (AliasGroup) super.clone();
		} catch (CloneNotSupportedException e) {
		}
		return null;
	}

	public Integer getScanInterval_ms(final Solvis solvis) {
		Integer scanInterval = null;
		for (ChannelDescription description : this.getChannelDescriptions(solvis)) {
			Integer si = description.getScanInterval_ms(solvis);
			if (si != null && (scanInterval == null || scanInterval < si)) {
				scanInterval = si;
			}
		}
		return scanInterval;
	}

}
