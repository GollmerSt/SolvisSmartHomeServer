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
		return timeStamp;
	}

	// public SingleData average( Collection< SingleData > values ) ;
	public abstract Integer getInt();

	public SingleData<T> create(int value, long timeStamp, boolean fastChange) {
		return this.create(value, timeStamp);
	}

	public abstract SingleData<T> create(int value, long timeStamp);

	public abstract String getXmlId();

	public abstract String toJson();

	public abstract T get();

	public boolean isFastChange() {
		return false;
	}
}
