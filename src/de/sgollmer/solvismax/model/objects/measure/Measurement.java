package de.sgollmer.solvismax.model.objects.measure;

import java.util.ArrayList;
import java.util.Collection;

import javax.xml.namespace.QName;

import de.sgollmer.solvismax.error.XmlError;
import de.sgollmer.solvismax.model.Solvis;
import de.sgollmer.solvismax.model.objects.DataSource;
import de.sgollmer.solvismax.model.objects.SolvisDescription;
import de.sgollmer.solvismax.model.objects.data.SolvisData;
import de.sgollmer.solvismax.objects.Field;
import de.sgollmer.solvismax.xml.BaseCreator;
import de.sgollmer.solvismax.xml.CreatorByXML;

public class Measurement extends DataSource {
	private final Strategy type;
	private final int divisor;
	private final boolean average;
	private final boolean dynamic;
	private final String unit;
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
	// 8100 RF1 8,1°C
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

	public Measurement(Strategy type, int divisor, boolean average, boolean dynamic, String unit, Collection< Field> fields) {
		this.type = type;
		this.divisor = divisor;
		this.average = average;
		this.dynamic = dynamic;
		this.unit = unit;
		this.fields = fields ;
	}

	@Override
	public boolean getValue(SolvisData dest, Solvis solvis) {
		return type.get(dest, this.fields, solvis.getMeasureData());
	}

	@Override
	public boolean setValue(Solvis solvis, SolvisData value) {
		return false;
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
	public String getUnit() {
		return this.unit;
	}

	public boolean isDynamic() {
		return dynamic;
	}

	@Override
	public void instantiate(Solvis solvis) {

	}

	public static class Creator extends CreatorByXML<Measurement> {

		private Strategy type;
		private int divisor = 1;
		private boolean average = false;
		private boolean dynamic = false;
		private String unit = "";
		private final Collection<Field> fields = new ArrayList<>(2);

		public Creator(String id, BaseCreator<?> creator) {
			super(id, creator);
		}

		@Override
		public void setAttribute(QName name, String value) {
			String id = name.getLocalPart();
			switch (id) {
				case "type":
					type = Strategy.valueOf(value.toUpperCase());
					break;
				case "divisor":
					this.divisor = Integer.parseInt(value);
					break;
				case "average":
					this.average = Boolean.parseBoolean(value);
					break;
				case "dynamic":
					this.dynamic = Boolean.parseBoolean(value);
					break;
				case "unit":
					this.unit = value;
			}

		}

		@Override
		public Measurement create() throws XmlError {
			return new Measurement(type, divisor, average, dynamic, unit,fields);
		}

		@Override
		public CreatorByXML<?> getCreator(QName name) {
			String id = name.getLocalPart();
			switch (id) {
				case "Field":
					return new Field.Creator(id, this.getBaseCreator());
			}
			return null;
		}

		@Override
		public void created(CreatorByXML<?> creator, Object created) {
			if ( creator.getId().equals("Field")) {
				this.fields.add((Field) created) ;
			}
			
		}

	}

	@Override
	public void assign(SolvisDescription description) {
	}

}