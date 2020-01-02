package de.sgollmer.solvismax.model;

import java.io.IOException;

import javax.xml.namespace.QName;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import de.sgollmer.solvismax.error.XmlError;
import de.sgollmer.solvismax.model.objects.Units;
import de.sgollmer.solvismax.xml.BaseCreator;
import de.sgollmer.solvismax.xml.CreatorByXML;

public class BaseData {

	@SuppressWarnings("unused")
	private static final Logger logger = LogManager.getLogger(BaseData.class);

	private static final String XML_BASEDATA_UNITS = "Units";

	private final String timeZone ;
	public String getTimeZone() {
		return timeZone;
	}

	private final Units units ;
	
	public BaseData( String timeZone, Units units ) {
		this.timeZone = timeZone ;
		this.units = units ;
	}
	
	public Units getUnits() {
		return units;
	}

	public static class Creator extends BaseCreator<BaseData> {

		private String timeZone = "Europe/Berlin" ;
		private Units units ;

		public Creator(String id) {
			super(id);
		}

		@Override
		public void setAttribute(QName name, String value) {
			switch ( name.getLocalPart() ) {
				case "timeZone" :
					this.timeZone = value ;
			}
		}

		@Override
		public BaseData create() throws XmlError, IOException {
			return new BaseData(timeZone, units);
		}

		@Override
		public CreatorByXML<?> getCreator(QName name) {
			String id = name.getLocalPart() ;
			switch ( id ) {
				case XML_BASEDATA_UNITS :
					return new Units.Creator(id, this.getBaseCreator()) ;
			}
			return null;
		}

		@Override
		public void created(CreatorByXML<?> creator, Object created) {
			switch ( creator.getId() ) {
				case XML_BASEDATA_UNITS:
					this.units = (Units) created ;
			}
			
		}
		
	}
}
