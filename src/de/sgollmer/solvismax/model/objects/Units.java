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

import de.sgollmer.solvismax.connection.AccountInfo;
import de.sgollmer.solvismax.error.XmlError;
import de.sgollmer.solvismax.model.objects.clock.ClockAdjustment;
import de.sgollmer.solvismax.xml.BaseCreator;
import de.sgollmer.solvismax.xml.CreatorByXML;

public class Units {

	// private static final Logger logger =
	// LogManager.getLogger(SolvisConnection.class);

	private static final String XML_UNITS_UNIT = "Unit";
	private static final String XML_FEATURES = "Features";
	private static final String XML_CLOCK_TUNING = "ClockTuning";
	private static final String XML_CLOCK_FINE_TUNING = "ClockFineTuning";
	private static final String XML_HEATING_BURNER_TIME_SYNC = "HeatingBurnerTimeSynchronisation";
	private static final String XML_UPDATE_AFTER_USER_ACCESS = "UpdateAfterUserAccess";
	private static final String XML_DETECT_SERVICE_ACCESS = "DetectServiceAccess";
	private static final String XMl_POWEROFF_IS_SERVICE_ACCESS = "PowerOffIsServiceAccess";
	private static final String XML_ONLY_MEASUREMENT = "OnlyMeasurements";

	private final Collection<Unit> units;

	public Units(Collection<Unit> units) {
		this.units = units;
	}

	public Collection<Unit> getUnits() {
		return units;
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
		public Units create() throws XmlError, IOException {
			return new Units(units);
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

	public static class Unit implements AccountInfo {

		private static final String XML_CLOCK_ADJUSTMENT = "ClockAdjustment";

		private final String id;
		private final String type;
		private final String url;
		private final String account;
		private final String password;
		private final int defaultAverageCount;
		private final int measurementHysteresisFactor;
		private final int defaultReadMeasurementsInterval_ms;
		private final int forcedUpdateInterval_ms;
		private final int bufferedInterval_ms;
		private final int watchDogTime_ms;
		private final boolean delayAfterSwitchingOnEnable;
		private final ClockAdjustment clockAdjustment;
		private final boolean fwLth2_21_02A;
		private final Features features;

		public Unit(String id, String type, String url, String account, String password, int defaultAverageCount,
				int measurementHysteresisFactor, int defaultReadMeasurementsInterval_ms, int forcedUpdateInterval_ms,
				int bufferedInterval_ms, int watchDogTime_ms, boolean delayAfterSwitchingOn, boolean fwLth2_21_02A,
				ClockAdjustment clockAdjustment, Features features) {
			this.id = id;
			this.type = type;
			this.url = url;
			this.account = account;
			this.password = password;
			this.defaultAverageCount = defaultAverageCount;
			this.measurementHysteresisFactor = measurementHysteresisFactor;
			this.defaultReadMeasurementsInterval_ms = defaultReadMeasurementsInterval_ms;
			this.forcedUpdateInterval_ms = forcedUpdateInterval_ms;
			this.bufferedInterval_ms = bufferedInterval_ms;
			this.watchDogTime_ms = watchDogTime_ms;
			this.delayAfterSwitchingOnEnable = delayAfterSwitchingOn;
			this.clockAdjustment = clockAdjustment;
			this.fwLth2_21_02A = fwLth2_21_02A;
			this.features = features;
		}

		public String getId() {
			return id;
		}

		public String getType() {
			return type;
		}

		public String getUrl() {
			return url;
		}

		@Override
		public String getAccount() {
			return account;
		}

		public ClockAdjustment getClockAdjustment() {
			return clockAdjustment;
		}

		public Features getFeatures() {
			return features;
		}

		public int getWatchDogTime_ms() {
			return watchDogTime_ms;
		}

		public static class Creator extends CreatorByXML<Unit> {

			private String id;
			private String type;
			private String url;
			private String account;
			private String password;
			private int defaultAverageCount;
			private int measurementHysteresisFactor;
			private int defaultReadMeasurementsInterval_ms;
			private int forcedUpdateInterval_ms;
			private int bufferedInterval_ms;
			private int watchDogTime_ms;
			private boolean delayAfterSwitchingOnEnable = false;
			private boolean fwLth2_21_02A = false;
			private ClockAdjustment clockAdjustment;
			private Features features;

			public Creator(String id, BaseCreator<?> creator) {
				super(id, creator);
			}

			@Override
			public void setAttribute(QName name, String value) {
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
					case "password":
						this.password = value;
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
					case "bufferedInterval_ms":
						this.bufferedInterval_ms = Integer.parseInt(value);
						break;
					case "watchDogTime_ms":
						this.watchDogTime_ms = Integer.parseInt(value);
						break;
					case "delayAfterSwitchingOnEnable":
						this.delayAfterSwitchingOnEnable = Boolean.parseBoolean(value);
						break;
					case "fwLth2_21_02A":
						this.fwLth2_21_02A = Boolean.parseBoolean(value);
						break;
				}

			}

			@Override
			public Unit create() throws XmlError, IOException {
				if (clockAdjustment == null) {
					clockAdjustment = new ClockAdjustment();
				}
				return new Unit(id, type, url, account, password, defaultAverageCount, measurementHysteresisFactor,
						defaultReadMeasurementsInterval_ms, forcedUpdateInterval_ms, bufferedInterval_ms,
						watchDogTime_ms, delayAfterSwitchingOnEnable, fwLth2_21_02A, clockAdjustment, features);

			}

			@Override
			public CreatorByXML<?> getCreator(QName name) {
				String id = name.getLocalPart();
				switch (id) {
					case XML_CLOCK_ADJUSTMENT:
						return new ClockAdjustment.Creator(id, this.getBaseCreator());
					case XML_FEATURES:
						return new Features.Creator(id, getBaseCreator());
				}
				return null;
			}

			@Override
			public void created(CreatorByXML<?> creator, Object created) {
				switch (creator.getId()) {
					case XML_CLOCK_ADJUSTMENT:
						this.clockAdjustment = (ClockAdjustment) created;
						break;
					case XML_FEATURES:
						this.features = (Features) created;
						break;
				}
			}

		}

		@Override
		public char[] createPassword() {
			return this.password.toCharArray();
		}

		public String getPassword() {
			return password;
		}

		public int getDefaultAverageCount() {
			return defaultAverageCount;
		}

		public int getMeasurementHysteresisFactor() {
			return measurementHysteresisFactor;
		}

		public int getDefaultReadMeasurementsInterval_ms() {
			return defaultReadMeasurementsInterval_ms;
		}

		public int getForcedUpdateInterval_ms() {
			return forcedUpdateInterval_ms;
		}

		public int getBufferedInterval_ms() {
			return bufferedInterval_ms;
		}

		public boolean isDelayAfterSwitchingOnEnable() {
			return delayAfterSwitchingOnEnable;
		}

		public boolean isFwLth2_21_02A() {
			return fwLth2_21_02A;
		}

	}

	public static class Features {

		private final boolean clockTuning;
		private final boolean clockFineTuning;
		private final boolean heatingBurnerTimeSynchronisation;
		private final boolean updateAfterUserAccess;
		private final boolean detectServiceAccess;
		private final boolean powerOffIsServiceAccess;
		private final boolean onlyMeasurements;

		public Features(boolean clockTuning, boolean clockFineTuning, boolean heatingBurnerTimeSynchronisation,
				boolean updateAfterUserAccess, boolean detectServiceAccess, boolean powerOffIsServiceAccess,
				boolean onlyMeasurements) {
			this.clockTuning = clockTuning;
			this.clockFineTuning = clockFineTuning;
			this.heatingBurnerTimeSynchronisation = heatingBurnerTimeSynchronisation;
			this.updateAfterUserAccess = updateAfterUserAccess;
			this.detectServiceAccess = detectServiceAccess;
			this.powerOffIsServiceAccess = powerOffIsServiceAccess;
			this.onlyMeasurements = onlyMeasurements;
		}

		public boolean isClockTuning() {
			return clockTuning;
		}

		public boolean isClockFineTuning() {
			return clockFineTuning;
		}

		public boolean isHeatingBurnerTimeSynchronisation() {
			return heatingBurnerTimeSynchronisation;
		}

		public boolean isUpdateAfterUserAccess() {
			return updateAfterUserAccess;
		}

		public boolean isOnlyMeasurements() {
			return onlyMeasurements;
		}

		public boolean isDetectServiceAccess() {
			return detectServiceAccess;
		}

		public boolean isPowerOffIsServiceAccess() {
			return powerOffIsServiceAccess;
		}

		public static class Creator extends CreatorByXML<Features> {

			private boolean clockTuning;
			private boolean clockFineTuning;
			private boolean heatingBurnerTimeSynchronisation;
			private boolean updateAfterUserAccess;
			private boolean detectServiceAccess;
			private boolean powerOffIsServiceAccess;
			private boolean onlyMeasurements;

			public Creator(String id, BaseCreator<?> creator) {
				super(id, creator);
			}

			@Override
			public void setAttribute(QName name, String value) {
			}

			@Override
			public Features create() throws XmlError, IOException {
				return new Features(clockTuning, clockFineTuning, heatingBurnerTimeSynchronisation,
						updateAfterUserAccess, detectServiceAccess, powerOffIsServiceAccess, onlyMeasurements);
			}

			@Override
			public CreatorByXML<?> getCreator(QName name) {
				String id = name.getLocalPart();
				switch (id) {
					case XML_CLOCK_TUNING:
					case XML_CLOCK_FINE_TUNING:
					case XML_HEATING_BURNER_TIME_SYNC:
					case XML_UPDATE_AFTER_USER_ACCESS:
					case XML_DETECT_SERVICE_ACCESS:
					case XMl_POWEROFF_IS_SERVICE_ACCESS:
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
						case XML_CLOCK_FINE_TUNING:
							this.clockFineTuning = bool;
							break;
						case XML_HEATING_BURNER_TIME_SYNC:
							this.heatingBurnerTimeSynchronisation = bool;
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
