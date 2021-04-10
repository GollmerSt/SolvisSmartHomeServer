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
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

import javax.xml.namespace.QName;

import de.sgollmer.solvismax.Constants;
import de.sgollmer.solvismax.connection.IAccountInfo;
import de.sgollmer.solvismax.crypt.CryptAes;
import de.sgollmer.solvismax.error.CryptDefaultValueException;
import de.sgollmer.solvismax.error.CryptExeception;
import de.sgollmer.solvismax.log.LogManager;
import de.sgollmer.solvismax.log.LogManager.DelayedMessage;
import de.sgollmer.solvismax.log.LogManager.Level;
import de.sgollmer.xmllibrary.ArrayXml;
import de.sgollmer.xmllibrary.BaseCreator;
import de.sgollmer.xmllibrary.CreatorByXML;
import de.sgollmer.xmllibrary.StringElement;
import de.sgollmer.xmllibrary.XmlException;

public class Units {

	private static final String XML_UNITS_UNIT = "Unit";
	private static final String XML_FEATURES = "Features";
	private static final String XML_IGNORED_CHANNELS = "IgnoredChannels";
	private static final String XML_REG_EX = "RegEx";

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
		private final int defaultMeasurementsInterval_ms;
		private final int defaultMeasurementsIntervalFast_ms;
		private final int forceUpdateAfterFastChangingIntervals;
		private final int forcedUpdateInterval_ms;
		private final int doubleUpdateInterval_ms;
		private final int bufferedInterval_ms;
		private final int watchDogTime_ms;
		private final int releaseBlockingAfterUserAccess_ms;
		private final int releaseBlockingAfterServiceAccess_ms;
		private final boolean delayAfterSwitchingOnEnable;
		private final boolean fwLth2_21_02A;
		private final Features features;
		private final int ignoredFrameThicknesScreenSaver;
		private final Collection<Pattern> ignoredChannels;

		private Unit(String id, String type, String url, String account, CryptAes password, int configOrMask,
				int defaultAverageCount, int measurementHysteresisFactor, int defaultMeasurementsInterval_ms,
				int defaultMeasurementsIntervalFast_ms, int forceUpdateAfterFastChangingIntervals,
				int forcedUpdateInterval_ms, int doubleUpdateInterval_ms, int bufferedInterval_ms, int watchDogTime_ms,
				int releaseBlockingAfterUserAccess_ms, int releaseBlockingAfterServiceAccess_ms,
				boolean delayAfterSwitchingOn, boolean fwLth2_21_02A, Features features,
				int ignoredFrameThicknesScreenSaver, Collection<Pattern> ignoredChannels) {
			this.id = id;
			this.type = type;
			this.url = url;
			this.account = account;
			this.password = password;
			this.configOrMask = configOrMask;
			this.defaultAverageCount = defaultAverageCount;
			this.measurementHysteresisFactor = measurementHysteresisFactor;
			this.defaultMeasurementsInterval_ms = defaultMeasurementsInterval_ms;
			this.defaultMeasurementsIntervalFast_ms = defaultMeasurementsIntervalFast_ms;
			this.forceUpdateAfterFastChangingIntervals = forceUpdateAfterFastChangingIntervals;
			this.forcedUpdateInterval_ms = forcedUpdateInterval_ms;
			this.doubleUpdateInterval_ms = doubleUpdateInterval_ms;
			this.bufferedInterval_ms = bufferedInterval_ms;
			this.watchDogTime_ms = watchDogTime_ms;
			this.releaseBlockingAfterUserAccess_ms = releaseBlockingAfterUserAccess_ms;
			this.releaseBlockingAfterServiceAccess_ms = releaseBlockingAfterServiceAccess_ms;
			this.delayAfterSwitchingOnEnable = delayAfterSwitchingOn;
			this.fwLth2_21_02A = fwLth2_21_02A;
			this.features = features;
			this.ignoredFrameThicknesScreenSaver = ignoredFrameThicknesScreenSaver;
			this.ignoredChannels = ignoredChannels;
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
			private Integer defaultMeasurementsInterval_ms = null;
			private Integer defaultMeasurementsIntervalFast_ms = null;
			private int forceUpdateAfterFastChangingIntervals = Constants.FORCE_UPDATE_AFTER_N_INTERVALS;
			private int forcedUpdateInterval_ms;
			private int doubleUpdateInterval_ms = 0;
			private int bufferedInterval_ms;
			private int watchDogTime_ms;
			private int releaseBlockingAfterUserAccess_ms;
			private int releaseBlockingAfterServiceAccess_ms;
			private boolean delayAfterSwitchingOnEnable = false;
			private boolean fwLth2_21_02A = false;
			private Features features;
			private int ignoredFrameThicknesScreenSaver;
			private final Collection<Pattern> ignoredChannels = new ArrayList<>();

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
							this.defaultMeasurementsInterval_ms = Integer.parseInt(value);
							break;
						case "measurementsInterval_s":
							this.defaultMeasurementsInterval_ms = Integer.parseInt(value) * 1000;
							break;
						case "measurementsIntervalFast_s":
							this.defaultMeasurementsIntervalFast_ms = Integer.parseInt(value) * 1000;
							break;
						case "forceUpdateInFastChangingAfterIntervals":
							this.forceUpdateAfterFastChangingIntervals = Integer.parseInt(value);
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

				if (this.defaultMeasurementsInterval_ms == null) {
					throw new XmlException("<defaultMeasurementsInterval_s> is missing in base.xml");
				}

				if (this.defaultMeasurementsIntervalFast_ms == null) {
					this.defaultMeasurementsIntervalFast_ms = this.defaultMeasurementsInterval_ms;
				}

				return new Unit(this.id, this.type, this.url, this.account, this.password, this.configOrMask,
						this.defaultAverageCount, this.measurementHysteresisFactor, this.defaultMeasurementsInterval_ms,
						this.defaultMeasurementsIntervalFast_ms, this.forceUpdateAfterFastChangingIntervals,
						this.forcedUpdateInterval_ms, this.doubleUpdateInterval_ms, this.bufferedInterval_ms,
						this.watchDogTime_ms, this.releaseBlockingAfterUserAccess_ms,
						this.releaseBlockingAfterServiceAccess_ms, this.delayAfterSwitchingOnEnable, this.fwLth2_21_02A,
						this.features, this.ignoredFrameThicknesScreenSaver, this.ignoredChannels);

			}

			@Override
			public CreatorByXML<?> getCreator(QName name) {
				String id = name.getLocalPart();
				switch (id) {
					case XML_FEATURES:
						return new Features.Creator(id, getBaseCreator());
					case XML_IGNORED_CHANNELS:
						return new ArrayXml.Creator<StringElement>(id, getBaseCreator(), StringElement.getBaseElement(),
								XML_REG_EX);
				}
				return null;
			}

			@Override
			public void created(CreatorByXML<?> creator, Object created) throws XmlException {
				switch (creator.getId()) {
					case XML_FEATURES:
						this.features = (Features) created;
						break;
					case XML_IGNORED_CHANNELS:
						@SuppressWarnings("unchecked")
						ArrayXml<StringElement> arrayXml = (ArrayXml<StringElement>) created;
						for (StringElement ignoreChannel : arrayXml.getArray()) {
							try {
								Pattern exp = Pattern.compile(ignoreChannel.toString());
								this.ignoredChannels.add(exp);
							} catch (PatternSyntaxException e) {
								throw new XmlException("Regular expression error on expression: " + ignoreChannel);
							}

						}
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

		public int getMeasurementsInterval_ms() {
			return this.defaultMeasurementsInterval_ms;
		}

		public int getMeasurementsIntervalFast_ms() {
			return this.defaultMeasurementsIntervalFast_ms;
		}

		public int getForcedUpdateInterval_ms() {
			return this.forcedUpdateInterval_ms;
		}

		public int getBufferedInterval_ms() {
			return this.bufferedInterval_ms;
		}

		public boolean isBuffered() {
			return this.bufferedInterval_ms > 0;
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

		public boolean isChannelIgnored(String channelId) {
			for (Pattern regEx : this.ignoredChannels) {
				if (regEx.matcher(channelId).matches()) {
					return true;
				}
			}
			return false;
		}

		public int getForceUpdateAfterFastChangingIntervals() {
			return this.forceUpdateAfterFastChangingIntervals;
		}

	}
}
