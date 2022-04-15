package de.sgollmer.solvismax.model.objects.control;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.ListIterator;

import de.sgollmer.solvismax.model.Solvis;

public class DependencyGroup implements Cloneable {
	private ArrayList<Dependency> dependencies = new ArrayList<>(3);

	public void add(final Dependency dependency) {
		Integer dPriority = dependency.getPriority();
		boolean added = false;
		for (ListIterator<Dependency> it = this.dependencies.listIterator(); it.hasNext() && !added;) {
			Dependency cmp = it.next();
			boolean strGth = dependency.getId().compareTo(cmp.getId()) > 0;
			Integer priority = cmp.getPriority();
			if (false //
					|| dPriority != null && priority == null //
					|| dPriority != null && dPriority < priority //
					|| dPriority != null && (dPriority.equals(priority) && strGth)//
					|| dPriority == null && priority == null && strGth) {
				it.previous();
				it.add(dependency);
				added = true;
			}
		}
		if (!added) {
			this.dependencies.add(dependency);
		}
	}

	public Collection<Dependency> get() {
		return this.dependencies;
	}

	@Override
	public boolean equals(final Object o) {
		if (!(o instanceof DependencyGroup)) {
			return false;
		} else {
			return this.dependencies.equals(((DependencyGroup) o).dependencies);
		}
	}

	@Override
	public int hashCode() {
		return this.dependencies.hashCode();
	}

	@Override
	public DependencyGroup clone() {
		DependencyGroup group = new DependencyGroup();
		@SuppressWarnings("unchecked")
		ArrayList<Dependency> clone = (ArrayList<Dependency>) this.dependencies.clone();
		group.dependencies = clone;
		return group;
	}

	public static boolean equals(final DependencyGroup g1, final DependencyGroup g2, final Solvis solvis) {
		@SuppressWarnings("unchecked")
		Collection<Dependency> gc1 = (Collection<Dependency>) g1.dependencies.clone();
		@SuppressWarnings("unchecked")
		Collection<Dependency> gc2 = (Collection<Dependency>) g2.dependencies.clone();

		for (Iterator<Dependency> it1 = gc1.iterator(); it1.hasNext();) {
			Dependency d1 = it1.next();
			for (Iterator<Dependency> it2 = gc2.iterator(); it2.hasNext();) {
				Dependency d2 = it2.next();
				if (Dependency.equals(d1, d2, solvis)) {
					it1.remove();
					it2.remove();
					break;
				}
			}
		}

		return gc1.isEmpty() && gc2.isEmpty();
	}

}
