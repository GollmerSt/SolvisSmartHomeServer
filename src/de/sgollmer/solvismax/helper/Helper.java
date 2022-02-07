/************************************************************************
 * 
 * $Id$
 *
 * 
 ************************************************************************/

package de.sgollmer.solvismax.helper;

import java.net.Inet4Address;
import java.net.InetAddress;
import java.net.InterfaceAddress;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Collection;
import java.util.Enumeration;
import java.util.Iterator;
import java.util.Locale;
import java.util.TimeZone;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import de.sgollmer.solvismax.log.LogManager;
import de.sgollmer.solvismax.log.LogManager.ILogger;

public class Helper {

	@SuppressWarnings("unused")
	private static final ILogger logger = LogManager.getInstance().getLogger(Helper.class);

	private static Collection<InterfaceAddress> LOCAL_INTERFACE_ADDRESSES = null;
	private static Pattern IP_V4_ADDRESS_PATTERN = Pattern.compile("((\\d+\\.){3}\\d+)(:\\d+)?");

	private static Collection<InterfaceAddress> getLocalInterfaceAddresses() {
		if (LOCAL_INTERFACE_ADDRESSES == null) {
			try {
				LOCAL_INTERFACE_ADDRESSES = new ArrayList<>();
				Enumeration<NetworkInterface> nis = NetworkInterface.getNetworkInterfaces();
				while (nis.hasMoreElements()) {
					NetworkInterface networkInterface = nis.nextElement();
					if (networkInterface.isLoopback()) {
						continue;
					}
					for (InterfaceAddress address : networkInterface.getInterfaceAddresses()) {
						LOCAL_INTERFACE_ADDRESSES.add(address);
					}
				}
			} catch (SocketException e) {
				return null;
			}
		}
		return LOCAL_INTERFACE_ADDRESSES;
	}

	public static class Format {
		private final Pattern pattern;

		public Format(final String regEx) {
			this.pattern = Pattern.compile(regEx);
		}

		public String getString(final String source) {
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

		public AverageInt(final int maxSize) {
			this.maxSize = maxSize;
			this.lastMeasureValues = new int[this.maxSize];
			this.clear();
		}

		public void put(final int value) {
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

		protected Runnable(final String threadName) {
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

		public static void shutdown() {
			EXECUTOR.shutdown();
		}

	}

	public static String replaceEnvironments(final String in) {
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

		public Reference(final R value) {
			this.value = value;
		}

		public R get() {
			return this.value;
		}

		public void set(final R value) {
			this.value = value;
		}

		@SuppressWarnings("unchecked")
		private void add(final int value) {
			if (this.value instanceof Integer) {
				this.value = (R) Integer.valueOf(((Integer) this.value).intValue() + value);
			} else if (this.value instanceof Byte) {
				this.value = (R) Byte.valueOf((byte) (((Byte) this.value).intValue() + value));
			} else if (this.value instanceof Long) {
				this.value = (R) Long.valueOf((long) (((Long) this.value).longValue() + value));
			} else if (this.value instanceof Float) {
				this.value = (R) Float.valueOf((float) (((Float) this.value).floatValue() + value));
			} else if (this.value instanceof Double) {
				this.value = (R) Double.valueOf((double) (((Double) this.value).doubleValue() + value));
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
	public static <T> Collection<T> copy(final Collection<T> source) {
		if (source == null) {
			return new ArrayList<>();
		} else {
			return new ArrayList<>(source);
		}
	}

	public static <T extends Comparable<T>> Integer compareTo(final T o1, final T o2) {
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

	public static <T> Integer compareNull(final T o1, final T o2) {
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

	/**
	 * 
	 * This helper enum avoids following error:
	 * 
	 * A method that returns either Boolean.TRUE, Boolean.FALSE or null is an
	 * accident waiting to happen.This method can be invoked as though it returned a
	 * value of type boolean, and the compiler will insert automatic unboxing of the
	 * Boolean value. If a null value is returned,this will result in a
	 * NullPointerException.
	 * 
	 *
	 */
	public enum Boolean {
		TRUE, FALSE, UNDEFINED;

		public boolean result() {
			return this == TRUE;
		}
	}

	public static <T> Helper.Boolean checkNull(final T o1, final T o2) {
		if (o1 == null & o2 == null) {
			return Helper.Boolean.TRUE;
		}
		if (o2 == null || o1 == null) {
			return Helper.Boolean.FALSE;
		}
		return Helper.Boolean.UNDEFINED;
	}

	public static <T> java.lang.Boolean equals(final T o1, final T o2) {
		if (o1 == null & o2 == null) {
			return true;
		}
		if (o2 == null || o1 == null) {
			return false;
		}
		return o1.equals(o2);
	}

	public static class Times {
		private final long now;
		private final long startOfDay;

		public Times(final long now, final long startOfDay) {
			this.now = now;
			this.startOfDay = startOfDay;
		}

		public long getNow() {
			return this.now;
		}

		public long getStartOfDay() {
			return this.startOfDay;
		}
	}

	public static Times getStartOfDay() {
		return getStartOfDay(null, null);
	}

	/**
	 * Get the beginning of the day and the current time. The current time ist
	 * delivered in case of possible consistency problems.
	 * 
	 * @param zone
	 * @param aLocale
	 * 
	 * @return
	 */

	public static Times getStartOfDay(final TimeZone zone, final Locale aLocale) {

		Calendar calendar;

		if (zone == null && aLocale == null) {
			calendar = Calendar.getInstance();
		} else if (zone != null && aLocale != null) {
			calendar = Calendar.getInstance(zone, aLocale);
		} else if (aLocale == null) {
			calendar = Calendar.getInstance(zone);
		} else {
			calendar = Calendar.getInstance(aLocale);
		}

		long now = calendar.getTimeInMillis();

		calendar.set(Calendar.HOUR_OF_DAY, 0);
		calendar.set(Calendar.MINUTE, 0);
		calendar.set(Calendar.SECOND, 0);
		calendar.set(Calendar.MILLISECOND, 0);

		return new Times(now, calendar.getTimeInMillis());
	}

	public static String escaping(String text) {
		return text.replace("\"", "\\\"");
	}

	public static String cat(String[] array, String separator) {
		StringBuilder builder = new StringBuilder();
		boolean first = true;
		for (String part : array) {
			if (first || separator == null) {
				first = false;
			} else {
				builder.append(separator);
			}
			builder.append(part);
		}
		return builder.toString();
	}

	public static Collection<String> getNetworkUrls(String url) {
		Collection<String> result = new ArrayList<>();
		Matcher matcher = IP_V4_ADDRESS_PATTERN.matcher(url);
		if (!matcher.matches()) {
			result.add(url);
		} else {
			Integer port = null;
			String portString = matcher.group(3);
			if (portString != null) {
				port = Integer.parseInt(portString.substring(1));
			}
			String hexString = matcher.group(1);
			InetAddress[] addresses;
			try {
				addresses = Inet4Address.getAllByName(hexString);
			} catch (UnknownHostException e) {
				logger.error("Unexpected error on interpretation of IP address <" + hexString + ">. ignored!");
				return null;
			}
			if (addresses.length != 1) {
				logger.error("Unexpected error on interpretation of IP address <" + hexString + ">. ignored!");
				return null;
			}
			result.add(url);
			for (InterfaceAddress local : Helper.getLocalInterfaceAddresses()) {
				InetAddress address;
				try {
					address = Inet4Address.getByName(url);
					String netAddress = getNetworkAddress(local, address);
					if (netAddress != null) {
						if (port != null) {
							netAddress += ':' + Integer.toString(port);
						}
						result.add(netAddress);
					}
				} catch (UnknownHostException e) {
				}
			}
		}
		return result;
	}

	private static String getNetworkAddress(final InterfaceAddress local, final InetAddress address) {
		byte[] bIp = address.getAddress();

		byte[] bNIp = local.getAddress().getAddress();

		if (bIp.length != bNIp.length) {
			return null;
		}

		byte[] bSub = new byte[bNIp.length];
		Arrays.fill(bSub, (byte) 0);
		int prefixLength = local.getNetworkPrefixLength();
		int cnt0xff = prefixLength / 8;
		prefixLength %= 8;
		Arrays.fill(bSub, 0, cnt0xff, (byte) 0xff);
		bSub[cnt0xff] = (byte) (0xff << (8 - prefixLength));

		byte[] bRslt = new byte[bNIp.length];

		boolean same = true;

		for (int i = 0; i < bIp.length; ++i) {
			bRslt[i] = (byte) (bIp[i] & ~bSub[i] | (bNIp[i] & bSub[i]));
			same &= bRslt[i] != bIp[i];
		}

		if (same) {
			return null;
		} else {
			try {
				return InetAddress.getByAddress(bRslt).getHostAddress();
			} catch (UnknownHostException e) {
				return null;
			}
		}

	}
}
