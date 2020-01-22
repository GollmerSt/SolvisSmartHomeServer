/************************************************************************
 * 
 * $Id$
 *
 * 
 ************************************************************************/

package de.sgollmer.solvismax.model.objects;

import java.io.IOException;
import java.util.Collection;

import javax.xml.namespace.QName;

import de.sgollmer.solvismax.error.ErrorPowerOn;
import de.sgollmer.solvismax.error.TerminationException;
import de.sgollmer.solvismax.error.XmlError;
import de.sgollmer.solvismax.model.Solvis;
import de.sgollmer.solvismax.model.objects.calculation.Calculation;
import de.sgollmer.solvismax.model.objects.configuration.ConfigurationMasks;
import de.sgollmer.solvismax.model.objects.control.Control;
import de.sgollmer.solvismax.model.objects.data.ModeI;
import de.sgollmer.solvismax.model.objects.data.SolvisData;
import de.sgollmer.solvismax.model.objects.measure.Measurement;
import de.sgollmer.solvismax.model.objects.screen.Screen;
import de.sgollmer.solvismax.xml.CreatorByXML;
import de.sgollmer.solvismax.xml.BaseCreator;

public class ChannelDescription implements ChannelSourceI, Assigner, OfConfigs.Element<ChannelDescription> {

	private static final String XML_CONFIGURATION_MASKS = "ConfigurationMasks";
	private static final String XML_CONTROL = "Control";
	private static final String XML_MEASUREMENT = "Measurement";
	private static final String XML_CALCULATION = "Calculation";

	private final String id;
	private final boolean buffered;
	private final ConfigurationMasks configurationMasks;
	private final ChannelSource channelSource;

	public ChannelDescription(String id, boolean buffered, ConfigurationMasks configurationMasks,
			ChannelSource channelSource) {
		this.id = id;
		this.buffered = buffered;
		this.configurationMasks = configurationMasks;
		this.channelSource = channelSource;
	}

	@Override
	public String getId() {
		return this.id;
	}

	public boolean getValue(Solvis solvis, int timeAfterLastSwitchingOn) throws IOException, ErrorPowerOn, TerminationException {
		SolvisData data = solvis.getAllSolvisData().get(this);
		return this.getValue(data, solvis, timeAfterLastSwitchingOn);
	}

	@Override
	public boolean getValue(SolvisData dest, Solvis solvis, int timeAfterLastSwitchingOn)
			throws IOException, ErrorPowerOn, TerminationException {
		return this.channelSource.getValue(dest, solvis, timeAfterLastSwitchingOn);
	}

	@Override
	public boolean setValue(Solvis solvis, SolvisData value) throws IOException, TerminationException {
		return this.channelSource.setValue(solvis, value);
	}

	@Override
	public boolean isWriteable() {
		return this.channelSource.isWriteable();
	}

	@Override
	public boolean isAverage() {
		return this.channelSource.isAverage();
	}

	@Override
	public Integer getDivisor() {
		return this.channelSource.getDivisor();
	}

	@Override
	public String getUnit() {
		return this.channelSource.getUnit();
	}

	@Override
	public Float getAccuracy() {
		return this.channelSource.getAccuracy();
	}

	@Override
	public boolean isBoolean() {
		return this.channelSource.isBoolean();
	}

	@Override
	public void assign(SolvisDescription description) {
		this.channelSource.assign(description);

	}

	@Override
	public void instantiate(Solvis solvis) {
		this.channelSource.instantiate(solvis);

	}

	public static class Creator extends CreatorByXML<ChannelDescription> {

		private String id;
		private boolean buffered;
		private ConfigurationMasks configurationMasks;
		private ChannelSource channelSource;

		public Creator(String id, BaseCreator<?> creator) {
			super(id, creator);
		}

		@Override
		public void setAttribute(QName name, String value) {
			switch (name.getLocalPart()) {
				case "id":
					this.id = value;
					break;
				case "buffered":
					this.buffered = Boolean.parseBoolean(value);
					break;
			}

		}

		@Override
		public ChannelDescription create() throws XmlError {
			ChannelDescription description = new ChannelDescription(id, buffered, configurationMasks, channelSource);
			channelSource.setDescription(description);
			return description;
		}

		@Override
		public CreatorByXML<?> getCreator(QName name) {
			String source = name.getLocalPart();
			switch (source) {
				case XML_CONFIGURATION_MASKS:
					return new ConfigurationMasks.Creator(source, this.getBaseCreator());
				case XML_CONTROL:
					return new Control.Creator(source, this.getBaseCreator());
				case XML_MEASUREMENT:
					return new Measurement.Creator(source, this.getBaseCreator());
				case XML_CALCULATION:
					return new Calculation.Creator(source, this.getBaseCreator());
			}
			return null;
		}

		@Override
		public void created(CreatorByXML<?> creator, Object created) {
			switch (creator.getId()) {
				case XML_CONFIGURATION_MASKS:
					this.configurationMasks = (ConfigurationMasks) created;
					break;
				case XML_CONTROL:
				case XML_MEASUREMENT:
				case XML_CALCULATION:
					this.channelSource = (ChannelSource) created;
					break;
			}

		}

	}

	@Override
	public void learn(Solvis solvis) throws IOException {
		this.channelSource.learn(solvis);

	}

	@Override
	public Type getType() {
		return this.channelSource.getType();
	}

	@Override
	public Screen getScreen(int configurationMask) {
		return this.channelSource.getScreen(configurationMask);
	}

	@Override
	public Collection<? extends ModeI> getModes() {
		return this.channelSource.getModes();
	}

	@Override
	public UpperLowerStep getUpperLowerStep() {
		return this.channelSource.getUpperLowerStep();
	}

	@Override
	public boolean isInConfiguration(int configurationMask) {
		if (this.configurationMasks == null) {
			return true;
		} else {
			return this.configurationMasks.isInConfiguration(configurationMask);
		}
	}

	@Override
	public boolean isConfigurationVerified(ChannelDescription e) {
		if ((this.configurationMasks == null) || (e.configurationMasks == null)) {
			return false;
		} else {
			return this.configurationMasks.isVerified(e.configurationMasks);
		}
	}

	public boolean isBuffered() {
		return buffered;
	}
}
