/************************************************************************
 * 
 * $Id$
 *
 * 
 ************************************************************************/

package de.sgollmer.solvismax.model.objects;

import java.io.IOException;

import javax.xml.namespace.QName;

import de.sgollmer.solvismax.error.XmlError;
import de.sgollmer.solvismax.xml.BaseCreator;
import de.sgollmer.solvismax.xml.CreatorByXML;

public class Miscellaneous {

	private final int measurementsBackupTime_ms;
	private final int powerOffDetectedAfterIoErrors;
	private final int unsuccessfullWaitTime_ms;
	private final int releaseblockingAfterUserChange_ms;
	private final int watchDogTime_ms;
	private final int connectionHoldTime_ms;
	private final int solvisConnectionTimeout_ms;
	private final int solvisReadTimeout_ms;

	public Miscellaneous(int measurementsBackupTime_ms, int powerOffDetectedAfterIoErrors, int unsuccessfullWaitTime_ms,
			int releaseblockingAfterUserChange_ms, int watchDogTime_ms, int connectionHoldTime_ms,
			int solvisConnectionTimeout_ms,
			int solvisReadTimeout_ms) {
		this.measurementsBackupTime_ms = measurementsBackupTime_ms;
		this.powerOffDetectedAfterIoErrors = powerOffDetectedAfterIoErrors;
		this.unsuccessfullWaitTime_ms = unsuccessfullWaitTime_ms;
		this.releaseblockingAfterUserChange_ms = releaseblockingAfterUserChange_ms;
		this.watchDogTime_ms = watchDogTime_ms;
		this.connectionHoldTime_ms = connectionHoldTime_ms;
		this.solvisConnectionTimeout_ms = solvisConnectionTimeout_ms;
		this.solvisReadTimeout_ms = solvisReadTimeout_ms;
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

		private int measurementsBackupTime_ms;
		private int powerOffDetectedAfterIoErrors;
		private int unsuccessfullWaitTime_ms;
		private int releaseblockingAfterUserChange_ms;
		private int watchDogTime_ms;
		private int connectionHoldTime_ms;
		private int solvisConnectionTimeout_ms;
		private int solvisReadTimeout_ms;

		public Creator(String id, BaseCreator<?> creator) {
			super(id, creator);
		}

		@Override
		public void setAttribute(QName name, String value) {
			switch (name.getLocalPart()) {
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
					this.connectionHoldTime_ms = Integer.parseInt(value);
					break;
				case "solvisConnectionTimeout_ms":
					this.solvisConnectionTimeout_ms = Integer.parseInt(value);
					break;
				case "solvisReadTimeout_ms":
					this.solvisReadTimeout_ms = Integer.parseInt(value);
					break;
			}

		}

		@Override
		public Miscellaneous create() throws XmlError, IOException {
			return new Miscellaneous(
					measurementsBackupTime_ms, powerOffDetectedAfterIoErrors, unsuccessfullWaitTime_ms,
					releaseblockingAfterUserChange_ms, watchDogTime_ms, connectionHoldTime_ms, solvisConnectionTimeout_ms, solvisReadTimeout_ms);
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

	public int getSolvisReadTimeout_ms() {
		return solvisReadTimeout_ms;
	}

	public int getSolvisConnectionTimeout_ms() {
		return solvisConnectionTimeout_ms;
	}

}
