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

import de.sgollmer.solvismax.model.Solvis;
import de.sgollmer.xmllibrary.XmlException;

public class OfConfigs<E extends OfConfigs.IElement<E>> implements Iterable<E> {
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
		return this.get(solvis, false);
	}

	public E get(final Solvis solvis, boolean init) {
		if (solvis.getConfigurationMask() == 0 && this.elements.size() == 1) {
			return this.elements.iterator().next();
		}
		for (E e : this.elements) {
			if (e.isInConfiguration(solvis, init)) {
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

	public interface IElement<E> {
		public boolean isConfigurationVerified(final E e);

		public Configuration getConfiguration();

		public boolean isInConfiguration(final Solvis solvis, boolean init);

		public String getName();

		public String getElementType();
	}

	@Override
	public Iterator<E> iterator() {
		return this.elements.iterator();
	}

}