/************************************************************************
 * 
 * $Id$
 *
 * 
 ************************************************************************/

package de.sgollmer.solvismax.model.objects.data;

public interface IMode<C> extends Comparable<C> {

	public String getName();

	public ModeValue<?> create(final long timeStamp);

	public enum Handling {
		RO, WO, RW;

		public String getCvsMeta() {
			return "(" + this.name().toLowerCase() + ")";
		}
	}

	public Handling getHandling();

	public String getCvsMeta();
}
