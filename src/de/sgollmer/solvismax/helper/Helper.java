/************************************************************************
 * 
 * $Id$
 *
 * 
 ************************************************************************/

package de.sgollmer.solvismax.helper;

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

		protected Runnable(String threadName) {
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

	public static String replaceEnvironments(String in) {
		if (!System.getProperty("os.name").toUpperCase().contains("WINDOW")) {
			return in;
		}
		String work = in;
		StringBuilder builder = new StringBuilder();
		Pattern pattern = Pattern.compile("(.*?)%(.*?)%(.*?)");
		Matcher matcher = pattern.matcher(work);
		while (matcher.matches()) {
			builder.append(matcher.group(1));
			String env = System.getenv(matcher.group(2));
			builder.append(env);
			work = matcher.group(3);
			matcher = pattern.matcher(work);
		}
		builder.append(work);
		return builder.toString();
	}

	public static class Reference<R> {
		private final R reference;

		public Reference(R reference) {
			this.reference = reference;
		}

		public R get() {
			return this.reference;
		}
	}
}
