package de.sgollmer.solvismax.model.objects.data;

import java.util.Collection;

import de.sgollmer.solvismax.error.TypeError;

public class IntegerValue implements SingleData {
	private final Integer data ;
	
	public IntegerValue( Integer value ) { 
		this.data = value ;
	}

	@Override
	public SingleData average(Collection<SingleData> values) {
		Integer average = 0 ;
		int cnt = 0 ;
		for ( SingleData data : values ) {
			if ( ! ( data instanceof IntegerValue )) {
				throw new TypeError( "Type error on calculation average ") ;
			}
			average += ((IntegerValue)data).data ;
			++cnt ;
		}
		if ( cnt == 0 ) {
			average = null ;
		}
		return( new IntegerValue( average )) ;
	}
	
	@Override
	public boolean equals( Object obj ) {
		if ( obj instanceof IntegerValue ) {
			return this.data.equals(((IntegerValue)obj).data) ;
		} else {
			return false ;
		}
	}
	
	@Override
	public int hashCode() {
		return this.data.hashCode() ;
	}

	/**
	 * @return the data
	 */
	public Integer getData() {
		return data;
	}
	
}
