package de.sgollmer.solvismax.objects;

public class Field {
	private final String id ;
	private final int position ;
	private final int length ;
	
	public Field( String id, int position, int length ) {
		this.id = id ;
		this.position = position ;
		this.length = length ;
	}

	public String getId() {
		return id;
	}

	public int getPosition() {
		return position;
	}

	public int getLength() {
		return length;
	}
	
	public String subString( String data ) {
		return data.substring(position, position + length ) ;
	}
}
