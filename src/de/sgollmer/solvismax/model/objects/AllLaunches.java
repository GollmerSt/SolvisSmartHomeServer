package de.sgollmer.solvismax.model.objects;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;

import javax.xml.namespace.QName;

import de.sgollmer.solvismax.error.XmlError;
import de.sgollmer.solvismax.objects.Field;
import de.sgollmer.solvismax.xml.BaseCreator;
import de.sgollmer.solvismax.xml.CreatorByXML;

public class AllLaunches {

	private static final String XML_LAUNCH = "Launch";

	private final Collection<Launch> launches;

	public AllLaunches(Collection<Launch> launches) {
		this.launches = launches;
	}

	public static class Creator extends CreatorByXML<AllLaunches> {

		private final Collection<Launch> launches = new ArrayList<>();

		public Creator(String id, BaseCreator<?> creator) {
			super(id, creator);
		}

		@Override
		public void setAttribute(QName name, String value) {
		}

		@Override
		public AllLaunches create() throws XmlError, IOException {
			return new AllLaunches(launches);
		}

		@Override
		public CreatorByXML<?> getCreator(QName name) {
			String id = name.getLocalPart();
			switch (id) {
				case XML_LAUNCH:
					return new Launch.Creator(id, getBaseCreator());
			}
			return null;
		}

		@Override
		public void created(CreatorByXML<?> creator, Object created) {
			switch (creator.getId()) {
				case XML_LAUNCH:
					this.launches.add((Launch) created);
					break;
			}

		}

	}

	public static class Launch {
		
		private static final String XML_TOUCH_POINT = "TouchPoint" ;
		private static final String XML_FIELD = "Field" ;

		private final String id;
		private final TouchPoint touchPoint;
		private final Field field;

		public Launch(String id, TouchPoint touchPoint, Field field) {
			this.id = id;
			this.touchPoint = touchPoint;
			this.field = field;
		}

		public static class Creator extends CreatorByXML<Launch> {

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
			public Launch create() throws XmlError, IOException {
				return new Launch(id, touchPoint, field);
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

}
