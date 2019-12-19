package de.sgollmer.solvismax.model.objects;

import java.io.IOException;

import javax.xml.namespace.QName;

import de.sgollmer.solvismax.error.XmlError;
import de.sgollmer.solvismax.xml.BaseCreator;
import de.sgollmer.solvismax.xml.CreatorByXML;

public class Miscellaneous {

	private final int defaultAverageCount;
	private final int defaultReadMeasurementsIntervall_ms;
	private final int measurementsBackupTime_ms;
	private final int powerOffDetectedAfterIoErrors;
	private final int unsuccessfullWaitTime_ms;
	private final int releaseblockingAfterUserChange_ms;
	private final int watchDogTime_ms;
	private final int connectionHoldTime_ms ;

	public Miscellaneous(int defaultAverageCount, int defaultReadMeasurementsIntervall_ms,
			int measurementsBackupTime_ms, int powerOffDetectedAfterIoErrors, int unsuccessfullWaitTime_ms,
			int releaseblockingAfterUserChange_ms, int watchDogTime_ms, int connectionHoldTime_ms) {
		this.defaultAverageCount = defaultAverageCount;
		this.defaultReadMeasurementsIntervall_ms = defaultReadMeasurementsIntervall_ms;
		this.measurementsBackupTime_ms = measurementsBackupTime_ms;
		this.powerOffDetectedAfterIoErrors = powerOffDetectedAfterIoErrors;
		this.unsuccessfullWaitTime_ms = unsuccessfullWaitTime_ms;
		this.releaseblockingAfterUserChange_ms = releaseblockingAfterUserChange_ms;
		this.watchDogTime_ms = watchDogTime_ms;
		this.connectionHoldTime_ms = connectionHoldTime_ms ;
	}

	public int getDefaultAverageCount() {
		return defaultAverageCount;
	}

	public int getDefaultReadMeasurementsIntervall() {
		return defaultReadMeasurementsIntervall_ms;
	}

	public int getPowerOffDetectedAfterIoErrors() {
		return powerOffDetectedAfterIoErrors;
	}

	public int getUnsuccessfullWaitTime_ms() {
		return unsuccessfullWaitTime_ms;
	}

	public int getReleaseblockingAfterUserChange_ms() {
		return releaseblockingAfterUserChange_ms;
	}

	public int getWatchDogTime_ms() {
		return watchDogTime_ms;
	}

	public int getMeasurementsBackupTime_ms() {
		return measurementsBackupTime_ms;
	}

	public static class Creator extends CreatorByXML<Miscellaneous> {

		private int defaultAverageCount;
		private int defaultReadMeasurementsIntervall_ms;
		private int measurementsBackupTime_ms;
		private int powerOffDetectedAfterIoErrors;
		private int unsuccessfullWaitTime_ms;
		private int releaseblockingAfterUserChange_ms;
		private int watchDogTime_ms;
		private int connectionHoldTime_ms;

		public Creator(String id, BaseCreator<?> creator) {
			super(id, creator);
		}

		@Override
		public void setAttribute(QName name, String value) {
			switch (name.getLocalPart()) {
				case "defaultAverageCount":
					this.defaultAverageCount = Integer.parseInt(value);
					break;
				case "defaultReadMeasurementsIntervall_ms":
					this.defaultReadMeasurementsIntervall_ms = Integer.parseInt(value);
					break;
				case "measurementsBackupTime_ms":
					this.measurementsBackupTime_ms = Integer.parseInt(value);
					break;
				case "powerOffDetectedAfterIoErrors":
					this.powerOffDetectedAfterIoErrors = Integer.parseInt(value);
					break;
				case "unsuccessfullWaitTime_ms":
					this.unsuccessfullWaitTime_ms = Integer.parseInt(value);
					break;
				case "releaseblockingAfterUserChange_ms":
					this.releaseblockingAfterUserChange_ms = Integer.parseInt(value);
					break;
				case "watchDogTime_ms":
					this.watchDogTime_ms = Integer.parseInt(value);
					break;
				case "connectionHoldTime_ms":
					this.connectionHoldTime_ms = Integer.parseInt(value) ;
			}

		}

		@Override
		public Miscellaneous create() throws XmlError, IOException {
			return new Miscellaneous(defaultAverageCount, defaultReadMeasurementsIntervall_ms,
					measurementsBackupTime_ms, powerOffDetectedAfterIoErrors, unsuccessfullWaitTime_ms,
					releaseblockingAfterUserChange_ms, watchDogTime_ms, connectionHoldTime_ms);
		}

		@Override
		public CreatorByXML<?> getCreator(QName name) {
			return null;
		}

		@Override
		public void created(CreatorByXML<?> creator, Object created) {

		}

	}

	public int getConnectionHoldTime() {
		return this.connectionHoldTime_ms;
	}
}
