/************************************************************************
 * 
 * $Id: 73_SolvisClient.pm 78 2020-01-03 17:50:08Z stefa $
 *
 * 
 ************************************************************************/

package de.sgollmer.solvismax.model.objects.data;

import de.sgollmer.solvismax.model.objects.backup.Measurement;

public class StringData implements SingleData<String> {
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
	public SingleData<String> create(int value) {
		return null;
	}

	@Override
	public String getXmlId() {
		return Measurement.XML_MEASUREMENT_STRING;
	}

	@Override
	public String toJson() {
		return "\"" + this.data + "\"" ;
	}

	@Override
	public String get() {
		return data;
	}
	
}
