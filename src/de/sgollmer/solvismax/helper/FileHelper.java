/************************************************************************
 * 
 * $Id$
 *
 * 
 ************************************************************************/

package de.sgollmer.solvismax.helper;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.MalformedURLException;
import java.net.URISyntaxException;
import java.net.URL;

import de.sgollmer.solvismax.Constants;
import de.sgollmer.solvismax.Main;

public class FileHelper {

	public static void copyFromResource(String resourcePath, File destination) throws IOException {
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

	public static void main(String[] args) throws IOException {

		String writeDirectory = System.getProperty("user.home");
		if (System.getProperty("os.name").startsWith("Windows")) {
			writeDirectory = System.getenv("APPDATA");
		}

		writeDirectory += File.separator + Constants.RESOURCE_DESTINATION_PATH;

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

		FileHelper.copyFromResource(Constants.RESOURCE_PATH + '/' + "graficData.xsd", file);
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
}
