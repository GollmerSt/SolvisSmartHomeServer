/************************************************************************
 * 
 * $Id$
 *
 * 
 ************************************************************************/

package de.sgollmer.solvismax.helper;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.FilenameFilter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.net.MalformedURLException;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import de.sgollmer.solvismax.Constants;
import de.sgollmer.solvismax.Main;

public class FileHelper {

	public static void copyFromResourceText(String resourcePath, File destination) throws IOException {
		FileHelper.copyFromResource(resourcePath, destination, null, null);
	}

	public static void copyFromResource(String resourcePath, File destination, String target, String replacement)
			throws IOException {

		InputStream inputStream = Main.class.getResourceAsStream(resourcePath);

		PrintWriter printWriter = null;

		try {
			BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(inputStream));
			printWriter = new PrintWriter(new FileWriter(destination));

			String line;
			while ((line = bufferedReader.readLine()) != null) {
				if (target != null && line.contains(target)) {
					line = line.replace(target, replacement);
				}
				printWriter.println(line);
			}

			bufferedReader.close();
			printWriter.flush();
			printWriter.close();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	public static void copyFromResourceBinary(String resourcePath, File destination) throws IOException {

		InputStream inputStream = Main.class.getResourceAsStream(resourcePath);
		OutputStream outputStream = new FileOutputStream(destination);

		boolean finished = false;

		byte[] buffer = new byte[Constants.Files.INPUT_BUFFER_SIZE];

		while (!finished) {
			int cnt = inputStream.read(buffer);
			outputStream.write(buffer, 0, cnt);
			finished = cnt < buffer.length;
		}

		outputStream.flush();
		outputStream.close();
	}

	public static void main(String[] args) throws IOException {

		String writeDirectory = System.getProperty("user.home");
		if (System.getProperty("os.name").startsWith("Windows")) {
			writeDirectory = System.getenv("APPDATA");
		}

		writeDirectory += File.separator + Constants.Files.RESOURCE_DESTINATION;

		File directory = new File(writeDirectory);

		if (!directory.exists()) {
			boolean success = directory.mkdir();
			if (!success) {
				return;
			}
		}

		File file = new File(directory, "graficData.copied");

		// if ( !file.canWrite() ) {
		// System.out.println("Write not possible") ;
		// }

		FileHelper.copyFromResourceText(Constants.Files.RESOURCE + '/' + "graficData.xsd", file);
	}

	/**
	 * Compute the absolute file path to the jar file. The framework is based on
	 * http://stackoverflow.com/a/12733172/1614775 But that gets it right for only
	 * one of the four cases.
	 * 
	 * @param aclass A class residing in the required jar.
	 * 
	 * @return A File object for the directory in which the jar file resides. During
	 *         testing with NetBeans, the result is ./build/classes/, which is the
	 *         directory containing what will be in the jar.
	 */
	public static File getJarDir(Class<?> aclass) {
		URL url;
		String extURL; // url.toExternalForm();

		// get an url
		try {
			url = aclass.getProtectionDomain().getCodeSource().getLocation();
			// url is in one of two forms
			// ./build/classes/ NetBeans test
			// jardir/JarName.jar froma jar
		} catch (SecurityException ex) {
			url = aclass.getResource(aclass.getSimpleName() + ".class");
			// url is in one of two forms, both ending
			// "/com/physpics/tools/ui/PropNode.class"
			// file:/U:/Fred/java/Tools/UI/build/classes
			// jar:file:/U:/Fred/java/Tools/UI/dist/UI.jar!
		}

		// convert to external form
		extURL = url.toExternalForm();

		// prune for various cases
		if (extURL.endsWith(".jar")) // from getCodeSource
			extURL = extURL.substring(0, extURL.lastIndexOf('/'));
		else { // from getResource
			String suffix = '/' + (aclass.getName()).replace('.', '/') + ".class";
			extURL = extURL.replace(suffix, "");
			if (extURL.startsWith("jar:") && extURL.endsWith(".jar!"))
				extURL = extURL.substring(4, extURL.lastIndexOf('/'));
		}

		// convert back to url
		try {
			url = new URL(extURL);
		} catch (MalformedURLException mux) {
			// leave url unchanged; probably does not happen
		}

		// convert url to File
		try {
			return new File(url.toURI());
		} catch (URISyntaxException ex) {
			return new File(url.getPath());
		}
	}

	public static boolean renameDuplicates(File source, int noOfDuplicates) {
		boolean success = false;
		for (int i = noOfDuplicates; i > 0; --i) {
			String sourceName = source.getName();
			String destName = sourceName + '.' + i;
			if (i > 1) {
				sourceName += '.' + Integer.toString(i - 1);
			}
			File s = new File(source.getParent(), sourceName);
			File d = new File(source.getParent(), destName);
			success = s.renameTo(d);
		}
		return success;
	}

	public static boolean mkdir(File directory) {
		if (directory.isDirectory() || directory == null) {
			return true;
		}
		if (!mkdir(directory.getParentFile())) {
			return false;
		}
		return directory.mkdir();
	}

	public static boolean canDelete(File file) {
		if (!file.exists() || file == null) {
			return true;
		}
		if (file.isDirectory()) {
			for (File child : file.listFiles()) {
				if (!canDelete(child)) {
					return false;
				}
			}
		}
		return file.canWrite();

	}

	public static boolean rmDir(File file) {
		if (!canDelete(file)) {
			return false;
		}
		if (!file.exists() || file == null) {
			return true;
		}
		if (file.isDirectory()) {
			for (File child : file.listFiles()) {
				rmDir(child);
			}
		}
		file.delete();
		return true;

	}

	public static String makeOSCompatible(String child) {

		String[] parts = child.split("/|<|>|:|\"|\\\\|\\||\\?|\\*|\\s");
		StringBuilder builder = new StringBuilder();
		for (String part : parts) {
			if (builder.length() != 0) {
				builder.append("-");
			}
			builder.append(part);
		}
		return builder.toString();
	}

	public static Collection<File> getSortedbyDate(File parent, final Pattern regex) {
		List<File> files = new ArrayList<>(Arrays.asList(parent.listFiles(new FilenameFilter() {

			@Override
			public boolean accept(File dir, String name) {
				Matcher matcher = regex.matcher(name);
				return matcher.matches();
			}
		})));
		files.sort(new Comparator<File>() {

			@Override
			public int compare(File o1, File o2) {
				long diff = o1.lastModified() - o2.lastModified();
				return diff > 0 ? 1 : diff == 0 ? 0 : -1;
			}
		});
		return files;
	}

	public static class ChecksumInputStream extends InputStream {

		private final InputStream inputStream;
		private byte[] inputBuffer = null;
		private long hash = 61;
		private long hashSave = 0;

		private void hash(int out) {
			this.hash = 397 * this.hash + 43 * Integer.hashCode(out);
		}

		public ChecksumInputStream(InputStream comp) {
			this.inputStream = comp;
		}

		@Override
		public int available() throws IOException {
			return this.inputStream.available();
		}

		@Override
		public void close() throws IOException {
			try {
				this.skip(null);
			} catch (IOException e) {
			}
			this.inputStream.close();
		}

		@Override
		public void mark(int readlimit) {
			this.hashSave = this.hash;
			this.inputStream.mark(readlimit);
		}

		@Override
		public boolean markSupported() {
			return this.inputStream.markSupported();
		}

		@Override
		public int read() throws IOException {
			int out = this.inputStream.read();
			if (out >= 0) {
				this.hash(out);
			}
			return out;
		}

		@Override
		public int read(byte[] array) throws IOException {
			return this.read(array, 0, array.length);
		}

		@Override
		public int read(byte[] array, int off, int len) throws IOException {
			int cnt = this.inputStream.read(array, off, len);
			for (int i = off; i < cnt + off; ++i) {
				this.hash(array[i]);
			}
			return cnt;
		}

		@Override
		public void reset() throws IOException {
			this.inputStream.reset();
			this.hash = this.hashSave;
		}

		@Override
		public long skip(long n) throws IOException {
			return this.skip(Long.valueOf(n));
		}

		private long skip(Long n) throws IOException {
			if (n != null && n < 0) {
				return 0;
			}
			if (this.inputBuffer == null) {
				this.inputBuffer = new byte[Constants.Files.INPUT_BUFFER_SIZE];
			}
			long current = 0;
			int len;
			if (n == null) {
				len = this.inputBuffer.length;
			} else {
				len = (int) Math.min(n, this.inputBuffer.length);
			}
			int cnt = this.inputBuffer.length;

			while ((n == null || current < n) && cnt >= len) {
				cnt = this.read(this.inputBuffer, 0, len);
				current += cnt;
			}
			return current;
		}

		public long getHash() {
			return this.hash;
		}
	}
}
