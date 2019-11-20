package de.sgollmer.solvismax.model.objects;

import java.util.ArrayList;
import java.util.Collection;

import de.sgollmer.solvismax.error.DependencyError;
import de.sgollmer.solvismax.model.objects.data.SolvisData;

public class Dependencies {
	private final Collection< Dependency> collection = new ArrayList<>() ;
	
	public void add( Dependency dependency ) {
		this.collection.add(dependency) ;
	}
	
	public Dependency get( String id ) {
		for ( Dependency d : this.collection ) {
			if ( d.getId().equals(id)) {
				return d ;
			}
		}
		return null ;
	}

	public SolvisData get( AllSolvisData allData, String id ) {
		Dependency dependency = this.get( id) ;
		if (dependency == null) {
			throw new DependencyError("Dependency error: <" + id + "> unknown");
		}

		DataDescription description = allData.getSolvis().getDataDescription(dependency.getDependencyId()) ;
		if ( description == null ) {
			throw new DependencyError("Dependency error: <" + dependency.getDependencyId() + "> unknown");
		}
		return allData.get(description) ;
	}
	
}
