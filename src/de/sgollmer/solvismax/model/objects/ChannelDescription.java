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

	private ChannelDescription(String id, boolean buffered, String unit, Configuration configurationMasks,
			ChannelSource channelSource, int glitchInhibitScanIntervals) throws XmlException {
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

	public boolean getValue(Solvis solvis)
			throws NumberFormatException, IOException, PowerOnException, TerminationException {
		return this.getValue(solvis, -1L);
	}

	public boolean getValue(Solvis solvis, long executionStartTime)
			throws IOException, PowerOnException, TerminationException, NumberFormatException {
		SolvisData data = solvis.getAllSolvisData().get(this);
		return this.getValue(data, solvis, executionStartTime);
	}

	@Override
	public boolean getValue(SolvisData dest, Solvis solvis, long executionStartTime)
			throws IOException, PowerOnException, TerminationException, NumberFormatException {
		return this.channelSource.getValue(dest, solvis, executionStartTime);
	}

	@Override
	public SetResult setValue(Solvis solvis, SolvisData value) throws IOException, TerminationException {
		return this.channelSource.setValue(solvis, value);
	}

	@Override
	public SetResult setValueFast(Solvis solvis, SolvisData value) throws IOException, TerminationException {
		return this.channelSource.setValueFast(solvis, value);
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
	public ChannelInstance instantiate(Solvis solvis) throws AssignmentException, AliasException {
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
		public CreatorByXML<?> getCreator(QName name) {
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
		public void created(CreatorByXML<?> creator, Object created) {
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
	public void learn(Solvis solvis) throws IOException, LearningException, TerminationException {
		this.channelSource.learn(solvis);

	}

	@Override
	public Type getType() {
		return this.channelSource.getType();
	}

	@Override
	public AbstractScreen getScreen(Solvis solvis) {
		return this.channelSource.getScreen(solvis);
	}

	@Override
	public Collection<? extends IMode<?>> getModes() {
		return this.channelSource.getModes();
	}

	@Override
	public UpperLowerStep getUpperLowerStep() {
		return this.channelSource.getUpperLowerStep();
	}

	@Override
	public boolean isInConfiguration(Solvis solvis) {
		return this.configuration == null || this.configuration.isInConfiguration(solvis);
	}

	@Override
	public boolean isConfigurationVerified(ChannelDescription e) {
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
	public SingleData<?> interpretSetData(SingleData<?> singleData) throws TypeException {
		return this.channelSource.interpretSetData(singleData);
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
	public ChannelDescription getRestoreChannel(Solvis solvis) {
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

	public SingleData<?> toInternal(SingleData<?> data) throws TypeException {
		data = this.interpretSetData(data);

		Integer divisor = this.getDivisor();

		if (divisor == null) {
			return data;
		}

		return new IntegerValue((int) Math.round(data.getDouble() * divisor), data.getTimeStamp());
	}

	public SingleData<?> normalize(SingleData<?> data) {
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
	public boolean isDelayed(Solvis solvis) {
		return this.channelSource.isDelayed(solvis);
	}

	@Override
	public boolean isFast() {
		return this.channelSource.isFast();
	}

	@Override
	public Integer getScanInterval_ms(Solvis solvis) {
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
	public String getCsvMeta(String column, boolean semicolon) {
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


}
