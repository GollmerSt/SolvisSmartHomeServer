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

import de.sgollmer.solvismax.connection.mqtt.Mqtt;
import de.sgollmer.solvismax.connection.mqtt.MqttData;
import de.sgollmer.solvismax.error.AssignmentException;
import de.sgollmer.solvismax.error.DependencyException;
import de.sgollmer.solvismax.error.LearningException;
import de.sgollmer.solvismax.error.ModbusException;
import de.sgollmer.solvismax.error.PowerOnException;
import de.sgollmer.solvismax.error.ReferenceException;
import de.sgollmer.solvismax.error.TerminationException;
import de.sgollmer.solvismax.error.TypeException;
import de.sgollmer.solvismax.error.XmlException;
import de.sgollmer.solvismax.model.Solvis;
import de.sgollmer.solvismax.model.objects.calculation.Calculation;
import de.sgollmer.solvismax.model.objects.configuration.ConfigurationMasks;
import de.sgollmer.solvismax.model.objects.configuration.OfConfigs;
import de.sgollmer.solvismax.model.objects.control.Control;
import de.sgollmer.solvismax.model.objects.data.IMode;
import de.sgollmer.solvismax.model.objects.data.SingleData;
import de.sgollmer.solvismax.model.objects.data.SolvisData;
import de.sgollmer.solvismax.model.objects.measure.Measurement;
import de.sgollmer.solvismax.model.objects.screen.AbstractScreen;
import de.sgollmer.solvismax.xml.BaseCreator;
import de.sgollmer.solvismax.xml.CreatorByXML;

public class ChannelDescription implements IChannelSource, IAssigner, OfConfigs.IElement<ChannelDescription> {

	private static final String XML_CONFIGURATION_MASKS = "ConfigurationMasks";
	private static final String XML_CONTROL = "Control";
	private static final String XML_MEASUREMENT = "Measurement";
	private static final String XML_CALCULATION = "Calculation";

	private final String id;
	private final boolean buffered;
	private final String unit;
	private final ConfigurationMasks configurationMasks;
	private final ChannelSource channelSource;

	private ChannelDescription(String id, boolean buffered, String unit, ConfigurationMasks configurationMasks,
			ChannelSource channelSource) {
		this.id = id;
		this.buffered = buffered;
		this.unit = unit;
		this.configurationMasks = configurationMasks;
		this.channelSource = channelSource;
	}

	public String getId() {
		return this.id;
	}

	@Override
	public String getName() {
		return this.getId();
	}

	public boolean getValue(Solvis solvis) throws IOException, PowerOnException, TerminationException, ModbusException {
		SolvisData data = solvis.getAllSolvisData().get(this);
		return this.getValue(data, solvis);
	}

	@Override
	public boolean getValue(SolvisData dest, Solvis solvis)
			throws IOException, PowerOnException, TerminationException, ModbusException {
		return this.channelSource.getValue(dest, solvis);
	}

	@Override
	public SetResult setValue(Solvis solvis, SolvisData value)
			throws IOException, TerminationException, ModbusException {
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

	public String getUnit() {
		return this.unit;
	}

	@Override
	public Double getAccuracy() {
		return this.channelSource.getAccuracy();
	}

	@Override
	public boolean isBoolean() {
		return this.channelSource.isBoolean();
	}

	@Override
	public void assign(SolvisDescription description) throws XmlException, AssignmentException, ReferenceException {
		if (this.channelSource != null) {
			this.channelSource.assign(description);
		}

	}

	@Override
	public void instantiate(Solvis solvis) throws AssignmentException, DependencyException {
		this.channelSource.instantiate(solvis);

	}

	static class Creator extends CreatorByXML<ChannelDescription> {

		private String id;
		private boolean buffered;
		private String unit = null;
		private ConfigurationMasks configurationMasks;
		private ChannelSource channelSource;

		Creator(String id, BaseCreator<?> creator) {
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
				case "unit":
					this.unit = value;
			}

		}

		@Override
		public ChannelDescription create() throws XmlException {
			ChannelDescription description = new ChannelDescription(this.id, this.buffered, this.unit,
					this.configurationMasks, this.channelSource);
			this.channelSource.setDescription(description);
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
					return new Measurement.Creator(this.id, source, this.getBaseCreator());
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
	public void learn(Solvis solvis) throws IOException, LearningException, TerminationException, ModbusException {
		this.channelSource.learn(solvis);

	}

	@Override
	public Type getType() {
		return this.channelSource.getType();
	}

	@Override
	public AbstractScreen getScreen(int configurationMask) {
		return this.channelSource.getScreen(configurationMask);
	}

	@Override
	public Collection<? extends IMode> getModes() {
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
		return this.buffered;
	}

	@Override
	public SingleData<?> interpretSetData(SingleData<?> singleData) throws TypeException {
		return this.channelSource.interpretSetData(singleData);
	}

	@Override
	public boolean isModbus(Solvis solvis) {
		return this.channelSource.isModbus(solvis);
	}

	@Override
	public boolean isScreenChangeDependend() {
		return this.channelSource.isScreenChangeDependend();
	}

	@Override
	public String toString() {
		return this.getId();
	}

	@Override
	public ChannelDescription getRestoreChannel(Solvis solvis) {
		return this.channelSource.getRestoreChannel(solvis);
	}

	MqttData getMqttMeta(Solvis solvis) {
		de.sgollmer.solvismax.connection.transfer.ChannelDescription meta = new de.sgollmer.solvismax.connection.transfer.ChannelDescription(
				this);
		return new MqttData(solvis, Mqtt.formatChannelMetaTopic(this.getId()), meta.getValue().toString(), 0, true);
	}

}
