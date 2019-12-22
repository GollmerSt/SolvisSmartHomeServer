package de.sgollmer.solvismax.model.objects;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;

import javax.xml.namespace.QName;

import de.sgollmer.solvismax.error.XmlError;
import de.sgollmer.solvismax.objects.Field;
import de.sgollmer.solvismax.xml.BaseCreator;
import de.sgollmer.solvismax.xml.CreatorByXML;

public class AllPreparations {

	private static final String XML_PREPARATION = "Preparation";

	private final Collection<Preparation> preparations;

	public AllPreparations(Collection<Preparation> preparations) {
		this.preparations = preparations;
	}

	public static class Creator extends CreatorByXML<AllPreparations> {

		private final Collection<Preparation> preparations = new ArrayList<>();

		public Creator(String id, BaseCreator<?> creator) {
			super(id, creator);
		}

		@Override
		public void setAttribute(QName name, String value) {
		}

		@Override
		public AllPreparations create() throws XmlError, IOException {
			return new AllPreparations(preparations);
		}

		@Override
		public CreatorByXML<?> getCreator(QName name) {
			String id = name.getLocalPart();
			switch (id) {
				case XML_PREPARATION:
					return new Preparation.Creator(id, getBaseCreator());
			}
			return null;
		}

		@Override
		public void created(CreatorByXML<?> creator, Object created) {
			switch (creator.getId()) {
				case XML_PREPARATION:
					this.preparations.add((Preparation) created);
					break;
			}

		}

	}

	public static class Preparation {
		
		private static final String XML_TOUCH_POINT = "TouchPoint" ;
		private static final String XML_FIELD = "Field" ;

		private final String id;
		private final TouchPoint touchPoint;
		private final Field field;

		public Preparation(String id, TouchPoint touchPoint, Field field) {
			this.id = id;
			this.touchPoint = touchPoint;
			this.field = field;
		}

		public static class Creator extends CreatorByXML<Preparation> {

			private String id;
			private TouchPoint touchPoint;
			private Field field;

			public Creator(String id, BaseCreator<?> creator) {
				super(id, creator);
			}

			@Override
			public void setAttribute(QName name, String value) {
				switch( name.getLocalPart()) {
					case "id":
					this.id = value ;
				}
			}

			@Override
			public Preparation create() throws XmlError, IOException {
				return new Preparation(id, touchPoint, field);
			}

			@Override
			public CreatorByXML<?> getCreator(QName name) {
				String id = name.getLocalPart() ;
				switch( id ) {
					case XML_TOUCH_POINT:
						return new TouchPoint.Creator(id, getBaseCreator());
					case XML_FIELD:
						return new Field.Creator(id, getBaseCreator()) ;
				}
				return null;
			}

			@Override
			public void created(CreatorByXML<?> creator, Object created) {
				switch ( creator.getId() ) {
					case XML_TOUCH_POINT:
						this.touchPoint = (TouchPoint) created ;
						break ;
					case XML_FIELD:
						this.field = (Field) created ;
				}

			}

		}
	}
	
	public static class PreparationRef {
		private final String preparationId ;
		
		public PreparationRef( String preparationId) {
			this.preparationId = preparationId ;
		}
		
		public String getPreparationId() {
			return preparationId;
		}

		public static class Creator extends CreatorByXML<PreparationRef> {

			private String preparationId = null ;

			public Creator(String id, BaseCreator<?> creator) {
				super(id, creator);
			}

			@Override
			public void setAttribute(QName name, String value) {
				switch( name.getLocalPart() ) {
					case "preparationRef" :
						this.preparationId = value ;
				}
				
			}

			@Override
			public PreparationRef create() throws XmlError, IOException {
				return new PreparationRef(preparationId);
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


}
