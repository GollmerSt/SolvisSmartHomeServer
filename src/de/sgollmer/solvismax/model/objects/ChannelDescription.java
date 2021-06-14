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

import de.sgollmer.solvismax.Constants.Csv;
import de.sgollmer.solvismax.error.AliasException;
import de.sgollmer.solvismax.error.AssignmentException;
import de.sgollmer.solvismax.error.LearningException;
import de.sgollmer.solvismax.error.PowerOnException;
import de.sgollmer.solvismax.error.ReferenceException;
import de.sgollmer.solvismax.error.TerminationException;
import de.sgollmer.solvismax.error.TypeException;
import de.sgollmer.solvismax.model.Solvis;
import de.sgollmer.solvismax.model.objects.calculation.Calculation;
import de.sgollmer.solvismax.model.objects.configuration.Configuration;
import de.sgollmer.solvismax.model.objects.configuration.OfConfigs;
import de.sgollmer.solvismax.model.objects.control.Control;
import de.sgollmer.solvismax.model.objects.control.DependencyGroup;
import de.sgollmer.solvismax.model.objects.data.DoubleValue;
import de.sgollmer.solvismax.model.objects.data.IMode;
import de.sgollmer.solvismax.model.objects.data.IntegerValue;
import de.sgollmer.solvismax.model.objects.data.SingleData;
import de.sgollmer.solvismax.model.objects.data.SolvisData;
import de.sgollmer.solvismax.model.objects.measure.Measurement;
import de.sgollmer.solvismax.model.objects.screen.AbstractScreen;
import de.sgollmer.xmllibrary.BaseCreator;
import de.sgollmer.xmllibrary.CreatorByXML;
import de.sgollmer.xmllibrary.XmlException;

public class ChannelDescription implements IChannelSource, IAssigner, OfConfigs.IElement<ChannelDescription> {

	private static final String XML_CONFIGURATION = "Configuration";
	private static final String XML_CONTROL = "Control";
	private static final String XML_MEASUREMENT = "Measurement";
	private static final String XML_CALCULATION = "Calculation";

	private final String id;
	private final boolean buffered;
	private final String unit;
	private final Configuration configuration;
	private final ChannelSource channelSource;
	private final int glitchInhibitScanIntervals;

	private ChannelDescription(final String id, final boolean buffered, String unit,
			final Configuration configurationMasks, final ChannelSource channelSource,
			final int glitchInhibitScanIntervals) throws XmlException {
		this.id = id;
		this.buffered = buffered;
		this.unit = unit;
		this.configuration = configurationMasks;
		this.channelSource = channelSource;
		this.glitchInhibitScanIntervals = glitchInhibitScanIntervals;
		if (!this.channelSource.isGlitchDetectionAllowed() && this.glitchInhibitScanIntervals > 0) {
			throw new XmlException("Error in description of channel <" + this.id
					+ ">. Definition of Glitch inhibit time not allowed.");
		}
	}

	public String getId() {
		return this.id;
	}

	@Override
	public String getName() {
		return this.getId();
	}

	public boolean getValue(final Solvis solvis)
			throws NumberFormatException, IOException, PowerOnException, TerminationException {
		return this.getValue(solvis, -1L);
	}

	public boolean getValue(final Solvis solvis, final long executionStartTime)
			throws IOException, PowerOnException, TerminationException, NumberFormatException {
		SolvisData data = solvis.getAllSolvisData().get(this);
		return this.getValue(data, solvis, executionStartTime);
	}

	@Override
	public boolean getValue(final SolvisData dest, final Solvis solvis, final long executionStartTime)
			throws IOException, PowerOnException, TerminationException, NumberFormatException {
		return this.channelSource.getValue(dest, solvis, executionStartTime);
	}

	@Override
	public SetResult setValue(final Solvis solvis, final SolvisData value) throws IOException, TerminationException {
		return this.channelSource.setValue(solvis, value);
	}

	@Override
	public SetResult setDebugValue(final Solvis solvis, final SingleData<?> value)
			throws IOException, TerminationException, TypeException {
		return this.channelSource.setDebugValue(solvis, value);
	}

	@Override
	public SetResult setValueFast(final Solvis solvis, final SolvisData value)
			throws IOException, TerminationException {
		return this.channelSource.setValueFast(solvis, value);
	}

	@Override
	public boolean isWriteable() {
		return this.channelSource.isWriteable();
	}
	
	@Override
	public boolean mustPolling() {
		return this.channelSource.mustPolling();
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
	public void assign(final SolvisDescription description)
			throws XmlException, AssignmentException, ReferenceException {
		if (this.channelSource != null) {
			this.channelSource.assign(description);
		}

	}

	@Override
	public ChannelInstance instantiate(final Solvis solvis) throws AssignmentException, AliasException {
		this.channelSource.instantiate(solvis);
		return null;

	}

	static class Creator extends CreatorByXML<ChannelDescription> {

		private String id;
		private boolean buffered;
		private String unit = null;
		private Configuration configurationMasks;
		private ChannelSource channelSource;
		private int glitchInhibitScanIntervals;

		Creator(String id, BaseCreator<?> creator) {
			super(id, creator);
		}

		@Override
		public void setAttribute(final QName name, final String value) {
			switch (name.getLocalPart()) {
				case "id":
					this.id = value;
					break;
				case "buffered":
					this.buffered = Boolean.parseBoolean(value);
					break;
				case "unit":
					this.unit = value;
					break;
				case "glitchInhibitScanIntervals":
					this.glitchInhibitScanIntervals = Integer.parseInt(value);
					break;
			}

		}

		@Override
		public ChannelDescription create() throws XmlException {
			ChannelDescription description = new ChannelDescription(this.id, this.buffered, this.unit,
					this.configurationMasks, this.channelSource, this.glitchInhibitScanIntervals);
			this.channelSource.setDescription(description);
			return description;
		}

		@Override
		public CreatorByXML<?> getCreator(final QName name) {
			String source = name.getLocalPart();
			switch (source) {
				case XML_CONFIGURATION:
					return new Configuration.Creator(source, this.getBaseCreator());
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
		public void created(final CreatorByXML<?> creator, final Object created) {
			switch (creator.getId()) {
				case XML_CONFIGURATION:
					this.configurationMasks = (Configuration) created;
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
	public void learn(final Solvis solvis) throws IOException, LearningException, TerminationException {
		this.channelSource.learn(solvis);

	}

	@Override
	public Type getType() {
		return this.channelSource.getType();
	}

	@Override
	public AbstractScreen getScreen(final Solvis solvis) {
		return this.channelSource.getScreen(solvis);
	}

	@Override
	public Collection<? extends IMode<?>> getModes() {
		return this.channelSource.getModes();
	}

	public IMode<?> getMode(String name) {
		if (this.getModes() != null) {
			for (IMode<?> mode : this.getModes()) {
				if (mode.getName().equals(name)) {
					return mode;
				}
			}
		}
		return null;
	}

	@Override
	public UpperLowerStep getUpperLowerStep() {
		return this.channelSource.getUpperLowerStep();
	}

	@Override
	public boolean isInConfiguration(final Solvis solvis, final boolean init) {
		return this.configuration == null || this.configuration.isInConfiguration(solvis, init);
	}

	@Override
	public boolean isConfigurationVerified(final ChannelDescription e) {
		if ((this.configuration == null) || (e.configuration == null)) {
			return false;
		} else {
			return this.configuration.isVerified(e.configuration);
		}
	}

	public boolean isBuffered() {
		return this.buffered;
	}

	@Override
	public SingleData<?> interpretSetData(final SingleData<?> singleData, final boolean debug) throws TypeException {
		return this.channelSource.interpretSetData(singleData, debug);
	}

	@Override
	public boolean isHumanAccessDependend() {
		return this.channelSource.isHumanAccessDependend();
	}

	@Override
	public String toString() {
		return this.getId();
	}

	@Override
	public ChannelDescription getRestoreChannel(final Solvis solvis) {
		return this.channelSource.getRestoreChannel(solvis);
	}

	public int glitchInhibitScanIntervals() {
		return this.glitchInhibitScanIntervals;
	}

	@Override
	public DependencyGroup getDependencyGroup() {
		return this.channelSource.getDependencyGroup();
	}

	@Override
	public boolean mustBackuped() {
		return this.channelSource.mustBackuped();
	}

//	public SingleData<?> toInternal(final SingleData<?> data) throws TypeException {
//
//		SingleData<?> interpretedData = this.interpretSetData(data);
//
//		Integer divisor = this.getDivisor();
//
//		if (divisor == null) {
//			return interpretedData;
//		}
//
//		return new IntegerValue((int) Math.round(interpretedData.getDouble() * divisor),
//				interpretedData.getTimeStamp());
//	}
//
	public SingleData<?> normalize(final SingleData<?> data) {
		if (data.get() == null) {
			return null;
		}
		if (data instanceof IntegerValue && this.getDivisor() != 1) {
			return new DoubleValue((double) data.getInt() / this.getDivisor(), data.getTimeStamp());
		} else {
			return data;
		}
	}

	@Override
	public boolean isDelayed(final Solvis solvis) {
		return this.channelSource.isDelayed(solvis);
	}

	@Override
	public boolean isFast() {
		return this.channelSource.isFast();
	}

	@Override
	public Integer getScanInterval_ms(final Solvis solvis) {
		return this.channelSource.getScanInterval_ms(solvis);
	}

	@Override
	public String getElementType() {
		return this.getClass().getSimpleName();
	}

	@Override
	public Configuration getConfiguration() {
		return this.configuration;
	}

	@Override
	public String getCsvMeta(final String column, final boolean semicolon) {
		switch (column) {
			case Csv.ID:
				return this.id;
			case Csv.CHANNEL_TYPE:
				return this.channelSource.getClass().getSimpleName();
			case Csv.BUFFERED:
				return Boolean.toString(this.buffered);
			case Csv.UNIT:
				return this.unit;
			case Csv.GLITCH_INHIBIT:
				return Integer.toString(this.glitchInhibitScanIntervals);
		}
		return this.channelSource.getCsvMeta(column, semicolon);
	}

	@Override
	public boolean inhibitGuiReadAfterWrite() {
		return this.channelSource.inhibitGuiReadAfterWrite();
	}

}
