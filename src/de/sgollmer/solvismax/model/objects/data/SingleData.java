/************************************************************************
 * 
 * $Id: 73_SolvisClient.pm 78 2020-01-03 17:50:08Z stefa $
 *
 * 
 ************************************************************************/

package de.sgollmer.solvismax.model.objects.data;

public interface SingleData< T > {
	//public SingleData average( Collection< SingleData > values ) ; 
	public Integer getInt() ;
	public SingleData< T > create( int value ) ;
	public String getXmlId() ;
	public String toJson() ;
	public T get() ;
}
