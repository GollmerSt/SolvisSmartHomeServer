/************************************************************************
 * 
 * $Id$
 *
 * 
 ************************************************************************/

package de.sgollmer.solvismax.helper;

import java.util.ArrayList;
import java.util.Collection;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class Helper {

	public static class Format {
		private final Pattern pattern;

		public Format(String regEx) {
			this.pattern = Pattern.compile(regEx);
		}

		public String getString(String source) {
			Matcher matcher = this.pattern.matcher(source);
			StringBuilder builder = new StringBuilder();
			if (!matcher.matches()) {
				return null;
			} else {
				for (int i = 1; i <= matcher.groupCount(); ++i) {
					builder.append(matcher.group(i));
				}
				return builder.toString();
			}
		}
	}

	public static class FormatOldX {
		private final Collection<FormatChar> formatChars = new ArrayList<>(5);
		private final int origin;

		public FormatOldX(String format) {
			this.origin = format.length() - 1;
			for (int pos = this.origin; pos >= 0; --pos) {
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

	public static class AverageInt {

		int maxSize;
		int[] lastMeasureValues;
		int lastIdx;
		long sum;
		int size;

		public AverageInt(int maxSize) {
			this.maxSize = maxSize;
			this.lastMeasureValues = new int[this.maxSize];
			this.clear();
		}

		public void put(int value) {
			int next = this.lastIdx + 1;
			if (next >= this.maxSize) {
				next = 0;
			}
			if (this.size == this.maxSize) {
				int first = this.lastMeasureValues[next];
				this.sum -= first;
			} else {
				++this.size;
			}
			this.sum += value;
			this.lastMeasureValues[next] = value;
			this.lastIdx = next;

		}

		public int getLast() {
			return this.lastMeasureValues[this.lastIdx];
		}

		public void clear() {
			this.sum = 0;
			this.size = 0;
			this.lastIdx = -1;
		}

		public int get() {
			return (int) ((2 * this.sum + (this.sum >= 0L ? this.size : -this.size)) / (2 * this.size));
		}

		public int size() {
			return this.size;
		}

		public boolean isFilled() {
			return this.size == this.maxSize;
		}
	}

	public static abstract class Runnable implements java.lang.Runnable {

		private static ThreadPoolExecutor EXECUTOR = (ThreadPoolExecutor) Executors.newCachedThreadPool();
		// private static ThreadPoolExecutor executor = (ThreadPoolExecutor)
		// Executors.newFixedThreadPool(Constants.MAX_CONNECTIONS);

		private final String threadName;

		public Runnable(String threadName) {
			this.threadName = threadName;
		}

		@Override
		public abstract void run();

		public void submit() {
			EXECUTOR.submit(new java.lang.Runnable() {

				@Override
				public void run() {
					setThreadName();
					Runnable.this.run();
					setThreadNameFinished();
				}
			});
		}

		private void setThreadName() {
			Thread thread = Thread.currentThread();
			String threadName = thread.getName();
			int i = threadName.lastIndexOf("-finished");
			if (i == threadName.length() - 9) {
				threadName = threadName.substring(0, i);
			}
			i = threadName.lastIndexOf('-');
			if (i > 0) {
				threadName = this.threadName + threadName.substring(i);
			}
			thread.setName(threadName);
		}

		private void setThreadNameFinished() {
			Thread thread = Thread.currentThread();
			String threadName = thread.getName();
			thread.setName(threadName + "-finished");
		}

	}
}
