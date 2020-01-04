/************************************************************************
 * 
 * $Id$
 *
 * 
 ************************************************************************/

package de.sgollmer.solvismax.model.objects.data;

import de.sgollmer.solvismax.model.objects.backup.Measurement;

public class BooleanValue implements SingleData<Boolean> {

	boolean value;

	public BooleanValue(boolean value) {
		this.value = value;
	}

	@Override
	public Boolean get() {
		return value;
	}

	@Override
	public Integer getInt() {
		return this.value ? 1 : 0;
	}

	@Override
	public SingleData<Boolean> create(int value) {
		return new BooleanValue( value > 0 );
	}

	@Override
	public String toString() {
		return Boolean.toString(value) ;
	}
	
	@Override
	public boolean equals( Object obj ) {
		if ( obj instanceof BooleanValue ) {
			return this.value == ((BooleanValue)obj).value ;
		}
		return false ;
	}

	@Override
	public int hashCode() {
		return value?179:72;
	}

	@Override
	public String getXmlId() {
		return Measurement.XML_MEASUREMENT_BOOLEAN;
	}

	@Override
	public String toJson() {
		return Boolean.toString(this.value);
	}
	
	

}
