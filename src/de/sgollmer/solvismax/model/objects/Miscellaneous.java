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
	private final int powerOffDetectedAfterTimeout_ms;
	private final int unsuccessfullWaitTime_ms;
	private final int releaseBlockingAfterUserAccess_ms;
	private final int releaseBlockingAfterServiceAccess_ms;
	private final int connectionHoldTime_ms;
	private final int solvisConnectionTimeout_ms;
	private final int solvisReadTimeout_ms;

	public Miscellaneous(int measurementsBackupTime_ms, int powerOffDetectedAfterIoErrors,
			int powerOffDetectedAfterTimeout_ms, int unsuccessfullWaitTime_ms, int releaseBlockingAfterUserAccess_ms,
			int releaseBlockingAfterServiceAccess_ms, int connectionHoldTime_ms, int solvisConnectionTimeout_ms,
			int solvisReadTimeout_ms) {
		this.measurementsBackupTime_ms = measurementsBackupTime_ms;
		this.powerOffDetectedAfterIoErrors = powerOffDetectedAfterIoErrors;
		this.powerOffDetectedAfterTimeout_ms = powerOffDetectedAfterTimeout_ms;
		this.unsuccessfullWaitTime_ms = unsuccessfullWaitTime_ms;
		this.releaseBlockingAfterUserAccess_ms = releaseBlockingAfterUserAccess_ms;
		this.releaseBlockingAfterServiceAccess_ms = releaseBlockingAfterServiceAccess_ms;
		this.connectionHoldTime_ms = connectionHoldTime_ms;
		this.solvisConnectionTimeout_ms = solvisConnectionTimeout_ms;
		this.solvisReadTimeout_ms = solvisReadTimeout_ms;
	}

	public int getPowerOffDetectedAfterIoErrors() {
		return this.powerOffDetectedAfterIoErrors;
	}

	public int getUnsuccessfullWaitTime_ms() {
		return this.unsuccessfullWaitTime_ms;
	}

	public int getReleaseBlockingAfterUserAccess_ms() {
		return this.releaseBlockingAfterUserAccess_ms;
	}

	public int getMeasurementsBackupTime_ms() {
		return this.measurementsBackupTime_ms;
	}

	public static class Creator extends CreatorByXML<Miscellaneous> {

		private int measurementsBackupTime_ms;
		private int powerOffDetectedAfterIoErrors;
		private int powerOffDetectedAfterTimeout_ms;
		private int unsuccessfullWaitTime_ms;
		private int releaseBlockingAfterUserAccess_ms;
		private int releaseBlockingAfterServiceAccess_ms;
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
				case "powerOffDetectedAfterTimeout_ms":
					this.powerOffDetectedAfterTimeout_ms = Integer.parseInt(value);
					break;
				case "unsuccessfullWaitTime_ms":
					this.unsuccessfullWaitTime_ms = Integer.parseInt(value);
					break;
				case "releaseBlockingAfterUserAccess_ms":
					this.releaseBlockingAfterUserAccess_ms = Integer.parseInt(value);
					break;
				case "releaseBlockingAfterServiceAccess_ms":
					this.releaseBlockingAfterServiceAccess_ms = Integer.parseInt(value);
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
			return new Miscellaneous(this.measurementsBackupTime_ms, this.powerOffDetectedAfterIoErrors,
					this.powerOffDetectedAfterTimeout_ms, this.unsuccessfullWaitTime_ms,
					this.releaseBlockingAfterUserAccess_ms, this.releaseBlockingAfterServiceAccess_ms,
					this.connectionHoldTime_ms, this.solvisConnectionTimeout_ms, this.solvisReadTimeout_ms);
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
		return this.solvisReadTimeout_ms;
	}

	public int getSolvisConnectionTimeout_ms() {
		return this.solvisConnectionTimeout_ms;
	}

	public int getPowerOffDetectedAfterTimeout_ms() {
		return this.powerOffDetectedAfterTimeout_ms;
	}

	public int getReleaseBlockingAfterServiceAccess_ms() {
		return this.releaseBlockingAfterServiceAccess_ms;
	}

}
