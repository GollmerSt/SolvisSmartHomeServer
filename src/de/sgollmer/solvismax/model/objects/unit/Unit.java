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
import de.sgollmer.solvismax.error.CryptDefaultValueException;
import de.sgollmer.solvismax.error.CryptExeception;
import de.sgollmer.solvismax.log.LogManager;
import de.sgollmer.solvismax.log.LogManager.DelayedMessage;
import de.sgollmer.solvismax.log.LogManager.Level;
import de.sgollmer.solvismax.model.objects.ChannelAssignment;
import de.sgollmer.solvismax.model.update.Correction;
import de.sgollmer.xmllibrary.ArrayXml;
import de.sgollmer.xmllibrary.BaseCreator;
import de.sgollmer.xmllibrary.CreatorByXML;
import de.sgollmer.xmllibrary.StringElement;
import de.sgollmer.xmllibrary.XmlException;

public class Unit implements IAccountInfo {

	private static final String XML_FEATURES = "Features";
	private static final String XML_IGNORED_CHANNELS = "IgnoredChannels";
	private static final String XML_DEFAULT_CORRECTIONS = "DefaultCorrections";
	private static final String XML_REG_EX = "RegEx";
	private static final String XML_CHANNEL_ASSIGNMENTS = "ChannelAssignments";
	private static final String XML_CHANNEL_ASSIGNMENT = "Assignment";

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
	private final DefaultCorrections defaultCorrections;
	private final Map<String, ChannelAssignment> assignments;

	private Unit(String id, String type, String url, String account, CryptAes password, int configOrMask,
			int defaultAverageCount, int measurementHysteresisFactor, int defaultMeasurementsInterval_ms,
			int defaultMeasurementsIntervalFast_ms, int forceUpdateAfterFastChangingIntervals,
			int forcedUpdateInterval_ms, int doubleUpdateInterval_ms, int bufferedInterval_ms, int watchDogTime_ms,
			int releaseBlockingAfterUserAccess_ms, int releaseBlockingAfterServiceAccess_ms,
			boolean delayAfterSwitchingOn, boolean fwLth2_21_02A, Features features,
			int ignoredFrameThicknesScreenSaver, Collection<Pattern> ignoredChannels,
			DefaultCorrections defaultCorrections, Map<String, ChannelAssignment> assignments) {
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
		this.defaultCorrections = defaultCorrections;
		this.assignments = assignments;
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

	static class Creator extends CreatorByXML<Unit> {

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
		private DefaultCorrections defaultCorrections = null;
		private Map<String, ChannelAssignment> assignments = null;

		Creator(String id, BaseCreator<?> creator) {
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
					this.features, this.ignoredFrameThicknesScreenSaver, this.ignoredChannels, this.defaultCorrections,
					this.assignments);

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
				case XML_DEFAULT_CORRECTIONS:
					return new DefaultCorrections.Creator(id, this.getBaseCreator());
				case XML_CHANNEL_ASSIGNMENTS:
					return new AssignmentsCreator(id, this.getBaseCreator());
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
				case XML_DEFAULT_CORRECTIONS:
					this.defaultCorrections = (DefaultCorrections) created;
					break;
				case XML_CHANNEL_ASSIGNMENTS:
					@SuppressWarnings("unchecked")
					Map<String, ChannelAssignment> created2 = (Map<String, ChannelAssignment>) created;
					this.assignments = created2;
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

	public void setDefaultCorrection(Correction correction) {
		if (this.defaultCorrections != null) {
			this.defaultCorrections.setCorrection(correction);
		}

	}

	public static class AssignmentsCreator extends CreatorByXML<Map<String, ChannelAssignment>> {

		private Map<String, ChannelAssignment> assignments = null;

		public AssignmentsCreator(String id, BaseCreator<?> creator) {
			super(id, creator);
		}

		@Override
		public void setAttribute(QName name, String value) throws XmlException {

		}

		@Override
		public Map<String, ChannelAssignment> create() throws XmlException, IOException {
			return this.assignments;
		}

		@Override
		public CreatorByXML<?> getCreator(QName name) {
			String id = name.getLocalPart();
			switch (id) {
				case XML_CHANNEL_ASSIGNMENT:
					return new ChannelAssignment.Creator(id, this.getBaseCreator());
			}
			return null;
		}

		@Override
		public void created(CreatorByXML<?> creator, Object created) throws XmlException {
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

	public ChannelAssignment getChannelAssignment(String id) {
		if (this.assignments == null) {
			return null;
		} else {
			return this.assignments.get(id);
		}
	}

}