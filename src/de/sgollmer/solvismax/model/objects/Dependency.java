package de.sgollmer.solvismax.model.objects;

import javax.xml.namespace.QName;

import de.sgollmer.solvismax.error.XmlError;
import de.sgollmer.solvismax.xml.CreatorByXML;
import de.sgollmer.solvismax.xml.BaseCreator;

public class Dependency implements Assigner {
	private final String id ;
	private final String dataId ;
	
	private Dependency( String id, String dataId ) {
		this.id = id ;
		this.dataId = dataId ;
	}

	public String getDataId() {
		return dataId;
	}

	public String getId() {
		return id;
	}
	
	public static class Creator extends CreatorByXML<Dependency> {

		private String id ;
		private String dataId ;

		public Creator(String id, BaseCreator<?> creator) {
			super(id, creator);
		}

		@Override
		public void setAttribute(QName name, String value) {
			switch( name.getLocalPart()) {
				case "id":
					this.id = value ;
					break ;
				case "dataId":
					this.dataId = value ;
					break ;
			}
			
		}

		@Override
		public Dependency create() throws XmlError {
			return new Dependency(id, dataId);
		}

		@Override
		public CreatorByXML<?> getCreator(QName name) {
			return null;
		}

		@Override
		public void created(CreatorByXML<?> creator, Object created) {			
		}
		
	}

	@Override
	public void assign(SolvisDescription description) {
		
	}

	
	
}
