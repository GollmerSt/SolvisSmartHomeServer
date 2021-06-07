/************************************************************************
 * 
 * $Id$
 *
 * 
 ************************************************************************/

package de.sgollmer.solvismax.model.objects.data;

import de.sgollmer.solvismax.helper.Helper;

public abstract class SingleData<T> implements Comparable<SingleData<?>> {

	private final long timeStamp;

	protected SingleData(final long timeStamp) {
		this.timeStamp = timeStamp;
	}

	public long getTimeStamp() {
		return this.timeStamp;
	}

	/**
	 * 
	 * @return integer value if supported. Otherwise null.
	 */
	public abstract Integer getInt();

	/**
	 * 
	 * @return integer value if supported. Otherwise null.
	 */
	public abstract Long getLong();

	/**
	 * 
	 * @return double value if supported. Otherwise null.
	 */
	public abstract Double getDouble();

	/**
	 * 
	 * @return boolean value if supported. Otherwise null.
	 */
	public abstract Helper.Boolean getBoolean();

	abstract SingleData<T> create(final int value, final long timeStamp);

	public abstract SingleData<T> clone(final long timeStamp);

	public abstract String getXmlId();

	public abstract String toJson();

	public abstract T get();

	boolean isFastChange() {
		return false;
	}
}
