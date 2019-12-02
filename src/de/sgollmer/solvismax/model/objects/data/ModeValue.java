package de.sgollmer.solvismax.model.objects.data;

public class ModeValue< M extends ModeI > implements SingleData {
	
	private final M mode ;
	
	public ModeValue( M mode) {
		this.mode = mode ;
	}

	public M get() {
		return this.mode ;
	}

	@Override
	public Integer getInt() {
		return null;
	}

	@Override
	public SingleData create(int value) {
		return null;
	}
	
	@Override
	public boolean equals( Object obj ) {
		if ( obj instanceof ModeValue<?> ) {
			return this.mode.getName().equals(((ModeValue<?>) obj).mode.getName() ) ;
		} else {
			return false ;
		}
	}

	@Override
	public int hashCode() {
		return this.mode.hashCode() ;
	}
	
	@Override
	public String toString() {
		return this.mode.getName() ;
	}

}
