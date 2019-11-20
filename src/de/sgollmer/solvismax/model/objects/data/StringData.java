package de.sgollmer.solvismax.model.objects.data;

import java.util.Collection;

public class StringData implements SingleData {
	private final String data ;
	
	public StringData( String data ) {
		this.data = data ;
	}

	@Override
	public SingleData average(Collection<SingleData> values) {
		return null;
	}
	
	@Override
	public boolean equals( Object obj) {
		if ( obj instanceof StringData ) {
			return this.data.equals(((StringData)obj).data ) ;
		} else {
			return false ;
		}
	}
	
	
	@Override
	public int hashCode() {
		return this.data.hashCode() ;
	}

	@Override
	public String toString() {
		return this.data ;
	}
	
}
