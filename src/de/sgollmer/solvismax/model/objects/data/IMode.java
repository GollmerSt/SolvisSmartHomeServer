/************************************************************************
 * 
 * $Id$
 *
 * 
 ************************************************************************/

package de.sgollmer.solvismax.model.objects.data;

public interface IMode<C> extends Comparable<C>{
	
	public String getName() ;
	
	public ModeValue<?> create( long timeStamp);
	
	public enum Handling {
		READ, WRITE, BOTH
	}
	
	public Handling getHandling();
}
