package de.sgollmer.solvismax.model.objects.control;

import java.util.ArrayList;
import java.util.Collection;

import de.sgollmer.solvismax.imagepatternrecognition.image.MyImage;
import de.sgollmer.solvismax.imagepatternrecognition.ocr.OcrRectangle;
import de.sgollmer.solvismax.model.Solvis;
import de.sgollmer.solvismax.model.objects.data.IntegerValue;
import de.sgollmer.solvismax.model.objects.data.SingleData;
import de.sgollmer.solvismax.model.objects.data.SolvisData;
import de.sgollmer.solvismax.objects.Rectangle;

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
	public SingleData getValue(MyImage image, Rectangle rectangle) {
		OcrRectangle ocr = new OcrRectangle(image);
		String s = ocr.getString();
		s = format.getString(s);
		if (s == null) {
			return null;
		}
		Integer i = Integer.getInteger(s);
		return new IntegerValue(i);
	}

	public static class Format {
		private final Collection<FormatChar> formatChars = new ArrayList<>();
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
				if (fc.must && pos < 0) {
					return null;
				}
				char c = source.charAt(pos);
				Boolean check = fc.check(c);
				if (check == null) {
					--delta;
				} else if (!check) {
					return null;
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
	public Boolean setValue(Solvis solvis, Rectangle rectangle, SolvisData value) {
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

}
