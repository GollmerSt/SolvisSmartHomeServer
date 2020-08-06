/************************************************************************
 * 
 * $Id$
 *
 * 
 ************************************************************************/

package de.sgollmer.solvismax.model.objects;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;

import javax.xml.namespace.QName;

import de.sgollmer.solvismax.Constants;
import de.sgollmer.solvismax.connection.IAccountInfo;
import de.sgollmer.solvismax.crypt.CryptAes;
import de.sgollmer.solvismax.error.CryptDefaultValueException;
import de.sgollmer.solvismax.error.CryptExeception;
import de.sgollmer.solvismax.error.XmlException;
import de.sgollmer.solvismax.log.LogManager;
import de.sgollmer.solvismax.log.LogManager.DelayedMessage;
import de.sgollmer.solvismax.log.LogManager.Level;
import de.sgollmer.solvismax.xml.BaseCreator;
import de.sgollmer.solvismax.xml.CreatorByXML;

public class Units {

	private static final String XML_UNITS_UNIT = "Unit";
	private static final String XML_FEATURES = "Features";
	private static final String XML_CLOCK_TUNING = "ClockTuning";
	private static final String XML_EQUIPMENT_TIME_SYNC = "EquipmentTimeSynchronisation";
	private static final String XML_UPDATE_AFTER_USER_ACCESS = "UpdateAfterUserAccess";
	private static final String XML_DETECT_SERVICE_ACCESS = "DetectServiceAccess";
	private static final String XMl_POWEROFF_IS_SERVICE_ACCESS = "PowerOffIsServiceAccess";
	private static final String XMl_SEND_MAIL_ON_ERROR = "SendMailOnError";
	private static final String XMl_CLEAR_ERROR_MESSAGE_AFTER_MAIL = "ClearErrorMessageAfterMail";
	private static final String XML_ONLY_MEASUREMENT = "OnlyMeasurements";

	private final Collection<Unit> units;

	private Units(Collection<Unit> units) {
		this.units = units;
	}

	public Collection<Unit> getUnits() {
		return this.units;
	}

	public static class Creator extends CreatorByXML<Units> {

		private final Collection<Unit> units = new ArrayList<>();

		public Creator(String id, BaseCreator<?> creator) {
			super(id, creator);
		}

		@Override
		public void setAttribute(QName name, String value) {
		}

		@Override
		public Units create() throws XmlException, IOException {
			return new Units(this.units);
		}

		@Override
		public CreatorByXML<?> getCreator(QName name) {
			String id = name.getLocalPart();
			switch (id) {
				case XML_UNITS_UNIT:
					return new Unit.Creator(id, this.getBaseCreator());
			}
			return null;
		}

		@Override
		public void created(CreatorByXML<?> creator, Object created) {
			switch (creator.getId()) {
				case XML_UNITS_UNIT:
					this.units.add((Unit) created);
					break;
			}
		}

	}

	public static class Unit implements IAccountInfo {

		private final String id;
		private final String type;
		private final String url;
		private final String account;
		private final CryptAes password;
		private final int configOrMask;
		private final int defaultAverageCount;
		private final int measurementHysteresisFactor;
		private final int defaultReadMeasurementsInterval_ms;
		private final int forcedUpdateInterval_ms;
		private final int doubleUpdateInterval_ms;
		private final int bufferedInterval_ms;
		private final int watchDogTime_ms;
		private final int releaseBlockingAfterUserAccess_ms;
		private final int releaseBlockingAfterServiceAccess_ms;
		private final boolean delayAfterSwitchingOnEnable;
		private final boolean fwLth2_21_02A;
		private final boolean modbus;
		private final Features features;
		private final int ignoredFrameThicknesScreenSaver;

		private Unit(String id, String type, String url, String account, CryptAes password, int configOrMask,
				int defaultAverageCount, int measurementHysteresisFactor, int defaultReadMeasurementsInterval_ms,
				int forcedUpdateInterval_ms, int doubleUpdateInterval_ms, int bufferedInterval_ms, int watchDogTime_ms,
				int releaseBlockingAfterUserAccess_ms, int releaseBlockingAfterServiceAccess_ms,
				boolean delayAfterSwitchingOn, boolean fwLth2_21_02A, boolean modbus, Features features,
				int ignoredFrameThicknesScreenSaver) {
			this.id = id;
			this.type = type;
			this.url = url;
			this.account = account;
			this.password = password;
			this.configOrMask = configOrMask;
			this.defaultAverageCount = defaultAverageCount;
			this.measurementHysteresisFactor = measurementHysteresisFactor;
			this.defaultReadMeasurementsInterval_ms = defaultReadMeasurementsInterval_ms;
			this.forcedUpdateInterval_ms = forcedUpdateInterval_ms;
			this.doubleUpdateInterval_ms = doubleUpdateInterval_ms;
			this.bufferedInterval_ms = bufferedInterval_ms;
			this.watchDogTime_ms = watchDogTime_ms;
			this.releaseBlockingAfterUserAccess_ms = releaseBlockingAfterUserAccess_ms;
			this.releaseBlockingAfterServiceAccess_ms = releaseBlockingAfterServiceAccess_ms;
			this.delayAfterSwitchingOnEnable = delayAfterSwitchingOn;
			this.fwLth2_21_02A = fwLth2_21_02A;
			this.modbus = modbus;
			this.features = features;
			this.ignoredFrameThicknesScreenSaver = ignoredFrameThicknesScreenSaver;
		}

		public String getId() {
			return this.id;
		}

		public String getType() {
			return this.type;
		}

		public String getUrl() {
			return this.url;
		}

		@Override
		public String getAccount() {
			return this.account;
		}

		public Features getFeatures() {
			return this.features;
		}

		public int getWatchDogTime_ms() {
			return this.watchDogTime_ms;
		}

		private static class Creator extends CreatorByXML<Unit> {

			private String id;
			private String type;
			private String url;
			private String account;
			private CryptAes password = new CryptAes();
			private int configOrMask;
			private int defaultAverageCount;
			private int measurementHysteresisFactor;
			private int defaultReadMeasurementsInterval_ms;
			private int forcedUpdateInterval_ms;
			private int doubleUpdateInterval_ms = 0;
			private int bufferedInterval_ms;
			private int watchDogTime_ms;
			private int releaseBlockingAfterUserAccess_ms;
			private int releaseBlockingAfterServiceAccess_ms;
			private boolean delayAfterSwitchingOnEnable = false;
			private boolean fwLth2_21_02A = false;
			private boolean modbus = false;
			private Features features;
			private int ignoredFrameThicknesScreenSaver;

			private Creator(String id, BaseCreator<?> creator) {
				super(id, creator);
			}

			@Override
			public void setAttribute(QName name, String value) {
				try {
				switch (name.getLocalPart()) {
					case "id":
						this.id = value;
						break;
					case "type":
						this.type = value;
						break;
					case "url":
						this.url = value;
						break;
					case "account":
						this.account = value;
						break;
					case "configOrMask":
						this.configOrMask = Integer.decode(value);
						break;
					case "passwordCrypt":
							this.password.decrypt(value);
						break;
					case "password":
						this.password.set(value);
						break;
					case "defaultAverageCount":
						this.defaultAverageCount = Integer.parseInt(value);
						break;
					case "measurementHysteresisFactor":
						this.measurementHysteresisFactor = Integer.parseInt(value);
						break;
					case "defaultReadMeasurementsInterval_ms":
						this.defaultReadMeasurementsInterval_ms = Integer.parseInt(value);
						break;
					case "forcedUpdateInterval_ms":
						this.forcedUpdateInterval_ms = Integer.parseInt(value);
						break;
					case "doubleUpdateInterval_ms":
						this.doubleUpdateInterval_ms = Integer.parseInt(value);
						break;
					case "bufferedInterval_ms":
						this.bufferedInterval_ms = Integer.parseInt(value);
						break;
					case "watchDogTime_ms":
						this.watchDogTime_ms = Integer.parseInt(value);
						break;
					case "releaseBlockingAfterUserAccess_ms":
						this.releaseBlockingAfterUserAccess_ms = Integer.parseInt(value);
						break;
					case "releaseBlockingAfterServiceAccess_ms":
						this.releaseBlockingAfterServiceAccess_ms = Integer.parseInt(value);
						break;
					case "delayAfterSwitchingOnEnable":
						this.delayAfterSwitchingOnEnable = Boolean.parseBoolean(value);
						break;
					case "fwLth2_21_02A":
						this.fwLth2_21_02A = Boolean.parseBoolean(value);
						break;
					case "modbus":
						this.modbus = Boolean.parseBoolean(value);
						break;
					case "ignoredFrameThicknesScreenSaver":
						this.ignoredFrameThicknesScreenSaver = Integer.parseInt(value);
						break;
				}
				} catch (CryptDefaultValueException | CryptExeception e) {
					String m = "base.xml error of passwordCrypt in Unit tag: " + e.getMessage();
					LogManager.getInstance().addDelayedErrorMessage(
							new DelayedMessage(Level.ERROR, m, Unit.class, Constants.ExitCodes.CRYPTION_FAIL));
				}

			}

			@Override
			public Unit create() throws XmlException, IOException {
				return new Unit(this.id, this.type, this.url, this.account, this.password, this.configOrMask,
						this.defaultAverageCount, this.measurementHysteresisFactor,
						this.defaultReadMeasurementsInterval_ms, this.forcedUpdateInterval_ms,
						this.doubleUpdateInterval_ms, this.bufferedInterval_ms, this.watchDogTime_ms,
						this.releaseBlockingAfterUserAccess_ms, this.releaseBlockingAfterServiceAccess_ms,
						this.delayAfterSwitchingOnEnable, this.fwLth2_21_02A, this.modbus, this.features,
						this.ignoredFrameThicknesScreenSaver);

			}

			@Override
			public CreatorByXML<?> getCreator(QName name) {
				String id = name.getLocalPart();
				switch (id) {
					case XML_FEATURES:
						return new Features.Creator(id, getBaseCreator());
				}
				return null;
			}

			@Override
			public void created(CreatorByXML<?> creator, Object created) {
				switch (creator.getId()) {
					case XML_FEATURES:
						this.features = (Features) created;
						break;
				}
			}

		}

		@Override
		public char[] cP() {
			return this.password.cP();
		}

		public int getDefaultAverageCount() {
			return this.defaultAverageCount;
		}

		public int getMeasurementHysteresisFactor() {
			return this.measurementHysteresisFactor;
		}

		public int getDefaultReadMeasurementsInterval_ms() {
			return this.defaultReadMeasurementsInterval_ms;
		}

		public int getForcedUpdateInterval_ms() {
			return this.forcedUpdateInterval_ms;
		}

		public int getBufferedInterval_ms() {
			return this.bufferedInterval_ms;
		}

		public boolean isDelayAfterSwitchingOnEnable() {
			return this.delayAfterSwitchingOnEnable;
		}

		public boolean isFwLth2_21_02A() {
			return this.fwLth2_21_02A;
		}

		public int getDoubleUpdateInterval_ms() {
			return this.doubleUpdateInterval_ms;
		}

		public boolean isModbus() {
			return this.modbus;
		}

		public int getIgnoredFrameThicknesScreenSaver() {
			return this.ignoredFrameThicknesScreenSaver;
		}

		public int getConfigOrMask() {
			return this.configOrMask;
		}

		public int getReleaseBlockingAfterUserAccess_ms() {
			return this.releaseBlockingAfterUserAccess_ms;
		}

		public int getReleaseBlockingAfterServiceAccess_ms() {
			return this.releaseBlockingAfterServiceAccess_ms;
		}

	}

	public static class Features {

		private final boolean clockTuning;
		private final boolean equipmentTimeSynchronisation;
		private final boolean updateAfterUserAccess;
		private final boolean detectServiceAccess;
		private final boolean powerOffIsServiceAccess;
		private final boolean sendMailOnError;
		private final boolean clearErrorMessageAfterMail;
		private final boolean onlyMeasurements;

		private Features(boolean clockTuning, boolean equipmentTimeSynchronisation, boolean updateAfterUserAccess,
				boolean detectServiceAccess, boolean powerOffIsServiceAccess, boolean sendMailOnError,
				boolean clearErrorMessageAfterMail, boolean onlyMeasurements) {
			this.clockTuning = clockTuning;
			this.equipmentTimeSynchronisation = equipmentTimeSynchronisation;
			this.updateAfterUserAccess = updateAfterUserAccess;
			this.detectServiceAccess = detectServiceAccess;
			this.powerOffIsServiceAccess = powerOffIsServiceAccess;
			this.sendMailOnError = sendMailOnError;
			this.clearErrorMessageAfterMail = clearErrorMessageAfterMail;
			this.onlyMeasurements = onlyMeasurements;
		}

		public boolean isClockTuning() {
			return this.clockTuning;
		}

		public boolean isEquipmentTimeSynchronisation() {
			return this.equipmentTimeSynchronisation;
		}

		public boolean isUpdateAfterUserAccess() {
			return this.updateAfterUserAccess;
		}

		public boolean isOnlyMeasurements() {
			return this.onlyMeasurements;
		}

		public boolean isDetectServiceAccess() {
			return this.detectServiceAccess;
		}

		public boolean isClearErrorMessageAfterMail() {
			return this.clearErrorMessageAfterMail;
		}

		public boolean isPowerOffIsServiceAccess() {
			return this.powerOffIsServiceAccess;
		}

		public boolean isSendMailOnError() {
			return this.sendMailOnError;
		}

		private static class Creator extends CreatorByXML<Features> {

			private boolean clockTuning;
			private boolean equipmentTimeSynchronisation;
			private boolean updateAfterUserAccess;
			private boolean detectServiceAccess;
			private boolean powerOffIsServiceAccess;
			private boolean sendMailOnError;
			private boolean clearErrorMessageAfterMail;
			private boolean onlyMeasurements;

			private Creator(String id, BaseCreator<?> creator) {
				super(id, creator);
			}

			@Override
			public void setAttribute(QName name, String value) {
			}

			@Override
			public Features create() throws XmlException, IOException {
				return new Features(this.clockTuning, this.equipmentTimeSynchronisation, this.updateAfterUserAccess,
						this.detectServiceAccess, this.powerOffIsServiceAccess, this.sendMailOnError,
						this.clearErrorMessageAfterMail, this.onlyMeasurements);
			}

			@Override
			public CreatorByXML<?> getCreator(QName name) {
				String id = name.getLocalPart();
				switch (id) {
					case XML_CLOCK_TUNING:
					case XML_EQUIPMENT_TIME_SYNC:
					case XML_UPDATE_AFTER_USER_ACCESS:
					case XML_DETECT_SERVICE_ACCESS:
					case XMl_POWEROFF_IS_SERVICE_ACCESS:
					case XMl_SEND_MAIL_ON_ERROR:
					case XMl_CLEAR_ERROR_MESSAGE_AFTER_MAIL:
					case XML_ONLY_MEASUREMENT:
						return new StringElement.Creator(id, getBaseCreator());
				}
				return null;
			}

			@Override
			public void created(CreatorByXML<?> creator, Object created) {
				if (created instanceof StringElement) {
					boolean bool = Boolean.parseBoolean(((StringElement) created).toString());
					switch (creator.getId()) {
						case XML_CLOCK_TUNING:
							this.clockTuning = bool;
							break;
						case XML_EQUIPMENT_TIME_SYNC:
							this.equipmentTimeSynchronisation = bool;
							break;
						case XML_UPDATE_AFTER_USER_ACCESS:
							this.updateAfterUserAccess = bool;
							break;
						case XML_DETECT_SERVICE_ACCESS:
							this.detectServiceAccess = bool;
							break;
						case XMl_POWEROFF_IS_SERVICE_ACCESS:
							this.powerOffIsServiceAccess = bool;
							break;
						case XMl_SEND_MAIL_ON_ERROR:
							this.sendMailOnError = bool;
							break;
						case XMl_CLEAR_ERROR_MESSAGE_AFTER_MAIL:
							this.clearErrorMessageAfterMail = bool;
							break;
						case XML_ONLY_MEASUREMENT:
							this.onlyMeasurements = bool;
							break;
					}
				} else {
					System.err.println("Check <" + creator.getId() + "> in the base.xml file, wrong value format ");
				}
			}

		}
	}
}
