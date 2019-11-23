package de.sgollmer.solvismax.model.objects.data;

public class StringData implements SingleData {
	private final String data ;
	
	public StringData( String data ) {
		this.data = data ;
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

	@Override
	public Integer getInt() {
		return null;
	}

	@Override
	public SingleData create(long divisor, int divident) {
		return null;
	}
	
}
