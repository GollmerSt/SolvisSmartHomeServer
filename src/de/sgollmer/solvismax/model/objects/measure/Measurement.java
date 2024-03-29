/************************************************************************
 * 
 * $Id$
 *
 * 
 ************************************************************************/

package de.sgollmer.solvismax.model.objects.measure;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;

import javax.xml.namespace.QName;

import de.sgollmer.solvismax.Constants.Csv;
import de.sgollmer.solvismax.connection.SolvisConnection.SolvisMeasurements;
import de.sgollmer.solvismax.error.PowerOnException;
import de.sgollmer.solvismax.error.TerminationException;
import de.sgollmer.solvismax.error.TypeException;
import de.sgollmer.solvismax.model.Solvis;
import de.sgollmer.solvismax.model.objects.ChannelDescription;
import de.sgollmer.solvismax.model.objects.ChannelSource;
import de.sgollmer.solvismax.model.objects.IChannelSource;
import de.sgollmer.solvismax.model.objects.IInstance;
import de.sgollmer.solvismax.model.objects.control.DependencyGroup;
import de.sgollmer.solvismax.model.objects.data.IMode;
import de.sgollmer.solvismax.model.objects.data.SingleData;
import de.sgollmer.solvismax.model.objects.data.SolvisData;
import de.sgollmer.solvismax.model.objects.screen.Screen;
import de.sgollmer.solvismax.objects.Field;
import de.sgollmer.xmllibrary.BaseCreator;
import de.sgollmer.xmllibrary.CreatorByXML;
import de.sgollmer.xmllibrary.XmlException;

public class Measurement extends ChannelSource {

	private static final String XML_FIELD = "Field";

	private final IType type;
	private final int divisor;
	private final boolean average;
	private final boolean fast;
	private final Collection<Field> fields;

	// AA5555AA
	// 056B ????
	// 10131B Time 16:19:27
	// 0600 Anlagentyp 6
	// 1100 Systemnummer 17
	// 9C01 S1v /10
	// 9E00 S2v /10
	// A900 S3v /10
	// 0901 S4v /10
	// C409 S5v /10
	// C409 S6v /10
	// C409 S7v /10
	// C409 S8v /10
	// B800 S9v /10
	// 4400 S10v /10
	// C409 S11v /10
	// BC00 S12v /10
	// C409 S13v /10
	// C409 S14v /10
	// C409 S15v /10
	// C409 S16v /10
	// 0000 S18v /10
	// 0000 S17v
	// 0000 AI1 /10
	// 0000 AI2 /10
	// 0000 AI3 /10
	// 00 P1 ?Analog out?
	// 4C P2
	// 4C P3
	// 0D P4
	// 8100 RF1 8,1�C
	// 0000 RF2
	// 0000 RF3
	// 00 A1
	// 00 A2
	// 00 A3
	// 00 A4
	// 00 A5
	// 00 A6
	// 00 A7
	// 00 A8
	// 00 A9
	// 00 A10
	// 00 A11
	// 00 A12
	// 00 A13
	// 00 A14
	// 3A11E9C38D000000
	// 00 P5
	// 000000010101000000
	// 0501 SL
	// 01010096CD000000
	// 130B0F Datum 15.11.2019
	// 49AB00000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000

	private Measurement(final String channelId, final Strategy type, final int divisor, final boolean average,
			final boolean fast, final Collection<Field> fields) throws XmlException {
		this.type = type;
		this.divisor = divisor;
		this.average = average;
		this.fast = fast;
		this.fields = fields;
		if (!this.type.validate(fields)) {
			throw new XmlException("XML-Error: File is not valid of measurement <" + channelId + ">.");
		}
	}

	@Override
	public boolean getValue(final SolvisData dest, final Solvis solvis, final long executionStartTime)
			throws PowerOnException, IOException, TerminationException, NumberFormatException, TypeException {

		if (dest.isDelayed()) {
			dest.setSingleData((SingleData<?>) null);
			return true;
		} else {
			return this.type.get(dest, this.fields, solvis.getMeasureData(), solvis);
		}
	}

	@Override
	public SetResult setValue(final Solvis solvis, final SolvisData value) {
		return null;
	}

	@Override
	public SetResult setValueFast(final Solvis solvis, final SolvisData value) {
		return null;
	}

	@Override
	public boolean isWriteable() {
		return false;
	}

	@Override
	public boolean isAverage() {
		return this.average;
	}

	@Override
	public Integer getDivisor() {
		return this.divisor;
	}

	@Override
	public IInstance instantiate(final Solvis solvis) {
		return null;
	}

	public static class Creator extends CreatorByXML<Measurement> {

		private String channelId;
		private Strategy type;
		private int divisor = 1;
		private boolean average = false;
		private boolean fast = false;
		private final Collection<Field> fields = new ArrayList<>(2);

		public Creator(final String channelId, final String id, final BaseCreator<?> creator) {
			super(id, creator);
			this.channelId = channelId;
		}

		@Override
		public void setAttribute(final QName name, final String value) {
			String id = name.getLocalPart();
			switch (id) {
				case "type":
					this.type = Strategy.valueOf(value.toUpperCase());
					break;
				case "divisor":
					this.divisor = Integer.parseInt(value);
					break;
				case "average":
					this.average = Boolean.parseBoolean(value);
					break;
				case "fast":
					this.fast = Boolean.parseBoolean(value);
					break;
			}

		}

		@Override
		public Measurement create() throws XmlException {
			return new Measurement(this.channelId, this.type, this.divisor, this.average, this.fast, this.fields);
		}

		@Override
		public CreatorByXML<?> getCreator(final QName name) {
			String id = name.getLocalPart();
			switch (id) {
				case XML_FIELD:
					return new Field.Creator(id, this.getBaseCreator());
			}
			return null;
		}

		@Override
		public void created(final CreatorByXML<?> creator, final Object created) {
			switch (creator.getId()) {
				case XML_FIELD:
					this.fields.add((Field) created);
					break;
			}

		}

	}

	@Override
	public void learn(final Solvis solvis) throws IOException {
	}

	@Override
	public Type getType() {
		return IChannelSource.Type.MEASUREMENT;
	}

	@Override
	public Screen getScreen(final Solvis solvis) {
		return null;
	}

	@Override
	public Collection<? extends IMode<?>> getModes() {
		return null;
	}

	@Override
	public Double getAccuracy() {
		if (this.type.isNumeric()) {
			return (double) 1 / (double) this.getDivisor();
		}
		return null;
	}

	@Override
	public UpperLowerStep getUpperLowerStep() {
		return null;
	}

	@Override
	public boolean isBoolean() {
		return this.type.isBoolean();
	}

	@Override
	public SingleData<?> interpretSetData(final SingleData<?> singleData, final boolean internal) throws TypeException {
		if (internal) {
			return this.type.interpretSetData(singleData, this.divisor);
		} else {
			return null;
		}
	}

	@Override
	public ChannelDescription getRestoreChannel(final Solvis solvis) {
		return null;
	}

	interface IType {
		// public boolean get(SolvisData destin, Collection<Field> fields,
		// SolvisMeasurements data)

		public boolean get(final SolvisData dest, final Collection<Field> fields, final SolvisMeasurements measureData,
				final Solvis solvis) throws PowerOnException, IOException, NumberFormatException, TypeException;

		public SingleData<?> interpretSetData(final SingleData<?> singleData, final int divisor) throws TypeException;

		public boolean isNumeric();

		public boolean isBoolean();

		public boolean validate(final Collection<Field> fields);

		public SetResult setDebugValue(final Solvis solvis, final SingleData<?> value);

	}

	@Override
	public DependencyGroup getDependencyGroup() {
		return null;
	}

	@Override
	public boolean mustBackuped() {
		return false;
	}

	@Override
	public boolean isFast() {
		return this.fast;
	}

	@Override
	public Integer getScanInterval_ms(final Solvis solvis) {
		return this.isFast() ? solvis.getUnit().getMeasurementsIntervalFast_ms()
				: solvis.getUnit().getMeasurementsInterval_ms();
	}

	@Override
	public String getCsvMeta(final String column, final boolean semicolon) {
		switch (column) {
			case Csv.DIVISOR:
				return Integer.toString(this.divisor);
			case Csv.AVERAGE:
				return Boolean.toString(this.average);
			case Csv.FAST:
				return Boolean.toString(this.fast);
			case Csv.STRATEGY:
				String s = this.type.toString();
				return s.substring(0, 1).toUpperCase() + s.substring(1).toLowerCase();
		}
		return null;
	}

	@Override
	public SetResult setDebugValue(Solvis solvis, SingleData<?> value) throws IOException, TerminationException {
		return this.type.setDebugValue(solvis, value);
	}

	@Override
	public boolean mustPolling() {
		return false;
	}

	@Override
	public boolean inhibitGuiReadAfterWrite() {
		return false;
	}

}