/************************************************************************
 * 
 * $Id$
 *
 * 
 ************************************************************************/

package de.sgollmer.solvismax.model.objects.data;

public abstract class AbstractMode<C> implements Comparable<C> {

	protected final String id;

	public AbstractMode(final String id) {
		this.id = id;
	}

	public String getId() {
		return this.id;
	}

	public abstract ModeValue<?> create(final long timeStamp);
}
