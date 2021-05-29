/************************************************************************
 * 
 * $Id$
 *
 * 
 ************************************************************************/

package de.sgollmer.solvismax.model.objects.configuration;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;

import de.sgollmer.solvismax.error.AssignmentException;
import de.sgollmer.solvismax.error.ReferenceException;
import de.sgollmer.solvismax.model.Solvis;
import de.sgollmer.solvismax.model.objects.IAssigner;
import de.sgollmer.solvismax.model.objects.SolvisDescription;
import de.sgollmer.xmllibrary.XmlException;

public class OfConfigs<E extends OfConfigs.IElement<E>> implements IAssigner, Iterable<E> {
	private final Collection<E> elements = new ArrayList<>(1);

	public void verifyAndAdd(final E element) throws XmlException {
		for (E e : this.elements) {
			if (!element.isConfigurationVerified(e)) {
				element.isConfigurationVerified(e);
				throw new XmlException("Configuration mask of " + element.getElementType() + " <" + element.getName()
						+ "> isn't unique.\n" + "Added mask: " + element.getConfiguration().toString() + "\n"
						+ "Not unique with: " + e.getConfiguration().toString());
			}
		}
		this.elements.add(element);
	}

	public E get(final Solvis solvis) {
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

	public static Object get(final Solvis solvis, final OfConfigs<?> ofConfigs) {
		if (ofConfigs == null) {
			return null;
		} else {
			return ofConfigs.get(solvis);
		}
	}

	@Override
	public void assign(final SolvisDescription description)
			throws XmlException, AssignmentException, ReferenceException {
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
		public boolean isConfigurationVerified(final E e);

		public Configuration getConfiguration();

		public boolean isInConfiguration(final Solvis solvis);

		public String getName();

		public String getElementType();
	}

	@Override
	public Iterator<E> iterator() {
		return this.elements.iterator();
	}

}