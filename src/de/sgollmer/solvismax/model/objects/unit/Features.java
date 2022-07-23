package de.sgollmer.solvismax.model.objects.unit;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import javax.xml.namespace.QName;

import de.sgollmer.solvismax.log.LogManager;
import de.sgollmer.solvismax.log.LogManager.ILogger;
import de.sgollmer.xmllibrary.BaseCreator;
import de.sgollmer.xmllibrary.CreatorByXML;
import de.sgollmer.xmllibrary.StringElement;
import de.sgollmer.xmllibrary.XmlException;

public class Features {

	private static final ILogger logger = LogManager.getInstance().getLogger(Features.class);

	private enum FeatureSetting {
		XML_CLOCK_TUNING("ClockTuning", false), //
		XML_EQUIPMENT_TIME_SYNC("EquipmentTimeSynchronisation", false), //
		XML_UPDATE_AFTER_USER_ACCESS("UpdateAfterUserAccess", false), //
		XML_DETECT_SERVICE_ACCESS("DetectServiceAccess", false), //
		XML_END_OF_USER_BY_SCREEN_SAVER("EndOfUserInterventionDetectionThroughScreenSaver", false), //
		XMl_POWEROFF_IS_SERVICE_ACCESS("PowerOffIsServiceAccess", false), //
		XMl_SEND_MAIL_ON_ERROR("SendMailOnError", false), //
		XMl_SEND_MAIL_ON_ERRORS_CLEARED("SendMailOnErrorsCleared", false), //
		XMl_CLEAR_ERROR_MESSAGE_AFTER_MAIL("ClearErrorMessageAfterMail", true), //
		XML_ONLY_MEASUREMENT("OnlyMeasurements", true), //
		XML_INTERACTIVE_GUI_ACCESS("InteractiveGUIAccess", false), //
		XML_ADMIN("Admin", false), //
		//
		XML_FEATURE("Feature");

		private final String id;
		private final Boolean missingValue; // null if Feature

		private FeatureSetting(final String id, final Boolean missingValue) {
			this.id = id;
			this.missingValue = missingValue;
		}

		private FeatureSetting(final String id) {
			this(id, null);
		}

		public String getId() {
			return this.id;
		}

		public boolean getMissingValue() {
			return this.missingValue;
		}

		public static FeatureSetting get(final String id) {
			for (FeatureSetting setting : FeatureSetting.values()) {
				if (setting.id.equals(id)) {
					return setting;
				}
			}
			return null;
		}

		public boolean isFeature() {
			return this.missingValue == null;
		}

		@Override
		public String toString() {
			return this.id;
		}
	}

	public static String getAdminKey() {
		return FeatureSetting.XML_ADMIN.getId();
	}

	private final Map<String, Boolean> features;
	private Boolean interactiveGUIAccess = null;

	private Features(final Map<String, Boolean> features) throws XmlException {
		this.features = features;
		this.checkInteractiveGUIAccess();
	}

	public boolean get(String feature, boolean missingValue) {
		Boolean result = this.features.get(feature);
		if (result == null) {
			return missingValue;
		} else {
			return result;
		}
	}

	public boolean get(final FeatureSetting setting) {
		return this.get(setting.getId(), setting.getMissingValue());
	}

	public boolean isClockTuning() {
		return this.get(FeatureSetting.XML_CLOCK_TUNING);
	}

	public boolean isEquipmentTimeSynchronisation() {
		return this.get(FeatureSetting.XML_EQUIPMENT_TIME_SYNC);
	}

	public boolean isUpdateAfterUserAccess() {
		return this.get(FeatureSetting.XML_UPDATE_AFTER_USER_ACCESS);
	}

	public boolean isDetectServiceAccess() {
		return this.get(FeatureSetting.XML_DETECT_SERVICE_ACCESS);
	}

	public boolean isClearErrorMessageAfterMail() {
		return this.get(FeatureSetting.XMl_CLEAR_ERROR_MESSAGE_AFTER_MAIL);
	}

	public boolean isPowerOffIsServiceAccess() {
		return this.get(FeatureSetting.XMl_POWEROFF_IS_SERVICE_ACCESS);
	}

	public boolean isSendMailOnError() {
		return this.get(FeatureSetting.XMl_SEND_MAIL_ON_ERROR);
	}

	public boolean isSendMailOnErrorsCleared() {
		return this.isSendMailOnError() && this.get(FeatureSetting.XMl_SEND_MAIL_ON_ERRORS_CLEARED);
	}

	public boolean isEndOfUserByScreenSaver() {
		return this.get(FeatureSetting.XML_END_OF_USER_BY_SCREEN_SAVER);
	}

	public boolean isAdmin() {
		return this.get(FeatureSetting.XML_ADMIN);
	}

	private boolean checkInteractiveGUIAccess() throws XmlException {
		boolean guiAccessValid = this.getMap().containsKey(FeatureSetting.XML_INTERACTIVE_GUI_ACCESS.getId());
		boolean onlyMeasurementsValid = this.getMap().containsKey(FeatureSetting.XML_ONLY_MEASUREMENT.getId());
		if (guiAccessValid == onlyMeasurementsValid) {
			throw new XmlException("Exactly one and only one of the feature \""
					+ FeatureSetting.XML_INTERACTIVE_GUI_ACCESS + "\" or \"" + FeatureSetting.XML_ONLY_MEASUREMENT
					+ "\" must be defined. Gui acces will be inhibited.");
		}
		return this.get(FeatureSetting.XML_INTERACTIVE_GUI_ACCESS) || !this.get(FeatureSetting.XML_ONLY_MEASUREMENT);
	}

	public boolean isInteractiveGUIAccess() {
		if (this.interactiveGUIAccess == null) {
			try {
				this.interactiveGUIAccess = this.checkInteractiveGUIAccess();
			} catch (XmlException e) {
				this.interactiveGUIAccess = false;
			}
		}
		return this.interactiveGUIAccess;
	}

	public Boolean getFeature(final String id) {
		return this.get(id, false);
	}

	static class Creator extends CreatorByXML<Features> {

		private final Map<String, Boolean> features = new HashMap<>();

		Creator(final String id, final BaseCreator<?> creator) {
			super(id, creator);
		}

		@Override
		public void setAttribute(final QName name, final String value) {
		}

		@Override
		public Features create() throws XmlException, IOException {
			return new Features(this.features);
		}

		@Override
		public CreatorByXML<?> getCreator(final QName name) {
			String id = name.getLocalPart();
			FeatureSetting setting = FeatureSetting.get(id);
			if (setting == null) {
				return null;
			} else if (setting.isFeature()) {
				return new Feature.Creator(id, this.getBaseCreator());
			} else {
				return new StringElement.Creator(id, this.getBaseCreator());
			}
		}

		@Override
		public void created(final CreatorByXML<?> creator, final Object created) {

			FeatureSetting setting = FeatureSetting.get(creator.getId());

			if (setting != null) {
				if (setting.isFeature()) {
					Feature feature = (Feature) created;
					this.features.put(feature.getId(), feature.isSet());
				} else {
					this.features.put(setting.getId(), Boolean.parseBoolean(created.toString()));
				}
			}
		}

	}

	public Map<String, Boolean> getMap() {
		return this.features;
	}

	public void checkMail(final String unitId) {
		if (!this.isSendMailOnError()) {
			logger.error("Mail was sent, but the mail is not activated for the unit \"" + unitId
					+ "\". Attributes involved:");
			logger.error(FeatureSetting.XMl_SEND_MAIL_ON_ERROR + ": " + Boolean.toString(this.isSendMailOnError()));
			logger.error(FeatureSetting.XMl_SEND_MAIL_ON_ERRORS_CLEARED + ": "
					+ Boolean.toString(this.isSendMailOnErrorsCleared()));
		}

	}

}