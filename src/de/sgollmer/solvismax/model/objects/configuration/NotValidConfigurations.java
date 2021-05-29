package de.sgollmer.solvismax.model.objects.configuration;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;

import javax.xml.namespace.QName;

import de.sgollmer.solvismax.model.objects.SolvisDescription;
import de.sgollmer.solvismax.model.objects.unit.Configuration;
import de.sgollmer.xmllibrary.BaseCreator;
import de.sgollmer.xmllibrary.CreatorByXML;
import de.sgollmer.xmllibrary.XmlException;

public class NotValidConfigurations {

	private static final String XML_CONFIGURATION = "Configuration";

	private final Collection<Configuration> configurations;
	private Collection<Long> notValidMasks = null;

	private NotValidConfigurations(final Collection<Configuration> configurations) {
		this.configurations = configurations;
	}

	public static class Creator extends CreatorByXML<NotValidConfigurations> {

		public Creator(final String id, final BaseCreator<?> creator) {
			super(id, creator);
		}

		private final Collection<Configuration> configurations = new ArrayList<>();

		@Override
		public void setAttribute(final QName name, final String value) throws XmlException {

		}

		@Override
		public NotValidConfigurations create() throws XmlException, IOException {
			return new NotValidConfigurations(this.configurations);
		}

		@Override
		public CreatorByXML<?> getCreator(final QName name) {
			String id = name.getLocalPart();
			switch (id) {
				case XML_CONFIGURATION:
					return new Configuration.Creator(id, this.getBaseCreator());
			}
			return null;
		}

		@Override
		public void created(final CreatorByXML<?> creator, final Object created) throws XmlException {
			switch (creator.getId()) {
				case XML_CONFIGURATION:
					this.configurations.add((Configuration) created);
					break;
			}

		}

	}

	public Collection<Long> getNotValidMasks(final SolvisDescription description) {
		Collection<Long> result = new ArrayList<>();
		for (Configuration configuration : this.configurations) {
			result.add(configuration.getConfigurationMask(description));
		}
		return result;
	}

	public boolean isValid(final long mask, final SolvisDescription description) {
		if (this.notValidMasks == null) {
			this.notValidMasks = new ArrayList<>();
			for (Configuration configuration : this.configurations) {
				this.notValidMasks.add(configuration.getConfigurationMask(description));
			}
		}
		for (Long invalidMask : this.notValidMasks) {
			if (invalidMask == (mask & invalidMask)) {
				return false;
			}
		}
		return true;
	}

}
