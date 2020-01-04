/************************************************************************
 * 
 * $Id$
 *
 * 
 ************************************************************************/

package de.sgollmer.solvismax.objects;

import javax.xml.namespace.QName;

import de.sgollmer.solvismax.error.XmlError;
import de.sgollmer.solvismax.xml.CreatorByXML;
import de.sgollmer.solvismax.xml.BaseCreator;

public class Field {
	private final int position ;
	private final int length ;
	
	public Field( int position, int length ) {
		this.position = position ;
		this.length = length ;
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
	
	public static class Creator extends CreatorByXML<Field> {

		private int position ;
		private int length ;

		public Creator(String id, BaseCreator<?> creator) {
			super(id, creator);
		}

		@Override
		public void setAttribute(QName name, String value) {
			switch( name.getLocalPart()) {
				case "position":
					this.position = Integer.parseInt(value) ;
					break ;
				case "length":
					this.length = Integer.parseInt(value) ;
			}
			
		}

		@Override
		public Field create() throws XmlError {
			return new Field(position, length);
		}

		@Override
		public CreatorByXML<?> getCreator(QName name) {
			return null;
		}

		@Override
		public void created(CreatorByXML<?> creator, Object created) {
		}
		
	}

}
