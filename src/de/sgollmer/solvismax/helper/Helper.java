package de.sgollmer.solvismax.helper;

import java.util.ArrayList;
import java.util.Collection;

public class Helper {

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

	}


}
