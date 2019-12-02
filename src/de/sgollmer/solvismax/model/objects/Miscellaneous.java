package de.sgollmer.solvismax.model.objects;

import java.io.IOException;

import javax.xml.namespace.QName;

import de.sgollmer.solvismax.error.XmlError;
import de.sgollmer.solvismax.xml.BaseCreator;
import de.sgollmer.solvismax.xml.CreatorByXML;

public class Miscellaneous {

	private final int defaultAverageCount;
	private final int defaultReadMeasurementsIntervall;
	private final int powerOffDetectedAfterIoErrors ;

	public Miscellaneous(int defaultAverageCount, int defaultReadMeasurementsIntervall, int powerOffDetectedAfterIoErrors) {
		this.defaultAverageCount = defaultAverageCount;
		this.defaultReadMeasurementsIntervall = defaultReadMeasurementsIntervall;
		this.powerOffDetectedAfterIoErrors = powerOffDetectedAfterIoErrors ;
	}

	public int getDefaultAverageCount() {
		return defaultAverageCount;
	}

	public int getDefaultReadMeasurementsIntervall() {
		return defaultReadMeasurementsIntervall;
	}

	public int getPowerOffDetectedAfterIoErrors() {
		return powerOffDetectedAfterIoErrors;
	}

	public static class Creator extends CreatorByXML<Miscellaneous> {

		private int defaultAverageCount;
		private  int defaultReadMeasurementsIntervall;
		private int powerOffDetectedAfterIoErrors;
		
		public Creator(String id, BaseCreator<?> creator) {
			super(id, creator);
		}

		@Override
		public void setAttribute(QName name, String value) {
			switch( name.getLocalPart() ) {
				case "defaultAverageCount":
					this.defaultAverageCount = Integer.parseInt(value) ;
					break;
				case "defaultReadMeasurementsIntervall_ms":
					this.defaultReadMeasurementsIntervall = Integer.parseInt(value) ;
					break;
				case "powerOffDetectedAfterIoErrors":
					this.powerOffDetectedAfterIoErrors = Integer.parseInt(value) ;
			}
			
		}

		@Override
		public Miscellaneous create() throws XmlError, IOException {
			return new Miscellaneous(defaultAverageCount, defaultReadMeasurementsIntervall, powerOffDetectedAfterIoErrors);
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
