package de.sgollmer.solvismax.model.objects.data;

public interface SingleData {
	//public SingleData average( Collection< SingleData > values ) ; 
	public Integer getInt() ;
	public SingleData create( int value ) ;
	public String getXmlId() ;
}
