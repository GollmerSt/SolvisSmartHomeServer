/************************************************************************
 * 
 * $Id$
 *
 * 
 ************************************************************************/

package de.sgollmer.solvismax.model.objects;

import java.util.ArrayList;
import java.util.Collection;

import de.sgollmer.solvismax.error.XmlError;

public class OfConfigs<E extends OfConfigs.Element<E>> {
	private final Collection<E> elements = new ArrayList<>(1);

	public void verifyAndAdd(E element) throws XmlError {
		for (E e : this.elements) {
			if (!element.isConfigurationVerified(e)) {
				element.isConfigurationVerified(e) ;
				throw new XmlError("Configuration mask of screen <" + element.getId() + "> isn't unique.");
			}
		}
		this.elements.add(element);
	}

	public E get(int configurationMask) {
		if ( configurationMask == 0 && this.elements.size() == 1 ) {
			return this.elements.iterator().next() ;
		}
		for (E e : this.elements) {
			if (e.isInConfiguration(configurationMask)) {
				return e;
			}
		}
		return null;
	}

	public void assign(SolvisDescription description) {
		for (E e : this.elements) {
			e.assign(description);
		}
	}

	public Collection<E> getElements() {
		return elements;
	}
	
	public E getIfSingle() {
		if ( this.elements.size() == 1 ) {
			return this.elements.iterator().next() ;
		} else {
			return null ;
		}
	}
	
	public interface Element<E> {
		public boolean isConfigurationVerified(E e) ;
		public boolean isInConfiguration(int configurationMask) ;
		public String getId() ;
		public void assign(SolvisDescription description);
	}
	
	public int size() {
		return elements.size() ;
	}
}