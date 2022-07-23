package de.sgollmer.solvismax.model.objects.unit;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

import javax.xml.namespace.QName;

import de.sgollmer.solvismax.Constants;
import de.sgollmer.solvismax.connection.IAccountInfo;
import de.sgollmer.solvismax.crypt.CryptAes;
import de.sgollmer.solvismax.error.CryptException;
import de.sgollmer.solvismax.log.LogManager;
import de.sgollmer.solvismax.log.LogManager.ILogger;
import de.sgollmer.solvismax.log.LogManager.Level;
import de.sgollmer.solvismax.model.objects.AllDurations;
import de.sgollmer.solvismax.model.objects.ChannelAssignment;
import de.sgollmer.solvismax.model.objects.Duration;
import de.sgollmer.xmllibrary.ArrayXml;
import de.sgollmer.xmllibrary.BaseCreator;
import de.sgollmer.xmllibrary.CreatorByXML;
import de.sgollmer.xmllibrary.StringElement;
import de.sgollmer.xmllibrary.XmlException;

public class Unit implements IAccountInfo {

	private static final ILogger logger = LogManager.getInstance().getLogger(Unit.class);

	private static final String XML_FEATURES = "Features";
	private static final String XML_URLS = "Urls";
	private static final String XML_URL = "Url";
	private static final String XML_IGNORED_CHANNELS = "IgnoredChannels";
	private static final String XML_REG_EX = "RegEx";
	private static final String XML_CHANNEL_ASSIGNMENTS = "ChannelAssignments";
	private static final String XML_CHANNEL_ASSIGNMENT = "Assignment";
	private static final String XML_DURATIONS = "Durations";
	private static final String XML_CHANNEL_OPTIONS = "ChannelOptions";

	private final String id;
	private final boolean admin;
	private final Configuration configuration;
	private final Collection<String> urls;
	private final String url;
	private final String account;
	private final CryptAes password;
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
	private final int reheatingNotRequiredActiveTime_ms;
	private final int resetErrorDelayTime_ms;
	private final boolean delayAfterSwitchingOnEnable;
	private final boolean fwLth2_21_02A;
	private final Features features;
	private final int ignoredFrameThicknesScreenSaver;
	private final Collection<Pattern> ignoredChannels;
	private final Map<String, ChannelAssignment> assignments;
	private Long forcedConfigMask = null;
	private final boolean csvUnit;
	private final AllDurations durations;
	private final AllChannelOptions channelOptions;

	private Unit(final String id, final Configuration configuration, final Collection<String> urls, final String url,
			final String account, final CryptAes password, final int defaultAverageCount,
			final int measurementHysteresisFactor, final int defaultMeasurementsInterval_ms,
			final int defaultMeasurementsIntervalFast_ms, final int forceUpdateAfterFastChangingIntervals,
			final int forcedUpdateInterval_ms, final int doubleUpdateInterval_ms, final int bufferedInterval_ms,
			final int watchDogTime_ms, final int releaseBlockingAfterUserAccess_ms,
			final int releaseBlockingAfterServiceAccess_ms, final int clearNotRequiredTime_ms,
			final int resetErrorDelayTime_ms, final boolean delayAfterSwitchingOn, final boolean fwLth2_21_02A,
			final Features features, final int ignoredFrameThicknesScreenSaver,
			final Collection<Pattern> ignoredChannels, final Map<String, ChannelAssignment> assignments,
			final boolean csvUnit, final AllDurations durations, final AllChannelOptions channelOptions) {
		this.id = id;
		this.configuration = configuration;
		this.url = url;
		this.urls = urls;
		this.account = account;
		this.password = password;
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
		this.resetErrorDelayTime_ms = resetErrorDelayTime_ms;
		this.reheatingNotRequiredActiveTime_ms = clearNotRequiredTime_ms;
		this.delayAfterSwitchingOnEnable = delayAfterSwitchingOn;
		this.fwLth2_21_02A = fwLth2_21_02A;
		this.features = features;
		this.ignoredFrameThicknesScreenSaver = ignoredFrameThicknesScreenSaver;
		this.ignoredChannels = ignoredChannels;
		this.assignments = assignments;
		this.csvUnit = csvUnit;
		this.durations = durations;
		this.channelOptions = channelOptions;
		this.admin = features.isAdmin();
		;
	}

	public String getId() {
		return this.id;
	}

	public Configuration getConfiguration() {
		return this.configuration;
	}

	public Collection<String> getUrls() {
		return this.urls;
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

	static class Creator extends CreatorByXML<Unit> {

		private String id;
		private Configuration configuration;
		private final Configuration.Creator configurationCreator;
		private String url;
		private Collection<String> urls;
		private String account;
		private CryptAes password = new CryptAes();
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
		private int resetErrorDelayTime_ms = 0;
		private int reheatingNotRequiredActiveTime_ms = Constants.Defaults.REHEATING_NOT_REQUIRED_ACTIVE_TIME;
		private boolean delayAfterSwitchingOnEnable = false;
		private boolean fwLth2_21_02A = false;
		private Features features;
		private int ignoredFrameThicknesScreenSaver;
		private final Collection<Pattern> ignoredChannels = new ArrayList<>();
		private Map<String, ChannelAssignment> assignments = null;
		private boolean csvUnit = false;
		private AllDurations durations = null;
		private AllChannelOptions channelOptions = null;

		Creator(final String id, final BaseCreator<?> creator) {
			super(id, creator);
			this.configurationCreator = new Configuration.Creator(null, creator);
		}

		@Override
		public void setAttribute(final QName name, final String value) throws XmlException {
			try {
				switch (name.getLocalPart()) {
					case "id":
						this.id = value;
						break;
					case "url":
						this.url = value;
						break;
					case "account":
						this.account = value;
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
					case "resetErrorDelayTime_ms":
						this.resetErrorDelayTime_ms = Integer.parseInt(value);
						break;
					case "reheatingNotRequiredActiveTime_ms":
						this.reheatingNotRequiredActiveTime_ms = Integer.parseInt(value);
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
					case "csvUnit":
						this.csvUnit = Boolean.parseBoolean(value);
						break;
				}
			} catch (CryptException e) {
				String m = "base.xml error of passwordCrypt in Unit tag: " + e.getMessage();
				logger.log(Level.ERROR, m, null, Constants.ExitCodes.CRYPTION_FAIL);
			}
			this.configurationCreator.setAttribute(name, value);

		}

		@Override
		public Unit create() throws XmlException, IOException {

			if (this.defaultMeasurementsInterval_ms == null) {
				throw new XmlException("<defaultMeasurementsInterval_s> is missing in base.xml");
			}

			if (this.defaultMeasurementsIntervalFast_ms == null) {
				this.defaultMeasurementsIntervalFast_ms = this.defaultMeasurementsInterval_ms;
			}

			this.configuration = this.configurationCreator.create();

			return new Unit(this.id, this.configuration, this.urls, this.url, this.account, this.password,
					this.defaultAverageCount, this.measurementHysteresisFactor, this.defaultMeasurementsInterval_ms,
					this.defaultMeasurementsIntervalFast_ms, this.forceUpdateAfterFastChangingIntervals,
					this.forcedUpdateInterval_ms, this.doubleUpdateInterval_ms, this.bufferedInterval_ms,
					this.watchDogTime_ms, this.releaseBlockingAfterUserAccess_ms,
					this.releaseBlockingAfterServiceAccess_ms, this.reheatingNotRequiredActiveTime_ms,
					this.resetErrorDelayTime_ms, this.delayAfterSwitchingOnEnable, this.fwLth2_21_02A, this.features,
					this.ignoredFrameThicknesScreenSaver, this.ignoredChannels, this.assignments, this.csvUnit,
					this.durations, this.channelOptions);

		}

		@Override
		public CreatorByXML<?> getCreator(final QName name) {
			String id = name.getLocalPart();
			switch (id) {
				case XML_FEATURES:
					return new Features.Creator(id, getBaseCreator());
				case XML_IGNORED_CHANNELS:
					return new ArrayXml.Creator<String, StringElement>(id, getBaseCreator(),
							StringElement.getBaseElement(), XML_REG_EX);
				case XML_CHANNEL_ASSIGNMENTS:
					return new AssignmentsCreator(id, this.getBaseCreator());
				case XML_DURATIONS:
					return new AllDurations.Creator(id, this.getBaseCreator());
				case XML_CHANNEL_OPTIONS:
					return new AllChannelOptions.Creator(id, this.getBaseCreator());
				case XML_URLS:
					return new ArrayXml.Creator<String, StringElement>(id, this.getBaseCreator(),
							StringElement.getBaseElement(), XML_URL);
			}
			return this.configurationCreator.getCreator(name);
		}

		@Override
		public void created(final CreatorByXML<?> creator, final Object created) throws XmlException {
			switch (creator.getId()) {
				case XML_FEATURES:
					this.features = (Features) created;
					break;
				case XML_IGNORED_CHANNELS:
					@SuppressWarnings("unchecked")
					ArrayXml<String, StringElement> arrayXml = (ArrayXml<String, StringElement>) created;
					for (String ignoreChannel : arrayXml.getArray()) {
						try {
							Pattern exp = Pattern.compile(ignoreChannel);
							this.ignoredChannels.add(exp);
						} catch (PatternSyntaxException e) {
							throw new XmlException("Regular expression error on expression: " + ignoreChannel);
						}

					}
					break;
				case XML_CHANNEL_ASSIGNMENTS:
					@SuppressWarnings("unchecked")
					Map<String, ChannelAssignment> created2 = (Map<String, ChannelAssignment>) created;
					this.assignments = created2;
					break;
				case XML_DURATIONS:
					this.durations = (AllDurations) created;
					break;
				case XML_CHANNEL_OPTIONS:
					this.channelOptions = (AllChannelOptions) created;
					break;
				case XML_URLS:
					@SuppressWarnings("unchecked")
					ArrayXml<String, StringElement> arrayXml1 = (ArrayXml<String, StringElement>) created;
					this.urls = arrayXml1.getArray();
					break;
			}
			this.configurationCreator.created(creator, created);
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

	public int getReleaseBlockingAfterUserAccess_ms() {
		return this.releaseBlockingAfterUserAccess_ms;
	}

	public int getReleaseBlockingAfterServiceAccess_ms() {
		return this.releaseBlockingAfterServiceAccess_ms;
	}

	public int getReheatingNotRequiredActiveTime_ms() {
		return this.reheatingNotRequiredActiveTime_ms;
	}

	public boolean isChannelIgnored(final String channelId) {
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

	public static class AssignmentsCreator extends CreatorByXML<Map<String, ChannelAssignment>> {

		private Map<String, ChannelAssignment> assignments = null;

		public AssignmentsCreator(final String id, final BaseCreator<?> creator) {
			super(id, creator);
		}

		@Override
		public void setAttribute(final QName name, final String value) throws XmlException {

		}

		@Override
		public Map<String, ChannelAssignment> create() throws XmlException, IOException {
			return this.assignments;
		}

		@Override
		public CreatorByXML<?> getCreator(final QName name) {
			String id = name.getLocalPart();
			switch (id) {
				case XML_CHANNEL_ASSIGNMENT:
					return new ChannelAssignment.Creator(id, this.getBaseCreator());
			}
			return null;
		}

		@Override
		public void created(final CreatorByXML<?> creator, final Object created) throws XmlException {
			switch (creator.getId()) {
				case XML_CHANNEL_ASSIGNMENT:
					if (this.assignments == null) {
						this.assignments = new HashMap<>();
					}
					ChannelAssignment assignment = (ChannelAssignment) created;
					ChannelAssignment former = this.assignments.put(assignment.getName(), assignment);
					if (former != null) {
						throw new XmlException("base.xml error, <" + assignment.getName() + "> isn't unique.");
					}
			}
		}

	}

	public ChannelAssignment getChannelAssignment(final String id) {
		if (this.assignments == null) {
			return null;
		} else {
			return this.assignments.get(id);
		}
	}

	public boolean isCsvUnit() {
		return this.csvUnit;
	}

	public boolean isAdmin() {
		return this.admin;
	}

	public Long getForcedConfigMask() {
		return this.forcedConfigMask;
	}

	public void setForcedConfigMask(final Long forcedConfigMask) {
		this.forcedConfigMask = forcedConfigMask;
	}

	public String getComment() {
		return this.configuration.getComment();
	}

	public Duration getDuration(final String id) {
		if (this.durations == null) {
			return null;
		} else {
			return this.durations.get(id);
		}
	}

	public AllChannelOptions getChannelOptions() {
		return this.channelOptions;
	}

	public boolean isMailEnabled() {
		return this.features.isSendMailOnError();
	}

	public int getResetErrorDelayTime() {
		return this.resetErrorDelayTime_ms;
	}

}