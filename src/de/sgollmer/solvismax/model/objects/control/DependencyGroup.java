package de.sgollmer.solvismax.model.objects.control;

import java.util.Collection;
import java.util.HashSet;
import java.util.Iterator;

import de.sgollmer.solvismax.error.AssignmentException;
import de.sgollmer.solvismax.error.ReferenceException;
import de.sgollmer.solvismax.error.XmlException;
import de.sgollmer.solvismax.model.Solvis;
import de.sgollmer.solvismax.model.objects.IAssigner;
import de.sgollmer.solvismax.model.objects.SolvisDescription;

public class DependencyGroup implements IAssigner, Cloneable{
	private HashSet<Dependency > dependencies = new HashSet<>(3);
	
	public void add( Dependency dependency ) {
		this.dependencies.add(dependency);
	}
	
	public Collection< Dependency > get() {
		return this.dependencies;
	}
	
	@Override
	public boolean equals( Object o ) {
		if ( !(o instanceof DependencyGroup )) {
			return false;
		} else{
			return this.dependencies.equals(((DependencyGroup)o).dependencies);
		}
	}

	@Override
	public int hashCode() {
		return this.dependencies.hashCode();
	}

	@Override
	public void assign(SolvisDescription description) throws XmlException, AssignmentException, ReferenceException {
		for ( Dependency dependency: this.dependencies ) {
			dependency.assign(description);
		}
		
	}
	
	@Override
	public DependencyGroup clone() {
		DependencyGroup group = new DependencyGroup();
		@SuppressWarnings("unchecked")
		HashSet<Dependency> clone = (HashSet<Dependency>) this.dependencies.clone();
		group.dependencies = clone;
		return group;
	}
	
	public static boolean equals( DependencyGroup g1, DependencyGroup g2, Solvis solvis) {
		@SuppressWarnings("unchecked")
		Collection< Dependency> gc1 = (Collection<Dependency>) g1.dependencies.clone();
		@SuppressWarnings("unchecked")
		Collection< Dependency> gc2 = (Collection<Dependency>) g2.dependencies.clone();
		
		for ( Iterator<Dependency> it1 = gc1.iterator(); it1.hasNext();) {
			Dependency d1 = it1.next();
			for ( Iterator<Dependency> it2 = gc2.iterator(); it2.hasNext();) {
				Dependency d2 = it2.next();
				if ( Dependency.equals(d1, d2, solvis)) {
					it1.remove();
					it2.remove();
					break;
				}
			}			
		}
		
		return gc1.isEmpty() && gc2.isEmpty();
	}
	
}
