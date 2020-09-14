/************************************************************************
 * 
 * $Id$
 *
 * 
 ************************************************************************/

package de.sgollmer.solvismax.model.objects.data;

public abstract class SingleData<T> {

	private final long timeStamp;

	protected SingleData(long timeStamp) {
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
	 * @return boolean value if supported. Otherwise null.
	 */
	public abstract Boolean getBoolean();

	SingleData<T> create(int value, long timeStamp, boolean fastChange) {
		return this.create(value, timeStamp);
	}

	abstract SingleData<T> create(int value, long timeStamp);

	public abstract String getXmlId();

	public abstract String toJson();

	public abstract T get();

	boolean isFastChange() {
		return false;
	}

}
