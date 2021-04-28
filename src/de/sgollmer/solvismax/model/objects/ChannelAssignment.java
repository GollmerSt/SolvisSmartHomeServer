package de.sgollmer.solvismax.model.objects;

import java.io.IOException;

import javax.xml.namespace.QName;

import de.sgollmer.solvismax.error.AssignmentException;
import de.sgollmer.solvismax.error.ReferenceException;
import de.sgollmer.solvismax.model.Solvis;
import de.sgollmer.solvismax.model.objects.configuration.Configuration;
import de.sgollmer.xmllibrary.BaseCreator;
import de.sgollmer.xmllibrary.CreatorByXML;
import de.sgollmer.xmllibrary.XmlException;
import de.sgollmer.solvismax.model.objects.configuration.OfConfigs;

public class ChannelAssignment implements OfConfigs.IElement<ChannelAssignment>{

	private static final String XML_CONFIGURATION = "Configuration";

	private final String id;
	private final String name;
	private final String alias;
	private final String unit;
	private final Integer booleanValue;
	private final Configuration configuration;

	public ChannelAssignment(String id, String name, String alias, String unit, Integer booleanValue,
			Configuration configuration) {
		this.id = id;
		this.name = name;
		this.alias = alias;
		this.unit = unit;
		this.booleanValue = booleanValue;
		this.configuration = configuration;
	}

	public static class Creator extends CreatorByXML<ChannelAssignment> {

		private String id;
		private String name;
		private String alias = null;
		private String unit = null;
		private Integer booleanValue = null;
		private Configuration configuration = null;

		public Creator(String id, BaseCreator<?> creator) {
			super(id, creator);
		}

		@Override
		public void setAttribute(QName name, String value) throws XmlException {
			switch (name.getLocalPart()) {
				case "id":
					this.id = value;
					break;
				case "name":
					this.name = value;
					break;
				case "alias":
					this.alias = value;
					break;
				case "unit":
					this.unit = value;
					break;
				case "booleanValue":
					this.booleanValue = Integer.parseInt(value);
					break;
			}

		}

		@Override
		public ChannelAssignment create() throws XmlException, IOException {
			return new ChannelAssignment(this.id, this.name, this.alias, this.unit, this.booleanValue,
					this.configuration);
		}

		@Override
		public CreatorByXML<?> getCreator(QName name) {
			String id = name.getLocalPart();
			switch (id) {
				case XML_CONFIGURATION:
					return new Configuration.Creator(id, this.getBaseCreator());
			}

			return null;
		}

		@Override
		public void created(CreatorByXML<?> creator, Object created) throws XmlException {
			switch (creator.getId()) {
				case XML_CONFIGURATION:
					this.configuration = (Configuration) created;
			}

		}

	}

	@Override
	public void assign(SolvisDescription description) throws XmlException, AssignmentException, ReferenceException {
		// TODO Auto-generated method stub
		
	}

	@Override
	public boolean isConfigurationVerified(ChannelAssignment e) {
		// TODO Auto-generated method stub
		return this.configuration.isVerified(e.configuration);
	}

	@Override
	public boolean isInConfiguration(Solvis solvis) {
		return this.configuration == null || this.configuration.isInConfiguration(solvis);
	}

	@Override
	public String getName() {
		return this.id;
	}

	public String getChannelName() {
		return this.name;
	}

	@Override
	public String getElementType() {
		return this.getClass().getSimpleName();
	}

	@Override
	public Configuration getConfiguration() {
		return this.configuration;
	}

	public String getUnit() {
		return this.unit;
	}

	public Integer getBooleanValue() {
		return this.booleanValue;
	}

	public String getAlias() {
		return this.alias;
	}
}
