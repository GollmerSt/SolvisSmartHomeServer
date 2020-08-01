/************************************************************************
 * 
 * $Id$
 *
 * 
 ************************************************************************/

package de.sgollmer.solvismax.model.objects.configuration;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;

import javax.xml.namespace.QName;

import de.sgollmer.solvismax.error.XmlException;
import de.sgollmer.solvismax.xml.BaseCreator;
import de.sgollmer.solvismax.xml.CreatorByXML;

public class ConfigurationMasks {

	private static final String XML_CONFIGURATION_MASK = "ConfigurationMask";

	private final Collection<ConfigurationMask> masks;

	private ConfigurationMasks(Collection<ConfigurationMask> masks) {
		this.masks = masks;
	}

	public boolean isInConfiguration(int configurationMask) {
		for (ConfigurationMask mask : this.masks) {
			if (mask.isInConfiguration(configurationMask)) {
				return true;
			}
		}
		return false;
	}

	public boolean isVerified(ConfigurationMasks masks) {
		for (ConfigurationMask maskO : this.masks) {
			for (ConfigurationMask maskI : masks.masks) {
				if (!maskO.isVerified(maskI)) {
					return false;
				}
			}
		}
		return true;
	}

	public static class Creator extends CreatorByXML<ConfigurationMasks> {

		private final Collection<ConfigurationMask> masks = new ArrayList<>();

		public Creator(String id, BaseCreator<?> creator) {
			super(id, creator);
		}

		@Override
		public void setAttribute(QName name, String value) {
		}

		@Override
		public ConfigurationMasks create() throws XmlException, IOException {
			return new ConfigurationMasks(this.masks);
		}

		@Override
		public CreatorByXML<?> getCreator(QName name) {
			String id = name.getLocalPart();
			switch (id) {
				case XML_CONFIGURATION_MASK:
					return new ConfigurationMask.Creator(id, getBaseCreator());
			}
			return null;
		}

		@Override
		public void created(CreatorByXML<?> creator, Object created) {
			switch (creator.getId()) {
				case XML_CONFIGURATION_MASK:
					this.masks.add((ConfigurationMask) created);
					break;
			}
		}

	}

	private static class ConfigurationMask {
		private final int andMask;
		private final int cmpMask;

		private ConfigurationMask(int andMask, int cmpMask) {
			this.andMask = andMask;
			this.cmpMask = cmpMask;
		}

		private boolean isInConfiguration(int configurationMask) {
			return this.cmpMask == (configurationMask & this.andMask);
		}

		private boolean isVerified(ConfigurationMask mask) {
			int andMask = this.andMask & mask.andMask;
			return 0 != ((this.cmpMask ^ mask.cmpMask) & andMask);
		}

		@Override
		public String toString() {
			return "And-Mask: " + Integer.toString(this.andMask, 16) + ", Cmp-Mask: "
					+ Integer.toString(this.cmpMask, 16);
		}

		private static class Creator extends CreatorByXML<ConfigurationMask> {

			private int andMask;
			private int cmpMask;

			private Creator(String id, BaseCreator<?> creator) {
				super(id, creator);
			}

			@Override
			public void setAttribute(QName name, String value) {
				switch (name.getLocalPart()) {
					case "andMask":
						this.andMask = Integer.decode(value);
						break;
					case "compareMask":
						this.cmpMask = Integer.decode(value);
						break;
				}

			}

			@Override
			public ConfigurationMask create() throws XmlException, IOException {
				return new ConfigurationMask(this.andMask, this.cmpMask);
			}

			@Override
			public CreatorByXML<?> getCreator(QName name) {
				return null;
			}

			@Override
			public void created(CreatorByXML<?> creator, Object created) {

			}
		}
	}
}
