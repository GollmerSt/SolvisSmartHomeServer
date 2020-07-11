/************************************************************************
 * 
 * $Id$
 *
 * 
 ************************************************************************/

package de.sgollmer.solvismax.model.objects;

import java.util.ArrayList;
import java.util.Collection;

import de.sgollmer.solvismax.error.DependencyError;
import de.sgollmer.solvismax.model.objects.data.SolvisData;

public class Dependencies implements IAssigner {
	private final Collection<Dependency> collection = new ArrayList<>(2);

	public void add(Dependency dependency) {
		this.collection.add(dependency);
	}

	public Dependency get(String id) {
		for (Dependency d : this.collection) {
			if (d.getId().equals(id)) {
				return d;
			}
		}
		return null;
	}

	public SolvisData get(AllSolvisData allData, String id) {
		Dependency dependency = this.get(id);
		if (dependency == null) {
			throw new DependencyError("Dependency error: <" + id + "> unknown");
		}

		ChannelDescription description = allData.getSolvis().getChannelDescription(dependency.getDataId());
		if (description == null) {
			throw new DependencyError("Dependency error: <" + dependency.getDataId() + "> unknown");
		}
		return allData.get(description);
	}

	@Override
	public void assign(SolvisDescription description) {
		for (Dependency d : this.collection) {
			d.assign(description);
		}
	}

}
