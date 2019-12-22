package de.sgollmer.solvismax.helper;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.PrintWriter;

import de.sgollmer.solvismax.Constants;
import de.sgollmer.solvismax.Main;

public class FileHelper {
	
	

	public static void copyFromResource(String resourcePath, File destination)
			throws IOException {
		FileHelper.copyFromResource(resourcePath, destination, null, null);
	}

	public static void copyFromResource(String resourcePath, File destination, String target, String replacement)
			throws IOException {
		


		InputStream inputStream = Main.class.getResourceAsStream(resourcePath) ;


		PrintWriter printWriter = null;

		try {
			BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(inputStream));
			printWriter = new PrintWriter(new FileWriter(destination));

			String line;
			while ((line = bufferedReader.readLine()) != null) {
				if ( target != null && line.contains(target)) {
					line = line.replace(target, replacement) ;
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

		FileHelper.copyFromResource(Constants.RESOURCE_PATH + File.separator + "graficData.xsd", file);
	}
}
