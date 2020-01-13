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
import de.sgollmer.solvismax.xml.BaseCreator;
import de.sgollmer.solvismax.xml.CreatorByXML;

public class Units {

	private static final String XML_UNITS_UNIT = "Unit";

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

		private final String id;
		private final String type;
		private final String url;
		private final String account;
		private final String password;
		private final int defaultAverageCount;
		private final int defaultReadMeasurementsIntervall_ms;
		private final int forcedUpdateIntervall_ms;
		private final int bufferedIntervall_ms;
		private final boolean delayAfterSwitchingOnEnable;

		public Unit(String id, String type, String url, String account, String password, int defaultAverageCount,
				int defaultReadMeasurementsIntervall_ms, int forcedUpdateIntervall_ms, int bufferedIntervall_ms,
				boolean delayAfterSwitchingOn) {
			this.id = id;
			this.type = type;
			this.url = url;
			this.account = account;
			this.password = password;
			this.defaultAverageCount = defaultAverageCount;
			this.defaultReadMeasurementsIntervall_ms = defaultReadMeasurementsIntervall_ms;
			this.forcedUpdateIntervall_ms = forcedUpdateIntervall_ms;
			this.bufferedIntervall_ms = bufferedIntervall_ms;
			this.delayAfterSwitchingOnEnable = delayAfterSwitchingOn;
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

		public static class Creator extends CreatorByXML<Unit> {

			private String id;
			private String type;
			private String url;
			private String account;
			private String password;
			private int defaultAverageCount;
			private int defaultReadMeasurementsIntervall_ms;
			private int forcedUpdateIntervall_ms;
			private int bufferedIntervall_ms;
			private boolean delayAfterSwitchingOnEnable = false;

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
					case "defaultReadMeasurementsIntervall_ms":
						this.defaultReadMeasurementsIntervall_ms = Integer.parseInt(value);
						break;
					case "forcedUpdateIntervall_ms":
						this.forcedUpdateIntervall_ms = Integer.parseInt(value);
						break;
					case "bufferedIntervall_ms":
						this.bufferedIntervall_ms = Integer.parseInt(value);
						break;
					case "delayAfterSwitchingOnEnable":
						this.delayAfterSwitchingOnEnable = Boolean.parseBoolean(value);
						break;
				}

			}

			@Override
			public Unit create() throws XmlError, IOException {
				return new Unit(id, type, url, account, password, defaultAverageCount,
						defaultReadMeasurementsIntervall_ms, forcedUpdateIntervall_ms, bufferedIntervall_ms,
						delayAfterSwitchingOnEnable);
			}

			@Override
			public CreatorByXML<?> getCreator(QName name) {
				return null;
			}

			@Override
			public void created(CreatorByXML<?> creator, Object created) {
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

		public int getDefaultReadMeasurementsIntervall_ms() {
			return defaultReadMeasurementsIntervall_ms;
		}

		public int getForcedUpdateIntervall_ms() {
			return forcedUpdateIntervall_ms;
		}

		public int getBufferedIntervall_ms() {
			return bufferedIntervall_ms;
		}

		public boolean isDelayAfterSwitchingOnEnable() {
			return delayAfterSwitchingOnEnable;
		}

	}
}
