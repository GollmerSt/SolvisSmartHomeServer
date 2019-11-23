package de.sgollmer.solvismax.helper;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import de.sgollmer.solvismax.Main;

public class FileHelper {
	public static void copyFromResource(String resourcePath, File destination) throws IOException {

		InputStream inputStream = Main.class.getResourceAsStream(resourcePath);

		OutputStream outputStream = new FileOutputStream(destination);

		boolean copied = false;

		byte[] buffer = new byte[2048];

		while (!copied) {
			int readCnt = inputStream.read(buffer);
			if (readCnt < 0) {
				copied = true;
			} else {
				outputStream.write(buffer, 0, readCnt);
			}
		}
		outputStream.flush();
		outputStream.close();
		inputStream.close();
	}

	public static void main(String[] args) throws IOException {

		String writeDirectory = System.getProperty("user.home");
		if (System.getProperty("os.name").startsWith("Windows")) {
			writeDirectory = System.getenv("APPDATA");
		}

		writeDirectory += File.separator + "SolvisMaxJava";

		File directory = new File(writeDirectory);

		if (!directory.exists()) {
			boolean success = directory.mkdir();
			if ( !success ) {
				return ;
			}
		}

		File file = new File(directory, "graficData.copied");

		// if ( !file.canWrite() ) {
		// System.out.println("Write not possible") ;
		// }

		FileHelper.copyFromResource("data/graficData.xsd", file);
	}
}
