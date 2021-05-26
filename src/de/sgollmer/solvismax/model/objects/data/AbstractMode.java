/************************************************************************
 * 
 * $Id$
 *
 * 
 ************************************************************************/

package de.sgollmer.solvismax.model.objects.data;

public abstract class AbstractMode<C> implements Comparable<C>{
	
	protected final String id ;
	
	public AbstractMode( String id ) {
		this.id = id;
	}
	
	public String getId() {
		return this.id;
	}
	
	public abstract ModeValue<?> create( long timeStamp);
}
