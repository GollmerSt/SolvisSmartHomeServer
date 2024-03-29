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

import de.sgollmer.solvismax.model.Solvis;
import de.sgollmer.solvismax.model.objects.unit.Feature;
import de.sgollmer.solvismax.model.objects.unit.Features;
import de.sgollmer.xmllibrary.BaseCreator;
import de.sgollmer.xmllibrary.CreatorByXML;
import de.sgollmer.xmllibrary.XmlException;

public class Configuration {

	private static final String XML_CONFIGURATION_MASK = "ConfigurationMask";
	private static final String XML_FEATURE = "Feature";

	public enum Admin {
		ADMIN, // Executed in admin mode
		VALUE, // Fix value can be defined in features, admin mode not necessary
		NONE;

		public boolean isAdmin(boolean init) {
			return this == ADMIN || !init && this == VALUE;
		}

	}

	private final Admin admin;
	private final Collection<ConfigurationMask> masks;
	private final Feature feature;

	private Configuration(Admin admin, final Collection<ConfigurationMask> masks, final Feature feature) {
		this.admin = admin;
		this.masks = masks;
		this.feature = feature;
	}

	public boolean isInConfiguration(final Solvis solvis, boolean init) {
		if (solvis.isLearning()) {
			if (this.admin.isAdmin(false)) {
				solvis.addFeatureDependency(Features.getAdminKey());
			}
			if (this.feature != null) {
				solvis.addFeatureDependency(this.feature.getId());
			}

		}
		if (!solvis.isAdmin() && this.admin.isAdmin(init)) {
			return false;
		}
		if (this.feature != null && !solvis.isFeature(this.feature)) {
			return false;
		}

		if (this.masks.isEmpty()) {
			return true;
		}
		for (ConfigurationMask mask : this.masks) {
			if (mask.isInConfiguration(solvis.getConfigurationMask())) {
				return true;
			}
		}
		return false;
	}

	public void setFeatureDependencies(final Solvis solvis) {
	}

	public boolean isVerified(final Configuration masks) {
		for (ConfigurationMask maskO : this.masks) {
			for (ConfigurationMask maskI : masks.masks) {
				if (!maskO.isVerified(maskI)) {
					return false;
				}
			}
		}
		return true;
	}

	public static class Creator extends CreatorByXML<Configuration> {

		private Admin admin = Admin.NONE;
		private final Collection<ConfigurationMask> masks = new ArrayList<>();
		private Feature feature;

		public Creator(final String id, final BaseCreator<?> creator) {
			super(id, creator);
		}

		@Override
		public void setAttribute(final QName name, final String value) {
			switch (name.getLocalPart()) {
				case "admin":
					this.admin = Admin.valueOf(value);
					break;
			}
		}

		@Override
		public Configuration create() throws XmlException, IOException {
			return new Configuration(this.admin, this.masks, this.feature);
		}

		@Override
		public CreatorByXML<?> getCreator(final QName name) {
			String id = name.getLocalPart();
			switch (id) {
				case XML_CONFIGURATION_MASK:
					return new ConfigurationMask.Creator(id, this.getBaseCreator());
				case XML_FEATURE:
					return new Feature.Creator(id, this.getBaseCreator());
			}
			return null;
		}

		@Override
		public void created(final CreatorByXML<?> creator, final Object created) {
			switch (creator.getId()) {
				case XML_CONFIGURATION_MASK:
					this.masks.add((ConfigurationMask) created);
					break;
				case XML_FEATURE:
					this.feature = (Feature) created;
					break;
			}
		}
	}

	private static class ConfigurationMask {
		private final long andMask;
		private final long cmpMask;

		private ConfigurationMask(final long andMask, final long cmpMask) {
			this.andMask = andMask;
			this.cmpMask = cmpMask;
		}

		private boolean isInConfiguration(final long configurationMask) {

			return this.cmpMask == (configurationMask & this.andMask);
		}

		private boolean isVerified(final ConfigurationMask mask) {
			long andMask = this.andMask & mask.andMask;
			return 0 != ((this.cmpMask ^ mask.cmpMask) & andMask);
		}

		@Override
		public String toString() {
			return "And-Mask: " + Long.toString(this.andMask, 16) + ", Cmp-Mask: " + Long.toString(this.cmpMask, 16);
		}

		private static class Creator extends CreatorByXML<ConfigurationMask> {

			private long andMask;
			private long cmpMask;

			private Creator(final String id, final BaseCreator<?> creator) {
				super(id, creator);
			}

			@Override
			public void setAttribute(final QName name, final String value) {
				switch (name.getLocalPart()) {
					case "andMask":
						this.andMask = Long.decode(value);
						break;
					case "compareMask":
						this.cmpMask = Long.decode(value);
						break;
				}

			}

			@Override
			public ConfigurationMask create() throws XmlException, IOException {
				return new ConfigurationMask(this.andMask, this.cmpMask);
			}

			@Override
			public CreatorByXML<?> getCreator(final QName name) {
				return null;
			}

			@Override
			public void created(final CreatorByXML<?> creator, final Object created) {

			}
		}
	}

	@Override
	public String toString() {
		return this.masks.toString();
	}
}
