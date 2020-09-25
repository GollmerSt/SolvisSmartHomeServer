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

import de.sgollmer.solvismax.log.LogManager;
import de.sgollmer.solvismax.log.LogManager.ILogger;

public class Helper {

	@SuppressWarnings("unused")
	private static final ILogger logger = LogManager.getInstance().getLogger(Helper.class);

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
				String integerPlaces = null;
				String decimalPlaces = null;
				String sign = null;
				try {
					integerPlaces = matcher.group("integerPlaces");
					decimalPlaces = matcher.group("decimalPlaces");
					sign = matcher.group("sign");
				} catch (IllegalArgumentException e) {
				}

				if (integerPlaces == null && decimalPlaces == null) {
					for (int i = 1; i <= matcher.groupCount(); ++i) {
						builder.append(matcher.group(i));
					}
				} else {
					if (sign != null && sign.equals("-")) {
						builder.append('-');
					}
					if (integerPlaces != null) {
						builder.append(integerPlaces);
					}
					builder.append('.');
					if (decimalPlaces != null) {
						builder.append(decimalPlaces);
					}
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

		private R value;

		public Reference(R value) {
			this.value = value;
		}

		public R get() {
			return this.value;
		}

		public void set(R value) {
			this.value = value;
		}

		@SuppressWarnings("unchecked")
		private void add(int value) {
			if (this.value instanceof Integer) {
				this.value = (R) new Integer((int) (((Integer) this.value).intValue() + value));
			} else if (this.value instanceof Byte) {
				this.value = (R) new Byte((byte) (((Byte) this.value).intValue() + value));
			} else if (this.value instanceof Long) {
				this.value = (R) new Long((long) (((Long) this.value).longValue() + value));
			} else if (this.value instanceof Float) {
				this.value = (R) new Float((float) (((Float) this.value).floatValue() + value));
			} else if (this.value instanceof Double) {
				this.value = (R) new Double((double) (((Double) this.value).doubleValue() + value));
			} else if (this.value != null) {
				throw new UnsupportedOperationException(
						"add cannot be used fot element of class " + this.value.getClass().getName());
			} else {
				throw new NullPointerException();
			}
		}

		public void increment() {
			this.add(1);
		}

		public void decrement() {
			this.add(-1);
		}
	}

	/**
	 * Copy a collection. If th source collection is null, an empty collection will
	 * be generated
	 * 
	 * @param <T>
	 * @param source source collection, can be null
	 * 
	 * @return copied collection, elements aren't cloned
	 */
	public static <T> Collection<T> copy(Collection<T> source) {
		if (source == null) {
			return new ArrayList<>();
		} else {
			return new ArrayList<>(source);
		}
	}

	public static <T extends Comparable<T>> Integer compareTo(T o1, T o2) {
		if (o1 == null & o2 == null) {
			return 0;
		}
		if (o2 == null) {
			return 1;
		} else if (o1 == null) {
			return -1;
		}
		return o1.compareTo(o2);
	}

	public static <T> Integer compareNull(T o1, T o2) {
		if (o1 == null & o2 == null) {
			return 0;
		}
		if (o2 == null) {
			return 1;
		} else if (o1 == null) {
			return -1;
		}
		return null;
	}

	public static <T> Boolean checkNull(T o1, T o2) {
		if (o1 == null & o2 == null) {
			return true;
		}
		if (o2 == null||o1 == null) {
			return false;
		}
		return null;
	}

	public static <T> Boolean equals(T o1, T o2) {
		if (o1 == null & o2 == null) {
			return true;
		}
		if (o2 == null||o1 == null) {
			return false;
		}
		return o1.equals(o2);
	}

}
