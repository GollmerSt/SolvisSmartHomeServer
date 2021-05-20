package de.sgollmer.solvismax.model.objects.unit;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;

import javax.xml.namespace.QName;

import de.sgollmer.solvismax.model.objects.SolvisDescription;
import de.sgollmer.solvismax.model.objects.configuration.Configurations;
import de.sgollmer.xmllibrary.BaseCreator;
import de.sgollmer.xmllibrary.CreatorByXML;
import de.sgollmer.xmllibrary.XmlException;

public class Configuration {

	private static final String XML_EXTENSIONS = "Extensions";
	private static final String XML_EXTENSION = "Extension";

	private final String solvisType;
	private final String mainHeating;
	private final String solarType;
	private final Integer heaterCircuits;
	private final Collection<String> extensions;

	private Configuration(final String solvisType, final String mainHeating, final Integer heaterCircuits,
			final String solarType, final Collection<String> extensions) {
		this.solvisType = solvisType;
		this.mainHeating = mainHeating;
		this.heaterCircuits = heaterCircuits;
		this.solarType = solarType;
		this.extensions = extensions;
	}

	public long getConfigurationMask(SolvisDescription description) {
		long mask = 0L;
		Configurations configurations = description.getConfigurations();
		if (this.solvisType != null) {
			mask |= configurations.getSolvisTypes().getConfiguration(this.solvisType);
		}
		if (this.mainHeating != null) {
			mask |= configurations.getMainHeatings().getConfiguration(this.mainHeating);
		}
		if (this.heaterCircuits != null) {
			mask |= configurations.getHeaterCircuits().getConfiguration(Integer.toString(this.heaterCircuits));
		}
		if (this.solarType != null) {
			mask |= configurations.getSolarTypes().getConfiguration(this.solarType);
		}
		if (this.extensions != null) {
			for (String extension : this.extensions) {
				mask |= configurations.getExtensions().getConfiguration(extension);
			}
		}
		return mask;
	}

	public static class Creator extends CreatorByXML<Configuration> {

		private String solvisType = null;
		private String mainHeating = null;
		private Integer heaterCircuits = null;
		private String solarType = null;
		private Collection<String> extensions;

		public Creator(String id, BaseCreator<?> creator) {
			super(id, creator);
		}

		@Override
		public void setAttribute(QName name, String value) throws XmlException {
			switch (name.getLocalPart()) {
				case "type":
					this.solvisType = value;
					break;
				case "mainHeating":
					this.mainHeating = value;
					break;
				case "heatingCircuits":
					this.heaterCircuits = Integer.parseInt(value);
					break;
				case "solarType":
					this.solarType = value;
					break;
			}

		}

		@Override
		public Configuration create() throws XmlException, IOException {
			return new Configuration(this.solvisType, this.mainHeating, this.heaterCircuits, this.solarType,
					this.extensions);
		}

		@Override
		public CreatorByXML<?> getCreator(QName name) {
			String id = name.getLocalPart();
			switch (id) {
				case XML_EXTENSIONS:
					return new ExtensionsCreator(id, this.getBaseCreator());
			}
			return null;
		}

		@Override
		public void created(CreatorByXML<?> creator, Object created) throws XmlException {
			switch (creator.getId()) {
				case XML_EXTENSIONS:
					Extensions extensions = (Extensions) created;
					this.extensions = extensions.extensions;
					break;
			}

		}

		private static class Extensions {
			private final Collection<String> extensions;

			private Extensions(final Collection<String> extensions) {
				this.extensions = extensions;
			}
		}

		private static class ExtensionsCreator extends CreatorByXML<Extensions> {

			private final Collection<String> extensions = new ArrayList<>();

			public ExtensionsCreator(String id, BaseCreator<?> creator) {
				super(id, creator);
			}

			@Override
			public void setAttribute(QName name, String value) throws XmlException {
			}

			@Override
			public Extensions create() throws XmlException, IOException {
				return new Extensions(this.extensions);
			}

			@Override
			public CreatorByXML<?> getCreator(QName name) {
				String id = name.getLocalPart();
				switch (id) {
					case XML_EXTENSION:
						return new ExtensionCreator(id, this.getBaseCreator());
				}
				return null;
			}

			@Override
			public void created(CreatorByXML<?> creator, Object created) throws XmlException {
				switch (creator.getId()) {
					case XML_EXTENSION:
						this.extensions.add(((Extension) created).extension);
						break;
				}

			}

		}

		private static class Extension {
			private final String extension;

			public Extension(final String extension) {
				this.extension = extension;
			}
		}

		private static class ExtensionCreator extends CreatorByXML<Extension> {

			private String extension;

			public ExtensionCreator(String id, BaseCreator<?> creator) {
				super(id, creator);
			}

			@Override
			public void setAttribute(QName name, String value) throws XmlException {
				switch (name.getLocalPart()) {
					case "id":
						this.extension = value;
				}

			}

			@Override
			public Extension create() throws XmlException, IOException {
				return new Extension(this.extension);
			}

			@Override
			public CreatorByXML<?> getCreator(QName name) {
				return null;
			}

			@Override
			public void created(CreatorByXML<?> creator, Object created) throws XmlException {

			}

		}

	}

	public String getComment() {

		StringBuilder builder = new StringBuilder();
		builder.append(this.solvisType);
		builder.append(" - ");
		builder.append(this.mainHeating);
		builder.append(" - ");
		builder.append(Integer.toString(this.heaterCircuits));

		for (String extension : this.extensions) {
			builder.append(" - ");
			builder.append(extension);
		}
		return builder.toString();
	}

}
