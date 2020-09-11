/************************************************************************
 * 
 * $Id$
 *
 * 
 ************************************************************************/

package de.sgollmer.solvismax.model.objects.configuration;

import java.util.ArrayList;
import java.util.Collection;

import de.sgollmer.solvismax.error.AssignmentException;
import de.sgollmer.solvismax.error.ReferenceException;
import de.sgollmer.solvismax.error.XmlException;
import de.sgollmer.solvismax.model.Solvis;
import de.sgollmer.solvismax.model.objects.IAssigner;
import de.sgollmer.solvismax.model.objects.SolvisDescription;

public class OfConfigs<E extends OfConfigs.IElement<E>> implements IAssigner {
	private final Collection<E> elements = new ArrayList<>(1);

	public void verifyAndAdd(E element) throws XmlException {
		for (E e : this.elements) {
			if (!element.isConfigurationVerified(e)) {
				element.isConfigurationVerified(e);
				throw new XmlException("Configuration mask of screen <" + element.getName() + "> isn't unique.");
			}
		}
		this.elements.add(element);
	}

	public E get(Solvis solvis) {
		if (solvis.getConfigurationMask() == 0 && this.elements.size() == 1) {
			return this.elements.iterator().next();
		}
		for (E e : this.elements) {
			if (e.isInConfiguration(solvis)) {
				return e;
			}
		}
		return null;
	}

	public static Object get(Solvis solvis, OfConfigs<?> ofConfigs) {
		if (ofConfigs == null) {
			return null;
		} else {
			return ofConfigs.get(solvis);
		}
	}

	@Override
	public void assign(SolvisDescription description) throws XmlException, AssignmentException, ReferenceException {
		for (E e : this.elements) {
			e.assign(description);
		}
	}

	public Collection<E> getElements() {
		return this.elements;
	}

	public E getIfSingle() {
		if (this.elements.size() == 1) {
			return this.elements.iterator().next();
		} else {
			return null;
		}
	}

	public interface IElement<E> extends IAssigner {
		public boolean isConfigurationVerified(E e);

		public boolean isInConfiguration(Solvis solvis);

		public String getName();
	}

}