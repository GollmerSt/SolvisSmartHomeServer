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

	private static final String XML_CLOCK_TUNING = "ClockTuning";
	private static final String XML_EQUIPMENT_TIME_SYNC = "EquipmentTimeSynchronisation";
	private static final String XML_UPDATE_AFTER_USER_ACCESS = "UpdateAfterUserAccess";
	private static final String XML_DETECT_SERVICE_ACCESS = "DetectServiceAccess";
	private static final String XMl_POWEROFF_IS_SERVICE_ACCESS = "PowerOffIsServiceAccess";
	private static final String XMl_SEND_MAIL_ON_ERROR = "SendMailOnError";
	private static final String XMl_SEND_MAIL_ON_ERRORS_CLEARED = "SendMailOnErrorsCleared";
	private static final String XMl_CLEAR_ERROR_MESSAGE_AFTER_MAIL = "ClearErrorMessageAfterMail";
	private static final String XML_ONLY_MEASUREMENT = "OnlyMeasurements";
	private static final String XML_INTERACTIVE_GUI_ACCESS = "InteractiveGUIAccess";
	public static final String XML_ADMIN = "Admin";
	private static final String XML_FEATURE = "Feature";

	private final Map<String, Boolean> features;
	private Boolean interactiveGUIAccess = null;

	private Features(final Map<String, Boolean> features) throws XmlException {
		this.features = features;
		this.checkInteractiveGUIAccess();
	}

	private boolean get(final String feature, final boolean missingValue) {
		Boolean result = this.features.get(feature);
		if (result == null) {
			return missingValue;
		} else {
			return result;
		}
	}

	private boolean get(final String feature) {
		return this.get(feature, false);
	}

	public boolean isClockTuning() {
		return this.get(XML_CLOCK_TUNING);
	}

	public boolean isEquipmentTimeSynchronisation() {
		return this.get(XML_EQUIPMENT_TIME_SYNC);
	}

	public boolean isUpdateAfterUserAccess() {
		return this.get(XML_UPDATE_AFTER_USER_ACCESS);
	}

	public boolean isDetectServiceAccess() {
		return this.get(XML_DETECT_SERVICE_ACCESS);
	}

	private boolean checkInteractiveGUIAccess() throws XmlException {
		Boolean interactiveGUIAccess = this.getMap().get(XML_INTERACTIVE_GUI_ACCESS);
		Boolean onlyMeasurements = this.getMap().get(XML_ONLY_MEASUREMENT);
		if (interactiveGUIAccess == null && onlyMeasurements == null) {
			throw new XmlException("Exactly one of the feature \"" + XML_INTERACTIVE_GUI_ACCESS + "\" or \""
					+ XML_ONLY_MEASUREMENT + "\" is missing.");
		} else if (interactiveGUIAccess != null && onlyMeasurements != null) {
			throw new XmlException("The feature \"" + XML_INTERACTIVE_GUI_ACCESS + "\" AND \"" + XML_ONLY_MEASUREMENT
					+ "\" are defined. Only one is possible.");
		}
		return interactiveGUIAccess != null && interactiveGUIAccess || onlyMeasurements != null && !onlyMeasurements;
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

	public boolean isClearErrorMessageAfterMail() {
		return this.get(XMl_CLEAR_ERROR_MESSAGE_AFTER_MAIL);
	}

	public boolean isPowerOffIsServiceAccess() {
		return this.get(XMl_POWEROFF_IS_SERVICE_ACCESS);
	}

	public boolean isSendMailOnError() {
		return this.get(XMl_SEND_MAIL_ON_ERROR);
	}

	public boolean isSendMailOnErrorsCleared() {
		return this.get(XMl_SEND_MAIL_ON_ERROR) && this.get(XMl_SEND_MAIL_ON_ERRORS_CLEARED, true);
	}

	public boolean isAdmin() {
		return this.get(XML_ADMIN);
	}

	public Boolean getFeature(final String id) {
		return this.get(id);
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
			switch (id) {
				case XML_CLOCK_TUNING:
				case XML_EQUIPMENT_TIME_SYNC:
				case XML_UPDATE_AFTER_USER_ACCESS:
				case XML_DETECT_SERVICE_ACCESS:
				case XMl_POWEROFF_IS_SERVICE_ACCESS:
				case XMl_SEND_MAIL_ON_ERROR:
				case XMl_CLEAR_ERROR_MESSAGE_AFTER_MAIL:
				case XML_ONLY_MEASUREMENT:
				case XML_ADMIN:
					return new StringElement.Creator(id, this.getBaseCreator());
				case XML_FEATURE:
					return new Feature.Creator(id, this.getBaseCreator());
			}
			return null;
		}

		@Override
		public void created(final CreatorByXML<?> creator, final Object created) {
			if (created instanceof StringElement) {
				boolean bool = Boolean.parseBoolean(((StringElement) created).toString());
				String id = creator.getId();
				switch (id) {
					case XML_CLOCK_TUNING:
					case XML_EQUIPMENT_TIME_SYNC:
					case XML_UPDATE_AFTER_USER_ACCESS:
					case XML_DETECT_SERVICE_ACCESS:
					case XMl_POWEROFF_IS_SERVICE_ACCESS:
					case XMl_SEND_MAIL_ON_ERROR:
					case XMl_CLEAR_ERROR_MESSAGE_AFTER_MAIL:
					case XML_ONLY_MEASUREMENT:
					case XML_ADMIN:
						this.features.put(id, bool);
						break;
				}
			} else {
				switch (creator.getId()) {
					case XML_FEATURE:
						Feature feature = (Feature) created;
						this.features.put(feature.id, feature.value);
						break;
					default:
						System.err.println("Check <" + creator.getId() + "> in the base.xml file, wrong value format ");
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
			logger.error(Features.XMl_SEND_MAIL_ON_ERROR + ": " + Boolean.toString(this.isSendMailOnError()));
			logger.error(Features.XMl_SEND_MAIL_ON_ERRORS_CLEARED + ": "
					+ Boolean.toString(this.isSendMailOnErrorsCleared()));
		}

	}

}