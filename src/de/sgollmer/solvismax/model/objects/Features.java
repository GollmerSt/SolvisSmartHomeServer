package de.sgollmer.solvismax.model.objects;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import javax.xml.namespace.QName;

import de.sgollmer.solvismax.error.XmlException;
import de.sgollmer.solvismax.xml.BaseCreator;
import de.sgollmer.solvismax.xml.CreatorByXML;

public class Features {

	private static final String XML_CLOCK_TUNING = "ClockTuning";
	private static final String XML_EQUIPMENT_TIME_SYNC = "EquipmentTimeSynchronisation";
	private static final String XML_UPDATE_AFTER_USER_ACCESS = "UpdateAfterUserAccess";
	private static final String XML_DETECT_SERVICE_ACCESS = "DetectServiceAccess";
	private static final String XMl_POWEROFF_IS_SERVICE_ACCESS = "PowerOffIsServiceAccess";
	private static final String XMl_SEND_MAIL_ON_ERROR = "SendMailOnError";
	private static final String XMl_CLEAR_ERROR_MESSAGE_AFTER_MAIL = "ClearErrorMessageAfterMail";
	private static final String XML_ONLY_MEASUREMENT = "OnlyMeasurements";
	public static final String XML_ADMIN = "Admin";
	private static final String XML_FEATURE = "Feature";

	private final Map<String, Boolean> features;

	private Features(Map<String, Boolean> features) {
		this.features = features;
	}
	
	private boolean get( String feature ) {
		Boolean result = this.features.get(feature);
		if ( result == null ) {
			return false ;
		} else {
			return result;
		}
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

	public boolean isOnlyMeasurements() {
		return this.get(XML_ONLY_MEASUREMENT);
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

	public boolean isAdmin() {
		return this.get(XML_ADMIN);
	}

	public Boolean getFeature(String id) {
		return this.get(id);
	}

	static class Creator extends CreatorByXML<Features> {

		private final Map<String, Boolean> features = new HashMap<>();

		Creator(String id, BaseCreator<?> creator) {
			super(id, creator);
		}

		@Override
		public void setAttribute(QName name, String value) {
		}

		@Override
		public Features create() throws XmlException, IOException {
			return new Features(this.features);
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
				case XML_ADMIN:
					return new StringElement.Creator(id, this.getBaseCreator());
				case XML_FEATURE:
					return new Feature.Creator(id, this.getBaseCreator());
			}
			return null;
		}

		@Override
		public void created(CreatorByXML<?> creator, Object created) {
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
}