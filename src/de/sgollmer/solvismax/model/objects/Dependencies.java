/************************************************************************
 * 
 * $Id$
 *
 * 
 ************************************************************************/

package de.sgollmer.solvismax.model.objects;

import java.util.ArrayList;
import java.util.Collection;

import de.sgollmer.solvismax.error.AliasException;
import de.sgollmer.solvismax.model.objects.data.SolvisData;

public class Dependencies implements IAssigner {
	private final Collection<Alias> collection = new ArrayList<>(2);

	public void add(Alias dependency) {
		this.collection.add(dependency);
	}

	public Alias get(String id) {
		for (Alias d : this.collection) {
			if (d.getId().equals(id)) {
				return d;
			}
		}
		return null;
	}

	public SolvisData get(AllSolvisData allData, String id) throws AliasException {
		Alias dependency = this.get(id);
		if (dependency == null) {
			throw new AliasException("Alias error: <" + id + "> unknown");
		}

		ChannelDescription description = allData.getSolvis().getChannelDescription(dependency.getDataId());
		if (description == null) {
			throw new AliasException("Alias error: <" + dependency.getDataId() + "> unknown");
		}
		return allData.get(description);
	}

	@Override
	public void assign(SolvisDescription description) {
		for (Alias d : this.collection) {
			d.assign(description);
		}
	}

}
