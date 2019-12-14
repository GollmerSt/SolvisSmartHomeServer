package de.sgollmer.solvismax.model;

import java.io.IOException;

import javax.xml.namespace.QName;

import org.slf4j.LoggerFactory;

import de.sgollmer.solvismax.error.XmlError;
import de.sgollmer.solvismax.model.objects.Units;
import de.sgollmer.solvismax.xml.BaseCreator;
import de.sgollmer.solvismax.xml.CreatorByXML;

public class BaseData {

	private static final org.slf4j.Logger logger = LoggerFactory.getLogger(BaseData.class);
	private static final String XML_BASEDATA_UNITS = "Units";

	private final Units units ;
	
	public BaseData( Units units ) {
		this.units = units ;
	}
	
	public Units getUnits() {
		return units;
	}

	public static class Creator extends BaseCreator<BaseData> {

		private Units units ;

		public Creator(String id) {
			super(id);
		}

		@Override
		public void setAttribute(QName name, String value) {
		}

		@Override
		public BaseData create() throws XmlError, IOException {
			logger.debug("BaseData created");
			return new BaseData(units);
		}

		@Override
		public CreatorByXML<?> getCreator(QName name) {
			String id = name.getLocalPart() ;
			switch ( id ) {
				case XML_BASEDATA_UNITS :
					logger.debug("Units.Creator created");
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
