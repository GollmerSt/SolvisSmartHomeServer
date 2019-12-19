package de.sgollmer.solvismax.model.objects.control;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;

import javax.xml.namespace.QName;

import de.sgollmer.solvismax.error.XmlError;
import de.sgollmer.solvismax.imagepatternrecognition.image.MyImage;
import de.sgollmer.solvismax.imagepatternrecognition.ocr.OcrRectangle;
import de.sgollmer.solvismax.model.Solvis;
import de.sgollmer.solvismax.model.objects.ChannelSourceI.UpperLowerStep;
import de.sgollmer.solvismax.model.objects.SolvisDescription;
import de.sgollmer.solvismax.model.objects.data.IntegerValue;
import de.sgollmer.solvismax.model.objects.data.ModeI;
import de.sgollmer.solvismax.model.objects.data.SolvisData;
import de.sgollmer.solvismax.objects.Rectangle;
import de.sgollmer.solvismax.xml.BaseCreator;
import de.sgollmer.solvismax.xml.CreatorByXML;

public class StrategyRead implements Strategy {
	private final Format format;
	private final int divisor;
	private final String unit;

	public StrategyRead(String format, int divisor, String unit) {
		this.format = new Format(format);
		this.divisor = divisor;
		this.unit = unit;
	}

	@Override
	public boolean isWriteable() {
		return false;
	}

	@Override
	public IntegerValue getValue(MyImage image, Rectangle rectangle, Solvis solvis) {
		OcrRectangle ocr = new OcrRectangle(image, rectangle);
		String s = ocr.getString();
		s = format.getString(s);
		if (s == null) {
			return null;
		}
		Integer i = Integer.parseInt(s);
		return new IntegerValue(i);
	}

	public static class Format {
		private final Collection<FormatChar> formatChars = new ArrayList<>(5);
		private final int origin;

		public Format(String format) {
			this.origin = format.length() - 1;
			for (int pos = origin; pos >= 0; --pos) {
				FormatChar fc = FormatChar.create(format, pos);
				if (fc != null) {
					this.formatChars.add(FormatChar.create(format, pos));
				}
			}
		}

		public String getString(String source) {
			StringBuilder builder = new StringBuilder();
			int delta = source.length() - 1 - this.origin;
			for (FormatChar fc : this.formatChars) {
				int pos = fc.pos + delta;
				if (pos < 0) {
					if (fc.must) {
						return null;
					} else {
						break;
					}
				}
				if (fc.must && pos < 0) {
					return null;
				}
				char c = source.charAt(pos);
				Boolean check = fc.check(c);
				if (check == null) {
					--delta;
				} else if (!check) {
					return null;
				} else {
					builder.append(c);
				}
			}
			return builder.reverse().toString();
		}

	}

	public static class FormatChar {
		private final int pos;
		private final boolean must;

		public FormatChar(int pos, boolean must) {
			this.pos = pos;
			this.must = must;
		}

		public static FormatChar create(String format, int pos) {
			char f = format.charAt(pos);
			switch (f) {
				case '0':
					return new FormatChar(pos, true);
				case '#':
					return new FormatChar(pos, false);
				default:
					return null;
			}
		}

		public Boolean check(char c) {
			if (Character.isDigit(c) || c == '-' || c == '+') {
				return true;
			} else if (this.must) {
				return false;
			} else {
				return null;
			}
		}
	}

	@Override
	public Boolean setValue(Solvis solvis, Rectangle rectangle, SolvisData value) throws IOException {
		return null;
	}

	@Override
	public Integer getDivisor() {
		return this.divisor;
	}

	@Override
	public String getUnit() {
		return this.unit;
	}

	public static class Creator extends CreatorByXML<StrategyRead> {

		private String format;
		private int divisor = 1;
		private String unit;

		public Creator(String id, BaseCreator<?> creator) {
			super(id, creator);
		}

		@Override
		public void setAttribute(QName name, String value) {
			switch (name.getLocalPart()) {
				case "format":
					this.format = value;
					break;
				case "unit":
					this.unit = value;
					break;
				case "divisor":
					this.divisor = Integer.parseInt(value);
					break;
			}

		}

		@Override
		public StrategyRead create() throws XmlError {
			return new StrategyRead(format, divisor, unit);
		}

		@Override
		public CreatorByXML<?> getCreator(QName name) {
			return null;
		}

		@Override
		public void created(CreatorByXML<?> creator, Object created) {

		}

	}

	@Override
	public void assign(SolvisDescription description) {

	}

	@Override
	public Float getAccuracy() {
		return (float) 1 / (float) this.getDivisor();
	}

	@Override
	public Collection<? extends ModeI> getModes() {
		return null;
	}

	@Override
	public UpperLowerStep getUpperLowerStep() {
		return null;
	}

	@Override
	public void setCurrentRectangle(Rectangle rectangle) {
	}

}
